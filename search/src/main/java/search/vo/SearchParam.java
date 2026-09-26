package search.vo;

import common.exception.ValidationException;
import lombok.Data;
import search.constant.EsConstant;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 前台商品检索的查询条件，由 URL 查询参数绑定。
 *
 * <p>缺省值补齐与合法性校验集中在 {@link #normalizeAndValidate()}，检索前必须先调用它。
 */

@Data
public class SearchParam {

    /**
     * 全文匹配关键字，匹配商品标题
     */
    private String keyword;

    /**
     * 品牌 ID，可多选，多个之间是 OR
     */
    private List<Long> brandId;

    /**
     * 三级分类 ID
     */
    private Long catalog3Id;

    /**
     * 排序条件，格式为 {@code <字段>_<asc|desc>}，字段名必须和 {@code es.SkuEsModel} 里的名字一致，
     * 后端拆开后直接当 ES 的排序字段用。例如：skuPrice_asc、saleCount_desc、hotScore_desc
     */
    private String sort;

    /**
     * 是否有货：0-无货，1-有货；不传表示不按库存过滤
     */
    private Integer hasStock;

    /**
     * 价格区间，格式为 {@code 最低价_最高价}，任一端可省略
     */
    private String skuPrice;

    /**
     * 属性筛选，每项格式为 {@code <属性id>_<属性值>}
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
     * 合法的排序条件：{@code <字段>_<asc|desc>}。
     *
     * <p>字段名必须和 {@code es.SkuEsModel} 里的一致（ES 的字段名区分大小写），因为后端拆开后直接
     * 当 ES 排序字段用；升降序不区分大小写。
     */
    private static final Pattern SORT_PATTERN = Pattern.compile("^(skuPrice|saleCount|hotScore)_(?i:asc|desc)$");

    /**
     * 合法的价格区间：{@code 1000_2000}、{@code _2000}（只要上限）、{@code 1000_}（只要下限）。
     *
     * <p>边界值允许小数，解析时按 {@code Double} 处理。
     */
    private static final Pattern SKU_PRICE_PATTERN = Pattern.compile("^(-?\\d+(\\.\\d+)?)?_(-?\\d+(\\.\\d+)?)?$");

    /**
     * 校验排序条件是否合法。
     *
     * <p>不校验的后果：字段名写错 ES 会直接报错，少写 {@code _asc}/{@code _desc} 会数组越界，两种
     * 返回的都是没有 code/msg 的 500，前端只能提示"请求失败"。
     *
     * @param sort 排序条件，允许为 {@code null} 或空（表示不排序）
     * @return {@code true} 表示合法或未指定排序
     */
    public static boolean isValidSort(String sort) {
        return sort == null || sort.isBlank() || SORT_PATTERN.matcher(sort).matches();
    }

    /**
     * 校验价格区间是否合法。
     *
     * <p>不校验的后果：{@code _2000} 这类只给上限的写法会被拆成两段、去解析空串而抛
     * {@code NumberFormatException}；{@code abc}、{@code 1000__2000} 这类解析不出区间的写法会被
     * 静默忽略，用户以为筛了价格其实没筛。
     *
     * @param skuPrice 价格区间，允许为 {@code null} 或空（表示不按价格过滤）
     * @return {@code true} 表示合法或未指定价格区间
     */
    public static boolean isValidSkuPrice(String skuPrice) {
        if (skuPrice == null || skuPrice.isBlank()) {
            return true;
        }
        // 单独一个下划线两边都空，等于没给条件，直接判为不合法
        return SKU_PRICE_PATTERN.matcher(skuPrice).matches() && !"_".equals(skuPrice);
    }

    /**
     * 解析属性筛选参数里的一项。
     *
     * <p>属性值里允许再出现下划线和冒号（冒号是同一属性多值的分隔符），所以只按第一个下划线切分，
     * 否则 {@code 1_8GB_plus} 这类值会被截断成 {@code 8GB}，静默筛错。
     *
     * @param attr 属性筛选项，格式为 {@code <属性id>_<属性值>}
     * @return 解析结果，格式不合法时返回 {@code null}（由调用方决定是报错还是忽略）
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
     * 属性筛选条件。
     *
     * @param attrId 属性 ID
     * @param value 属性值，可能是 {@code 8GB:12GB} 这种多值形式（冒号分隔，同属性内是 OR）
     */
    public record AttrFilter(long attrId, String value) {
    }

    /**
     * 补齐缺省值并校验查询条件，不合法时抛 {@link ValidationException}，产出与注解校验一致的
     * {@code {code:10001, errors:{字段:消息}}}。
     *
     * <p>手写校验而不用注解：本类走 URL 查询参数绑定，与 {@code @RequestParam} 上的约束、
     * {@code @RequestBody} 上的 {@code @Valid} 不是同一条异常链路；且 from+size 是跨字段规则，
     * sort / skuPrice / attrs 是复合格式规则，注解表达不了。
     *
     * <p>校验失败一律报错、不静默忽略：静默忽略会让前端以为筛选生效了、实际没生效，最难排查。
     *
     * <p>本方法会把补好的缺省值写回字段，之后 service 直接用 {@code getPageNum()}/{@code getPageSize()} 即可。
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
     * <p>字段名必须与调用方的字段名一致（前端表单控件的 prop、查询参数名），否则错误挂不到对应
     * 控件上；整体性规则挂到最相关的字段，不要退回成不带字段的消息，那会让调用方多一种形状要处理。
     *
     * @param field 出错的字段名
     * @param message 错误提示
     * @return 可直接抛出的校验异常
     */
    private static ValidationException invalid(String field, String message) {
        return new ValidationException(field, message);
    }
}
