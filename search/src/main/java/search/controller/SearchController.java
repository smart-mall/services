package search.controller;

import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import search.service.MallSearchService;
import search.vo.SearchParam;
import search.vo.SearchResult;

/**
 * 前台（商城页面）商品搜索接口，返回 JSON。
 *
 * <p>查询条件由 {@link SearchParam} 从 URL 查询参数绑定（keyword / catalog3Id / brandId / attrs /
 * skuPrice / hasStock / sort / pageNum）；已选筛选条件由 {@link SearchResult.NavVo} 给出
 * removeKey + removeValue，前端自行增删查询条件后重新请求，后端不拼装 URL。
 *
 * <p>路径带 search 前缀，才能被网关的 search-api-route（Path=/api/search/** 且剥掉 /api）命中：
 * GET /api/search/front/list → /search/front/list。
 */
@Slf4j
@RestController
@RequestMapping("search/front")
public class SearchController {

    private final MallSearchService mallSearchService;

    public SearchController(MallSearchService mallSearchService) {
        this.mallSearchService = mallSearchService;
    }

    /**
     * 商品检索：按关键字模糊匹配，并可按三级分类、品牌、属性、价格区间、是否有货过滤，支持排序和分页。
     *
     * <p>查询条件不合法时由服务层抛 {@code ValidationException}，经全局异常处理器转成 code=10001 的响应。
     *
     * @param param 检索条件，由 URL 查询参数绑定，不能为 {@code null}
     * @return 检索结果，含商品列表、分页信息与聚合出的筛选项
     */
    @GetMapping("/list")
    public R<SearchResult> list(SearchParam param) {
        log.debug("商品检索，param={}", param);
        SearchResult result = mallSearchService.search(param);
        return R.ok(result);
    }
}
