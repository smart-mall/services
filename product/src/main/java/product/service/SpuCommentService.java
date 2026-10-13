package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import product.entity.SpuCommentEntity;
import product.vo.CommentSubmitVo;


import common.query.PageQuery;
/**
 * 商品评价服务：维护 spu 的用户评价。
 *
 * <p>会员端提交评价走 {@link #submitComment}：先向订单服务核对来源再落库。增删改由继承的
 * {@code IService} 提供，供后台脚手架 CRUD 使用。
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
}
