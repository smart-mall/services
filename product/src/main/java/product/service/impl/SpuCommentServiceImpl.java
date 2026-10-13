package product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.to.CommentBriefVo;
import common.to.CommentTargetVo;
import common.utils.R;
import common.vo.PageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import product.dao.SpuCommentDao;
import product.entity.SpuCommentEntity;
import product.feign.OrderFeignService;
import product.service.SpuCommentService;
import product.vo.CommentSubmitVo;

import java.util.Date;
import java.util.List;

import common.query.PageQuery;
/**
 * 商品评价服务的默认实现，基于 MyBatis-Plus 的 {@code ServiceImpl} 读写 {@code pms_spu_comment}。
 *
 * <p>会员端的三件事都在本类：{@link #submitComment} 先核对来源再落库，{@link #queryMyComments} 与
 * {@link #deleteComment} 按评价行的 {@code member_id} 判归属。分页查询与继承来的增删改供后台使用，
 * 不做业务校验。
 */
@Slf4j
@Service("spuCommentService")
public class SpuCommentServiceImpl extends ServiceImpl<SpuCommentDao, SpuCommentEntity> implements SpuCommentService {

    /** 直接评价的 {@code comment_type} 取值；1 是对评论的回复，不由本类写入。 */
    private static final int COMMENT_TYPE_DIRECT = 0;

    private final OrderFeignService orderFeignService;

    /**
     * 由 Spring 注入订单服务的 Feign 客户端，创建后即可直接调用。
     *
     * @param orderFeignService 订单服务客户端
     */
    public SpuCommentServiceImpl(OrderFeignService orderFeignService) {
        this.orderFeignService = orderFeignService;
    }

    /** {@inheritDoc} */
    @Override
    public PageVO<SpuCommentEntity> queryPage(PageQuery query) {
        IPage<SpuCommentEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    /** {@inheritDoc} */
    @Override
    public PageVO<SpuCommentEntity> queryMyComments(Long memberId, PageQuery query) {
        IPage<SpuCommentEntity> page = this.page(query.toPage(), new LambdaQueryWrapper<SpuCommentEntity>()
                .eq(SpuCommentEntity::getMemberId, memberId)
                .orderByDesc(SpuCommentEntity::getCreateTime));

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    /** {@inheritDoc} */
    @Override
    public void submitComment(Long memberId, CommentSubmitVo vo) {
        // 1. 向订单服务核对来源：归属与状态判据在那边算，这里只接受结论
        R<CommentTargetVo> targetResult = orderFeignService.getCommentTarget(vo.getOrderItemId(), memberId);
        if (targetResult == null || targetResult.getData() == null) {
            if (targetResult != null && targetResult.getCode() != R.SUCCESS_CODE) {
                // 原样透传订单侧的业务码：11200 与 11201 要给出不同文案，笼统报"服务不可用"会误导
                throw new BaseException(targetResult.getCode(), targetResult.getMsg());
            }
            throw new BaseException(BaseCodeEnum.COMMENT_ORDER_UNAVAILABLE);
        }
        CommentTargetVo target = targetResult.getData();

        // 2. 先查再插：让"已经评过"落到一个可读的业务码上，而不是等唯一键抛异常
        long commented = this.count(new LambdaQueryWrapper<SpuCommentEntity>()
                .eq(SpuCommentEntity::getOrderItemId, target.getOrderItemId()));
        if (commented > 0) {
            throw new BaseException(BaseCodeEnum.COMMENT_ALREADY_EXISTS);
        }

        // 3. 落库。并发提交的两个请求都会通过第 2 步，由 uk_order_item 唯一索引兜住后一个
        SpuCommentEntity comment = new SpuCommentEntity();
        comment.setOrderItemId(target.getOrderItemId());
        comment.setMemberId(memberId);
        comment.setSkuId(target.getSkuId());
        comment.setSpuId(target.getSpuId());
        comment.setSpuName(target.getSpuName());
        comment.setSpuAttributes(target.getSpuAttributes());
        comment.setMemberNickName(target.getMemberUsername());
        comment.setStar(vo.getStar());
        comment.setContent(vo.getContent());
        comment.setCreateTime(new Date());
        comment.setCommentType(COMMENT_TYPE_DIRECT);
        // 新评价默认可见；下架某条评价是后台的审核动作，本链路不做
        comment.setShowStatus(1);
        // 点赞数与回复数是计数，写 0 而不是留 NULL，前端不必为它们多一种空值分支
        comment.setLikesCount(0);
        comment.setReplyCount(0);
        // member_ip 留 NULL：请求经网关转发后 remoteAddr 是网关地址，写进去不是会员的真实 IP
        try {
            this.save(comment);
        } catch (DuplicateKeyException e) {
            log.info("并发提交同一订单项的评价，唯一索引拦下后一个：orderItemId={}", target.getOrderItemId());
            throw new BaseException(BaseCodeEnum.COMMENT_ALREADY_EXISTS);
        }

        // 4. 登记订单的评价时间。失败只记日志：评价行已经落库，那个时间列是展示用，重试没有意义
        try {
            orderFeignService.markCommented(target.getOrderSn(), memberId);
        } catch (Exception e) {
            log.error("登记订单评价时间失败，orderSn={}", target.getOrderSn(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void deleteComment(Long commentId, Long memberId) {
        // 先读一次是为了拿 orderItemId：评价行里没有订单号，撤回订单时间只能按订单项定位。
        // 归属不靠这次读来保证 —— 真正的闸门是下面 DELETE 语句里的 member_id
        SpuCommentEntity existing = this.getById(commentId);
        if (existing == null || !memberId.equals(existing.getMemberId())) {
            throw new BaseException(BaseCodeEnum.COMMENT_NOT_FOUND);
        }

        if (this.baseMapper.deleteOwn(commentId, memberId) == 0) {
            // 读到之后被别人删掉了：并发删除只有一个生效，另一个落到这里
            throw new BaseException(BaseCodeEnum.COMMENT_NOT_FOUND);
        }

        // 撤回订单上的评价时间。失败只记日志：评价已经删掉，那列是展示用，
        // 让它拦住删除会把"删不掉自己的评价"变成用户可见的故障
        try {
            orderFeignService.unmarkCommented(existing.getOrderItemId(), memberId);
        } catch (Exception e) {
            log.error("撤回订单评价时间失败，orderItemId={}", existing.getOrderItemId(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<CommentBriefVo> listCommentedItems(List<Long> orderItemIds) {
        // 空集合直接返回：拼出 IN () 是非法 SQL，这个坑在项目里出现过不止一次
        if (orderItemIds == null || orderItemIds.isEmpty()) {
            return List.of();
        }

        return this.list(new LambdaQueryWrapper<SpuCommentEntity>()
                        .in(SpuCommentEntity::getOrderItemId, orderItemIds))
                .stream()
                .map(comment -> {
                    CommentBriefVo vo = new CommentBriefVo();
                    vo.setCommentId(comment.getId());
                    vo.setOrderItemId(comment.getOrderItemId());
                    vo.setStar(comment.getStar());
                    vo.setContent(comment.getContent());
                    vo.setCreateTime(comment.getCreateTime());
                    return vo;
                })
                .toList();
    }

}
