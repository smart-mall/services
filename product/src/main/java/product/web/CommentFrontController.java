package product.web;

import common.query.PageQuery;
import common.utils.LoginUserUtils;
import common.utils.R;
import common.vo.MemberResponseVo;
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
import product.service.SpuCommentService;
import product.vo.CommentReplySubmitVo;
import product.vo.CommentReplyVo;
import product.vo.CommentSubmitVo;
import product.vo.MyCommentVo;

/**
 * 前台需要登录的商品评价接口：提交、查看我的评价、删除我的一条评价，以及点赞、取消点赞与回复。
 *
 * <p>路径为 {@code product/front/jwt/comment}，网关对 {@code /api/{模块}/front/jwt/**} 统一要求会员凭证
 * 并把身份注入请求，所以本类不做登录校验。会员 ID 由 {@link LoginUserUtils#requireCurrentUser} 从请求里取，
 * 不读请求体：前端传的会员身份与它传的订单项一样不可信。
 *
 * <p>点赞与回复都要求登录：不登录就无从判断"这条内容是不是他自己写的"，也就无法执行
 * "不能给自己点赞"这条规则。
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
     * <p>只返回当前会员自己写的直接评价，按评价时间倒序；归属按 {@code member_id} 列判断，
     * 不看昵称（昵称可以改、可以重名）。每行带上获赞数与别人在这条评价下的回复。</p>
     *
     * @param request 当前请求，用于取出登录会员
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为该会员的评价
     */
    @GetMapping("/my")
    public R<PageVO<MyCommentVo>> my(HttpServletRequest request, PageQuery query) {
        Long memberId = LoginUserUtils.requireCurrentUser(request).getId();
        return R.ok(spuCommentService.queryMyComments(memberId, query));
    }

    /**
     * 删除我的一条评价。
     *
     * <p>归属校验在服务端：评价不存在或不是当前会员写的都返回 {@code 11205}。
     * 删除后这一项可以重新评价 —— 唯一索引随之释放；这条评价下的回复一并删除。</p>
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

    /**
     * 给一条评价或回复点赞。
     *
     * <p>不做幂等：同一会员重复调用会重复加，点过没点过由前端在本地记。作者是当前会员时
     * 返回 {@code 11206}，这一行不可见或不是评价/回复时返回 {@code 11205}。</p>
     *
     * @param request 当前请求，用于取出登录会员
     * @param commentId 评价或回复主键
     * @return 更新后的获赞数，不会小于 0
     */
    @PostMapping("/{commentId}/like")
    public R<Integer> like(HttpServletRequest request, @PathVariable("commentId") Long commentId) {
        Long memberId = LoginUserUtils.requireCurrentUser(request).getId();
        log.info("点赞：memberId={}, commentId={}", memberId, commentId);

        return R.ok(spuCommentService.likeComment(commentId, memberId));
    }

    /**
     * 取消给一条评价或回复的点赞。
     *
     * <p>校验与点赞完全相同，计数减一且不会减到负数。服务端不校验"之前点过没有"：
     * 点赞人不落库，只有计数。</p>
     *
     * @param request 当前请求，用于取出登录会员
     * @param commentId 评价或回复主键
     * @return 更新后的获赞数，不会小于 0
     */
    @DeleteMapping("/{commentId}/like")
    public R<Integer> unlike(HttpServletRequest request, @PathVariable("commentId") Long commentId) {
        Long memberId = LoginUserUtils.requireCurrentUser(request).getId();
        log.info("取消点赞：memberId={}, commentId={}", memberId, commentId);

        return R.ok(spuCommentService.unlikeComment(commentId, memberId));
    }

    /**
     * 在一条评价下发表回复。
     *
     * <p>回复人与昵称取自网关注入的登录身份，不从请求体读；只允许对可见的直接评价回复，
     * 对回复再回复一律返回 {@code 11205}。</p>
     *
     * @param request 当前请求，用于取出登录会员
     * @param commentId 被回复的评价主键
     * @param vo 回复参数，{@code content} 不能为空
     * @return 新增的回复，昵称已打码
     */
    @PostMapping("/{commentId}/reply")
    public R<CommentReplyVo> reply(HttpServletRequest request, @PathVariable("commentId") Long commentId,
                                   @Valid @RequestBody CommentReplySubmitVo vo) {
        MemberResponseVo member = LoginUserUtils.requireCurrentUser(request);
        log.info("回复商品评价：memberId={}, commentId={}", member.getId(), commentId);

        return R.ok(spuCommentService.replyComment(commentId, member.getId(), member.getNickname(), vo));
    }

}
