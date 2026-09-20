package search.vo;

import lombok.Data;

import java.util.List;
import java.util.regex.Pattern;

/**
 * @Description: 封装页面所有可能传递过来的查询条件
 * @Created: with IntelliJ IDEA.
 * @author: 夏沫止水
 * @createTime: 2020-06-13 14:17
 **/

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
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 合法的排序条件：<字段>_<asc|desc>。
     * 字段名必须和 es.SkuEsModel 里的一致（ES 的字段名区分大小写），因为后端拆开后直接当 ES 排序字段用；
     * 升降序不区分大小写，和原来 "asc".equalsIgnoreCase(...) 的行为保持一致，不要无谓收窄可接受的入参。
     */
    private static final Pattern SORT_PATTERN = Pattern.compile("^(skuPrice|saleCount|hotScore)_(?i:asc|desc)$");

    /**
     * 校验 sort 是否合法。
     *
     * 不校验的后果：字段名写错（比如 price）ES 会直接报错，少写 _asc/_desc 会 sortFields[1] 数组越界，
     * 两种返回的都是 Spring 默认的 500 error JSON，里面没有 code/msg，前端只能弹"请求失败（HTTP 500）"。
     */
    public static boolean isValidSort(String sort) {
        return sort == null || sort.isBlank() || SORT_PATTERN.matcher(sort).matches();
    }
}
