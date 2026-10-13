package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.to.CommentBriefVo;
import common.vo.PageVO;
import product.entity.SpuCommentEntity;
import product.vo.CommentAuditVo;
import product.vo.CommentReplySubmitVo;
import product.vo.CommentReplyVo;
import product.vo.CommentSubmitVo;
import product.vo.CommentSummaryVo;
import product.vo.MyCommentVo;
import product.vo.SpuCommentPageQuery;
import product.vo.SpuCommentVo;

import java.util.List;

import common.query.PageQuery;
/**
 * 商品评价服务：维护 spu 的用户评价，以及评价上的回复与点赞。
 *
 * <p>三类调用方各有入口：会员端走 {@link #submitComment}、{@link #queryMyComments}、
 * {@link #deleteComment}、{@link #replyComment} 与 {@link #likeComment}/{@link #unlikeComment}
 * （提交评价前向订单服务核对来源，其余按评价行的 {@code member_id} 判归属）；
 * 商品详情页走 {@link #querySpuComments} 与 {@link #summaryOfSpu}（公开、按 SPU 聚合、昵称已打码）；
 * 订单服务走 {@link #listCommentedItems} 批量查哪些订单项已评价。
 *
 * <p>后台走 {@link #queryAuditPage} 与 {@link #auditComment}：列表按直接评价分页、回复挂在各自父评价下，
 * 审核只改显示状态。评价只能由会员在会员端发起，后台没有新增与删除入口。
 */
public interface SpuCommentService extends IService<SpuCommentEntity> {

    /**
     * 分页查询后台审核用的评价列表，回复挂在各自的父评价下。
     *
     * <p>不过滤显示状态：已下架的行也要能查到，否则没有入口把它们恢复。筛选条件由 {@code query} 给出，
     * 都不填时列出全部直接评价（{@code comment_type = 0}），按评价时间倒序。</p>
     *
     * <p>实现方必须保证：只按直接评价分页，回复不单独占一行 —— 回复行混进列表会让总数虚高、
     * 每条评价的回复数也对不上。</p>
     *
     * @param query 查询条件，不能为 {@code null}
     * @return 分页结果，{@code rows} 为审核投影，每行的 {@code replies} 含该评价下已下架的回复
     */
    PageVO<CommentAuditVo> queryAuditPage(SpuCommentPageQuery query);

    /**
     * 审核一条评价或回复：下架或恢复。
     *
     * <p>只改显示状态，不碰内容、星级与计数。下架一条直接评价后它连同下属回复一起从前台消失，
     * 回复行自身的显示状态不受影响；只想下架某一条回复时，在 {@code commentId} 处传回复的主键。</p>
     *
     * <p>幂等：把显示状态设成它当前的值不报错，重复调用结果一致。</p>
     *
     * @param commentId 评价或回复主键，不能为 {@code null}
     * @param showStatus 目标显示状态，只能为 0（下架）或 1（恢复）
     * @throws common.exception.BaseException 这一行不存在时抛 {@code COMMENT_NOT_FOUND}；
     *         {@code showStatus} 不是 0 或 1 时抛 {@code VALID_EXCEPTION}
     */
    void auditComment(Long commentId, Integer showStatus);

    /**
     * 提交一条商品评价。
     *
     * <p>实现方必须保证：先向 order 核对"这个订单项属于该会员且订单已完成"再落库，核对不通过不写任何数据；
     * 同一订单项只能落一条评价，并发提交时后一个必须被拒绝而不是写出第二条；落库后登记订单的评价时间，
     * 登记失败不回滚评价。</p>
     *
     * @param memberId 当前登录会员 ID，不能为 {@code null}
     * @param vo 评价参数，{@code orderItemId}、{@code star} 与 {@code content} 不能为空
     * @throws common.exception.BaseException 订单项不存在或不属于该会员
     *         {@code COMMENT_ORDER_ITEM_NOT_FOUND}、订单未完成 {@code COMMENT_ORDER_NOT_FINISHED}、
     *         已评过 {@code COMMENT_ALREADY_EXISTS}、订单服务不可用 {@code COMMENT_ORDER_UNAVAILABLE}
     */
    void submitComment(Long memberId, CommentSubmitVo vo);

    /**
     * 分页查询某个会员自己写的评价，带上获赞数与收到的回复。
     *
     * <p>按评价时间倒序，只返回 {@code member_id} 匹配的行：归属以该列为准，不看昵称 ——
     * 昵称可以改、可以重名，也可能为空。</p>
     *
     * <p>实现方必须保证：只取 {@code comment_type} 为 0 的直接评价。回复行的 {@code member_id}
     * 也是本人，漏掉这个条件会把"我回复别人的"混进评论中心。</p>
     *
     * @param memberId 当前登录会员 ID，不能为 {@code null}
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数，不能为 {@code null}
     * @return 分页结果，{@code rows} 为该会员的评价
     */
    PageVO<MyCommentVo> queryMyComments(Long memberId, PageQuery query);

    /**
     * 删除会员自己的一条评价。
     *
     * <p>实现方必须保证：把"这条评价是不是他的"写进删除语句的 {@code WHERE}，删不到行就抛
     * {@code COMMENT_NOT_FOUND}；删除生效后一并清掉这条评价下的回复内容行与回复关系行，
     * 再撤回订单上的评价时间，撤回失败不回滚删除。删除是物理删除，删掉之后同一订单项可以重新评价。</p>
     *
     * @param commentId 评价主键，不能为 {@code null}
     * @param memberId 当前登录会员 ID，不能为 {@code null}
     * @throws common.exception.BaseException 评价不存在、不属于该会员，或这一行是回复时抛 {@code COMMENT_NOT_FOUND}
     */
    void deleteComment(Long commentId, Long memberId);

    /**
     * 给一条评价或回复点赞。
     *
     * <p>实现方必须保证：只接受公开可见的行（可见的评价或可见的回复），其余情况抛
     * {@code COMMENT_NOT_FOUND}；这一行的作者就是当前会员时拒绝，抛 {@code COMMENT_LIKE_SELF}；
     * 返回自增后的计数。</p>
     *
     * <p>不做幂等：点赞人不落库，同一会员重复调用会一直加。这是刻意的取舍 ——
     * "点过没点过"由前端在本地记，服务端只维护一个计数列。</p>
     *
     * @param commentId 评价或回复主键，不能为 {@code null}
     * @param memberId 当前登录会员 ID，不能为 {@code null}
     * @return 更新后的获赞数，不会小于 0
     * @throws common.exception.BaseException 这一行不可见或不是评价/回复
     *         {@code COMMENT_NOT_FOUND}、给作者自己点赞 {@code COMMENT_LIKE_SELF}
     */
    Integer likeComment(Long commentId, Long memberId);

    /**
     * 取消给一条评价或回复的点赞。
     *
     * <p>校验与 {@link #likeComment} 完全相同，计数减一且不会减到负数。实现方不必也无法校验
     * "这个会员之前点过没有"：没有点赞记录表，服务端只知道当前计数。</p>
     *
     * @param commentId 评价或回复主键，不能为 {@code null}
     * @param memberId 当前登录会员 ID，不能为 {@code null}
     * @return 更新后的获赞数，不会小于 0
     * @throws common.exception.BaseException 这一行不可见或不是评价/回复
     *         {@code COMMENT_NOT_FOUND}、作者自己取消 {@code COMMENT_LIKE_SELF}
     */
    Integer unlikeComment(Long commentId, Long memberId);

    /**
     * 在一条评价下发表回复。
     *
     * <p>实现方必须保证：目标必须是可见的直接评价，否则抛 {@code COMMENT_NOT_FOUND}；
     * 回复内容行、回复关系行与根评价的回复数在同一事务内落库，不能只写成其中一部分；
     * 返回对象里的昵称已打码。回复只挂一层，不允许对回复再回复。</p>
     *
     * @param commentId 被回复的评价主键，不能为 {@code null}
     * @param memberId 发表回复的会员 ID，不能为 {@code null}
     * @param memberNickName 发表回复的会员昵称，取自登录身份，允许为 {@code null}
     * @param vo 回复参数，{@code content} 不能为空
     * @return 新增的回复，昵称已打码
     * @throws common.exception.BaseException 评价不可见或不是直接评价时抛 {@code COMMENT_NOT_FOUND}
     */
    CommentReplyVo replyComment(Long commentId, Long memberId, String memberNickName, CommentReplySubmitVo vo);

    /**
     * 批量查询这些订单项里哪些已经评价过，供订单服务装配会员订单列表。
     *
     * <p>服务间接口用：入参只有订单项 ID，因为调用方给出的订单项已经属于它要展示的那张单。</p>
     *
     * @param orderItemIds 订单项 ID 列表，允许为 {@code null}；为空时返回空列表
     * @return 评价摘要列表；都没有评价时为空列表
     */
    List<CommentBriefVo> listCommentedItems(List<Long> orderItemIds);

    /**
     * 分页查询某件商品的评价，供商品详情页的评论区使用。
     *
     * <p>只返回可见的直接评价（{@code show_status = 1} 且 {@code comment_type = 0}），按评价时间倒序。
     * 按 SPU 而不是 SKU 聚合：一款商品有多个规格时，按 SKU 列会让每个规格都显得没有评价，
     * 每行的 {@code spuAttributes} 已经写明买的是哪个规格。</p>
     *
     * <p>返回对象里的昵称已打码，且不含会员 ID 与 IP —— 这是公开接口。每行的回复随列表一起装配，
     * 同样只给可见的那些。</p>
     *
     * <p>实现方必须保证：过滤条件与 {@link #summaryOfSpu} 完全一致（只算可见的直接评价），
     * 否则统计里的总数与这里的条数会对不上。</p>
     *
     * @param spuId 商品 SPU ID，不能为 {@code null}
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数，不能为 {@code null}
     * @return 分页结果，{@code rows} 为公开投影
     */
    PageVO<SpuCommentVo> querySpuComments(Long spuId, PageQuery query);

    /**
     * 统计某件商品的评价：总数、平均分与星级分布。
     *
     * <p>统计口径与 {@link #querySpuComments} 一致，避免总数与列表条数对不上。回复行不计入：
     * 它们没有星级，也不该把一条评价撑成两条。</p>
     *
     * <p>实现方必须保证：过滤条件与 {@link #querySpuComments} 完全一致，且没有评价时返回
     * {@code total = 0} 与 {@code averageStar = 0}，而不是 {@code null}。</p>
     *
     * @param spuId 商品 SPU ID，不能为 {@code null}
     * @return 统计结果；没有评价时 {@code total} 为 0、{@code averageStar} 为 0
     */
    CommentSummaryVo summaryOfSpu(Long spuId);
}
