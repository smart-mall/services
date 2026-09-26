package search.service;


import search.vo.SearchParam;
import search.vo.SearchResult;

/** 前台商品检索服务。 */
public interface MallSearchService {

    /**
     * 按查询条件检索商品，并聚合出可选的品牌、属性、分类与已选筛选条件。
     *
     * <p>实现方必须保证：先补齐并校验 {@code param} 的缺省值（页码、每页条数、排序、价格区间、
     * 属性），条件不合法时抛 {@code ValidationException}，不能静默忽略。
     *
     * @param param 检索条件，不能为 {@code null}
     * @return 检索结果，含商品列表、分页信息与聚合结果
     */
    SearchResult search(SearchParam param);
}
