package search.vo;

import common.exception.ValidationException;
import lombok.Data;
import search.constant.EsConstant;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 封装页面所有可能传递过来的查询条件
 */

@Data
public class SearchParam {

    /**
     * 页面传递过来的全文匹配关键字
     */
    private String keyword;

    /**
     * 品牌id,可以多选
     */
    private List<Long> brandId;

    /**
     * 三级分类id
     */
    private Long catalog3Id;

    /**
     * 排序条件，格式为 <字段>_<asc|desc>，字段名必须和 es.SkuEsModel 里的名字一致，
     * 后端是拆开后直接当 ES 的排序字段用的。
     * 例如：skuPrice_asc、saleCount_desc、hotScore_desc
     */
    private String sort;

    /**
     * 是否显示有货
     */
    private Integer hasStock;

    /**
     * 价格区间查询
     */
    private String skuPrice;

    /**
     * 按照属性进行筛选
     */
    private List<String> attrs;

    /**
     * 页码，从 1 开始
     */
    private Integer pageNum = 1;

    /**
     * 每页条数。由前端传，服务端只做范围校验（1 ~ {@link EsConstant#MAX_PAGE_SIZE}），
     * 不传时取 {@link EsConstant#DEFAULT_PAGE_SIZE}。
     */
    private Integer pageSize;

    /**
     * 合法的排序条件：<字段>_<asc|desc>。
     * 字段名必须和 es.SkuEsModel 里的一致（ES 的字段名区分大小写），因为后端拆开后直接当 ES 排序字段用；
     * 升降序不区分大小写，和原来 "asc".equalsIgnoreCase(...) 的行为保持一致，不要无谓收窄可接受的入参。
     */
    private static final Pattern SORT_PATTERN = Pattern.compile("^(skuPrice|saleCount|hotScore)_(?i:asc|desc)$");

    /**
     * 合法的价格区间：{@code 1000_2000}、{@code _2000}（只要上限）、{@code 1000_}（只要下限）。
     * 小数仍然允许（原来是 Double.parseDouble，别无谓收窄）。
     */
    private static final Pattern SKU_PRICE_PATTERN = Pattern.compile("^(-?\\d+(\\.\\d+)?)?_(-?\\d+(\\.\\d+)?)?$");

    /**
     * 校验 sort 是否合法。
     *
     * 不校验的后果：字段名写错（比如 price）ES 会直接报错，少写 _asc/_desc 会 sortFields[1] 数组越界，
     * 两种返回的都是 Spring 默认的 500 error JSON，里面没有 code/msg，前端只能弹"请求失败（HTTP 500）"。
     */
    public static boolean isValidSort(String sort) {
        return sort == null || sort.isBlank() || SORT_PATTERN.matcher(sort).matches();
    }

    /**
     * 校验价格区间是否合法。
     *
     * 不校验的后果有两个：
     * 1、{@code skuPrice=_2000} 原意是"价格小于等于 2000"，但 split("_") 得到的是 ["", "2000"]（两段），
     *    会走区间分支去 parseDouble("")，直接 NumberFormatException；
     * 2、{@code skuPrice=abc}、{@code 1000__2000} 这类两段都不是的，原来的 if/else if 一个分支都进不去，
     *    结果是"静默不加价格条件"，用户以为筛了其实没筛。
     */
    public static boolean isValidSkuPrice(String skuPrice) {
        if (skuPrice == null || skuPrice.isBlank()) {
            return true;
        }
        // 单独一个下划线两边都空，等于没给条件，直接判为不合法
        return SKU_PRICE_PATTERN.matcher(skuPrice).matches() && !"_".equals(skuPrice);
    }

    /**
     * attrs 里的一项：{@code <属性id>_<属性值>}。属性值里允许再出现下划线和冒号
     * （冒号是同一属性多值的分隔符），所以只按<b>第一个</b>下划线切分 ——
     * 原来的 {@code split("_")} 会把 "1_8GB_plus" 这种值截断成 "8GB"，静默筛错。
     *
     * @return 解析结果，格式不合法时返回 null（由调用方决定是报错还是忽略）
     */
    public static AttrFilter parseAttr(String attr) {
        if (attr == null) {
            return null;
        }
        int underscore = attr.indexOf('_');
        // 没有下划线、id 为空（"_8GB"）、值为空（"1_"）都算不合法
        if (underscore <= 0 || underscore == attr.length() - 1) {
            return null;
        }
        String value = attr.substring(underscore + 1);
        // 值全是冒号（":"、": :"）也等于没给值
        if (value.replace(":", "").isBlank()) {
            return null;
        }
        try {
            return new AttrFilter(Long.parseLong(attr.substring(0, underscore)), value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 属性筛选条件。{@code value} 可能是 {@code 8GB:12GB} 这种多值形式（冒号分隔，同属性内是 OR）。
     */
    public record AttrFilter(long attrId, String value) {
    }

    /**
     * 补齐缺省值并校验，不合法直接抛 {@link ValidationException}，产出和注解路径完全一样的
     * {@code {code:10001, errors:{字段:消息}}}。
     *
     * <p>为什么不用 @Valid 注解：本类是从 URL 查询参数绑定的（@ModelAttribute 路径），校验失败时
     * Spring 抛的异常和 @RequestParam 上的约束（ConstraintViolationException）、@RequestBody 上的
     * @Valid（MethodArgumentNotValidException）不是同一条链路。而且下面这几条规则注解也表达不了：
     * from+size 是跨字段的，sort / skuPrice / attrs 是复合格式的。手写校验既能和已有的 isValidSort
     * 保持同一套写法，又能让校验规则和下面的解析逻辑待在一起，不会出现"校验说合法、解析却按
     * 另一种方式理解"的漂移。</p>
     *
     * <p>校验失败一律报错、不静默忽略：静默忽略会让前端以为筛选生效了、实际没生效，最难排查。</p>
     *
     * <p>本方法会把补好的缺省值写回字段，之后 service 直接用 getPageNum()/getPageSize() 即可。</p>
     */
    public void normalizeAndValidate() {
        if (pageNum == null) {
            // pageNum= 这种空串绑定会把字段冲成 null，默认值不能只靠字段初始化
            pageNum = 1;
        }
        if (pageSize == null) {
            pageSize = EsConstant.DEFAULT_PAGE_SIZE;
        }

        if (pageNum < 1) {
            throw invalid("pageNum", "页码必须大于 0：" + pageNum);
        }
        if (pageSize < 1 || pageSize > EsConstant.MAX_PAGE_SIZE) {
            throw invalid("pageSize", "每页条数必须在 1~" + EsConstant.MAX_PAGE_SIZE + " 之间：" + pageSize);
        }
        // 跨字段规则（from + size 超限），没有单一归属字段，挂到 pageNum 上
        if ((long) pageNum * pageSize > EsConstant.MAX_RESULT_WINDOW) {
            throw invalid("pageNum", "翻页过深，最多只能查询前 " + EsConstant.MAX_RESULT_WINDOW + " 条数据");
        }
        if (hasStock != null && hasStock != 0 && hasStock != 1) {
            throw invalid("hasStock", "是否有货只能是 0 或 1：" + hasStock);
        }
        if (!isValidSort(sort)) {
            throw invalid("sort", "排序参数不合法：" + sort
                    + "，格式应为 skuPrice/saleCount/hotScore 加 _asc 或 _desc");
        }
        if (!isValidSkuPrice(skuPrice)) {
            throw invalid("skuPrice", "价格区间不合法（" + skuPrice
                    + "），格式应为 最低价_最高价，或 _最高价 / 最低价_");
        }
        if (attrs != null) {
            for (String attr : attrs) {
                if (parseAttr(attr) == null) {
                    throw invalid("attrs", "属性筛选参数不合法（" + attr + "），格式应为 <属性id>_<属性值>");
                }
            }
        }
    }

    /**
     * 复杂规则手写校验的统一出口。
     *
     * <p>字段名不是随便给的 —— 它要和调用方那边的字段名一致（前端 el-form-item 的 prop、
     * 查询参数名），否则错误挂不到对应控件上。整体性的规则（比如"翻页过深"）挂到最相关的
     * 那个字段，也不要退回成不带字段的 msg：那会让调用方多一种形状要处理。</p>
     */
    private static ValidationException invalid(String field, String message) {
        return new ValidationException(field, message);
    }
}
