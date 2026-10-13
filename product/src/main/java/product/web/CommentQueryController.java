package product.web;

import common.query.PageQuery;
import common.utils.R;
import common.vo.PageVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import product.service.SpuCommentService;
import product.vo.CommentSummaryVo;
import product.vo.SpuCommentVo;

/**
 * 前台公开的商品评价接口：某件商品的评价列表与评价统计。
 *
 * <p>路径为 {@code product/front/comment}，**不带 jwt**：网关只对 {@code /api/{模块}/front/jwt/**}
 * 要求会员凭证，这里是匿名可看的 —— 商品页不登录也要能看到评价。挂在 {@code /front} 下还有一层原因：
 * {@code /api/**} 里除前台前缀之外的路径一律按管理端接口要 token，写成 {@code product/comment/**}
 * 会让匿名访客拿到 401。
 *
 * <p>按 SPU 聚合而不是 SKU：一款商品多个规格时，按 SKU 列会让每个规格都显得没有评价，
 * 每行的 {@code spuAttributes} 已经写明买的是哪个规格。
 */
@RestController
@RequestMapping("product/front/comment")
public class CommentQueryController {

    private final SpuCommentService spuCommentService;

    /**
     * 由 Spring 注入评价服务，创建后即可直接调用。
     *
     * @param spuCommentService 商品评价服务
     */
    public CommentQueryController(SpuCommentService spuCommentService) {
        this.spuCommentService = spuCommentService;
    }

    /**
     * 分页查询某件商品的评价。
     *
     * <p>只列可见的直接评价，按评价时间倒序；返回的昵称已打码，不含会员 ID 与 IP。</p>
     *
     * @param spuId 商品 SPU ID
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为评价列表
     */
    @GetMapping("/list")
    public R<PageVO<SpuCommentVo>> list(@RequestParam("spuId") Long spuId, PageQuery query) {
        return R.ok(spuCommentService.querySpuComments(spuId, query));
    }

    /**
     * 查询某件商品的评价统计：总数、平均分与星级分布。
     *
     * <p>与列表同一套过滤口径，所以两者不会出现"总数 10 条、列表只有 6 条"。</p>
     *
     * @param spuId 商品 SPU ID
     * @return 统计结果；没有评价时 {@code total} 为 0、{@code averageStar} 为 0
     */
    @GetMapping("/summary")
    public R<CommentSummaryVo> summary(@RequestParam("spuId") Long spuId) {
        return R.ok(spuCommentService.summaryOfSpu(spuId));
    }

}
