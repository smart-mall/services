package product.web;

import common.query.PageQuery;
import common.utils.LoginUserUtils;
import common.utils.R;
import common.vo.PageVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import product.entity.SpuCommentEntity;
import product.service.SpuCommentService;
import product.vo.CommentSubmitVo;

/**
 * 前台需要登录的商品评价接口：提交、查看我的评价、删除我的一条评价。
 *
 * <p>路径为 {@code product/front/jwt/comment}，网关对 {@code /api/{模块}/front/jwt/**} 统一要求会员凭证
 * 并把身份注入请求，所以本类不做登录校验。会员 ID 由 {@link LoginUserUtils#requireCurrentUser} 从请求里取，
 * 不读请求体：前端传的会员身份与它传的订单项一样不可信。
 */
@Slf4j
@RestController
@RequestMapping("product/front/jwt/comment")
public class CommentFrontController {

    private final SpuCommentService spuCommentService;

    /**
     * 由 Spring 注入评价服务，创建后即可直接调用。
     *
     * @param spuCommentService 商品评价服务
     */
    public CommentFrontController(SpuCommentService spuCommentService) {
        this.spuCommentService = spuCommentService;
    }

    /**
     * 提交一条商品评价。
     *
     * <p>只允许对"属于当前会员且已完成"的订单项评价，且一项只能评一次：判据由订单服务给出，
     * 归属或状态不符返回 {@code 11200}/{@code 11201}，已评过返回 {@code 11203}，
     * 订单服务不可用时返回 {@code 11204} 且不落库。</p>
     *
     * @param request 当前请求，用于取出登录会员
     * @param vo 评价参数，{@code orderItemId}、{@code star} 与 {@code content} 均不能为空
     * @return 统一成功响应，不含业务数据
     */
    @PostMapping
    public R<Void> comment(HttpServletRequest request, @Valid @RequestBody CommentSubmitVo vo) {
        Long memberId = LoginUserUtils.requireCurrentUser(request).getId();
        log.info("提交商品评价：memberId={}, orderItemId={}", memberId, vo.getOrderItemId());
        spuCommentService.submitComment(memberId, vo);

        return R.ok();
    }

    /**
     * 分页查询我的评价。
     *
     * <p>只返回当前会员自己写的评价，按评价时间倒序；归属按 {@code member_id} 列判断，
     * 不看昵称（昵称可以改、可以重名）。</p>
     *
     * @param request 当前请求，用于取出登录会员
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为该会员的评价
     */
    @GetMapping("/my")
    public R<PageVO<SpuCommentEntity>> my(HttpServletRequest request, PageQuery query) {
        Long memberId = LoginUserUtils.requireCurrentUser(request).getId();
        return R.ok(spuCommentService.queryMyComments(memberId, query));
    }

    /**
     * 删除我的一条评价。
     *
     * <p>归属校验在服务端：评价不存在或不是当前会员写的都返回 {@code 11205}。
     * 删除后这一项可以重新评价 —— 唯一索引不再被占用。</p>
     *
     * @param request 当前请求，用于取出登录会员
     * @param commentId 评价主键
     * @return 统一成功响应，不含业务数据
     */
    @DeleteMapping("/{commentId}")
    public R<Void> delete(HttpServletRequest request, @PathVariable("commentId") Long commentId) {
        Long memberId = LoginUserUtils.requireCurrentUser(request).getId();
        log.info("删除商品评价：memberId={}, commentId={}", memberId, commentId);
        spuCommentService.deleteComment(commentId, memberId);

        return R.ok();
    }

}
