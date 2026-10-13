package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.to.CommentBriefVo;
import common.vo.PageVO;
import product.entity.SpuCommentEntity;
import product.vo.CommentSubmitVo;
import product.vo.CommentSummaryVo;
import product.vo.SpuCommentVo;

import java.util.List;

import common.query.PageQuery;
/**
 * 商品评价服务：维护 spu 的用户评价。
 *
 * <p>三类调用方各有入口：会员端走 {@link #submitComment}、{@link #queryMyComments} 与
 * {@link #deleteComment}（提交前向订单服务核对来源，查询与删除按评价行的 {@code member_id} 判归属）；
 * 商品详情页走 {@link #querySpuComments} 与 {@link #summaryOfSpu}（公开、按 SPU 聚合、昵称已打码）；
 * 订单服务走 {@link #listCommentedItems} 批量查哪些订单项已评价。
 *
 * <p>后台的增删改由继承的 {@code IService} 提供，不做业务校验。
 */
public interface SpuCommentService extends IService<SpuCommentEntity> {

    /**
     * 分页查询全部商品评价，不带筛选条件。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数，不能为 {@code null}
     * @return 分页结果，{@code rows} 为评价列表
     */
    PageVO<SpuCommentEntity> queryPage(PageQuery query);

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
     * 分页查询某个会员自己写的评价。
     *
     * <p>按评价时间倒序，只返回 {@code member_id} 匹配的行：归属以该列为准，不看昵称 ——
     * 昵称可以改、可以重名，也可能为空。</p>
     *
     * @param memberId 当前登录会员 ID，不能为 {@code null}
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数，不能为 {@code null}
     * @return 分页结果，{@code rows} 为该会员的评价
     */
    PageVO<SpuCommentEntity> queryMyComments(Long memberId, PageQuery query);

    /**
     * 删除会员自己的一条评价。
     *
     * <p>实现方必须保证：把"这条评价是不是他的"写进删除语句的 {@code WHERE}，删不到行就抛
     * {@code COMMENT_NOT_FOUND}；删除生效后同步撤回订单上的评价时间，撤回失败不回滚删除。
     * 删除是物理删除，删掉之后同一订单项可以重新评价。</p>
     *
     * @param commentId 评价主键，不能为 {@code null}
     * @param memberId 当前登录会员 ID，不能为 {@code null}
     * @throws common.exception.BaseException 评价不存在或不属于该会员时抛 {@code COMMENT_NOT_FOUND}
     */
    void deleteComment(Long commentId, Long memberId);

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
     * <p>返回对象里的昵称已打码，且不含会员 ID 与 IP —— 这是公开接口。</p>
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
     * <p>统计口径与 {@link #querySpuComments} 一致，避免总数与列表条数对不上。</p>
     *
     * <p>实现方必须保证：过滤条件与 {@link #querySpuComments} 完全一致，且没有评价时返回
     * {@code total = 0} 与 {@code averageStar = 0}，而不是 {@code null}。</p>
     *
     * @param spuId 商品 SPU ID，不能为 {@code null}
     * @return 统计结果；没有评价时 {@code total} 为 0、{@code averageStar} 为 0
     */
    CommentSummaryVo summaryOfSpu(Long spuId);
}
