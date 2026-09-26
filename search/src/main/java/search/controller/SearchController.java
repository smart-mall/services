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
 * 前台（商城页面）商品搜索接口。
 *
 * 原来是 @Controller + GET /list.html，返回 Thymeleaf 视图 list，靠 Model 把 SearchResult 交给模板渲染；
 * 为了让面包屑能"点 x 删掉这个筛选"，还用 request.getQueryString() 抓原始查询串、在服务端做字符串拼接算出新链接。
 * 前台换成 Vue 之后没有 Thymeleaf 和 Model 了，这里改成纯 JSON 接口：
 * 查询条件仍由 SearchParam 从 URL 查询参数绑定（keyword / catalog3Id / brandId / attrs / skuPrice / hasStock / sort / pageNum），
 * 面包屑改由 SearchResult.NavVo 给出 removeKey + removeValue，由前端自己增删查询条件再重新请求。
 *
 * 路径带 search 前缀，才能被网关的 search-api-route（Path=/api/search/** 且剥掉 /api）命中：
 * GET /api/search/front/list → /search/front/list
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
     * 商品检索：按关键字模糊匹配，并可按三级分类、品牌、属性、价格区间、是否有货过滤，支持排序和分页
     */
    @GetMapping("/list")
    public R<SearchResult> list(SearchParam param) {
        log.debug("商品检索，param={}", param);
        SearchResult result = mallSearchService.search(param);
        return R.ok(result);
    }
}
