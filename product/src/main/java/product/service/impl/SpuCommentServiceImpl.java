package product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import product.vo.CommentSummaryVo;
import product.vo.SpuCommentVo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import common.query.PageQuery;
/**
 * 商品评价服务的默认实现，基于 MyBatis-Plus 的 {@code ServiceImpl} 读写 {@code pms_spu_comment}。
 *
 * <p>会员端：{@link #submitComment} 先向订单服务核对来源再落库，{@link #queryMyComments} 与
 * {@link #deleteComment} 按评价行的 {@code member_id} 判归属。
 *
 * <p>公开接口：{@link #querySpuComments} 与 {@link #summaryOfSpu} 按 SPU 聚合，共同过滤
 * {@code show_status = 1} 与 {@code comment_type = 0}，对外只给 {@link product.vo.SpuCommentVo}
 * 这一份投影（{@link #maskNickName} 打码昵称，会员 ID 与 IP 不出本类）。
 *
 * <p>后台的分页查询与继承来的增删改不做业务校验。
 */
@Slf4j
@Service("spuCommentService")
public class SpuCommentServiceImpl extends ServiceImpl<SpuCommentDao, SpuCommentEntity> implements SpuCommentService {

    /** 直接评价的 {@code comment_type} 取值；1 是对评论的回复，不由本类写入。 */
    private static final int COMMENT_TYPE_DIRECT = 0;

    /** 可见的 {@code show_status} 取值；0 是后台下架，公开接口一律不列。 */
    private static final int SHOW_STATUS_VISIBLE = 1;

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
        // 归属不靠这里读到的内容来保证 —— 真正的闸门是下面 DELETE 语句里的 member_id
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

    /** {@inheritDoc} */
    @Override
    public PageVO<SpuCommentVo> querySpuComments(Long spuId, PageQuery query) {
        IPage<SpuCommentEntity> page = this.page(query.toPage(), new LambdaQueryWrapper<SpuCommentEntity>()
                .eq(SpuCommentEntity::getSpuId, spuId)
                // 只列可见的直接评价：show_status 为 0 是后台下架的，comment_type 为 1 是对评价的回复
                .eq(SpuCommentEntity::getShowStatus, SHOW_STATUS_VISIBLE)
                .eq(SpuCommentEntity::getCommentType, COMMENT_TYPE_DIRECT)
                // 星级为空的行不进公开区：会员端提交时强制 1~5，这类行只可能来自后台脚手架，
                // 放进来会让统计的总数与星级分布之和不等
                .isNotNull(SpuCommentEntity::getStar)
                .orderByDesc(SpuCommentEntity::getCreateTime));

        List<SpuCommentVo> rows = page.getRecords().stream().map(this::toPublicVo).toList();
        return new PageVO<>(page.getTotal(), rows);
    }

    /** {@inheritDoc} */
    @Override
    public CommentSummaryVo summaryOfSpu(Long spuId) {
        // 一次按星级分组把总数、分布与平均分都算出来，省掉单独发一条 count 查询；
        // 聚合要写列名，用 QueryWrapper 而不是 LambdaQueryWrapper，条件与列表那条保持一致。
        // star 必须显式 CAST：它是 tinyint(1)，Connector/J 默认把该类型读成 Boolean，
        // 走 selectMaps 拿到的是 Boolean 而不是数字，直接强转 Number 会抛 ClassCastException
        List<Map<String, Object>> grouped = this.baseMapper.selectMaps(new QueryWrapper<SpuCommentEntity>()
                .select("CAST(star AS SIGNED) AS star", "COUNT(*) AS cnt")
                .eq("spu_id", spuId)
                .eq("show_status", SHOW_STATUS_VISIBLE)
                .eq("comment_type", COMMENT_TYPE_DIRECT)
                .isNotNull("star")
                .groupBy("star"));

        Map<Integer, Integer> starCounts = new LinkedHashMap<>();
        long total = 0;
        long starSum = 0;
        for (Map<String, Object> row : grouped) {
            Number star = (Number) row.get("star");
            Number count = (Number) row.get("cnt");
            if (star == null || count == null) {
                continue;
            }
            starCounts.put(star.intValue(), count.intValue());
            total += count.longValue();
            starSum += star.longValue() * count.longValue();
        }

        CommentSummaryVo summary = new CommentSummaryVo();
        summary.setTotal(total);
        // 没有评价时平均分给 0 而不是 null：前端不必为"暂无评价"再写一个空值分支
        summary.setAverageStar(total == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(starSum).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP));
        summary.setStarCounts(starCounts);
        return summary;
    }

    /**
     * 把评价行转成公开投影。
     *
     * @param comment 评价行，不能为 {@code null}
     * @return 只含公开字段的投影，昵称已打码
     */
    private SpuCommentVo toPublicVo(SpuCommentEntity comment) {
        SpuCommentVo vo = new SpuCommentVo();
        vo.setId(comment.getId());
        vo.setStar(comment.getStar());
        vo.setContent(comment.getContent());
        vo.setMemberNickName(maskNickName(comment.getMemberNickName()));
        vo.setSpuAttributes(comment.getSpuAttributes());
        vo.setCreateTime(comment.getCreateTime());
        return vo;
    }

    /**
     * 给公开接口用的昵称打码。
     *
     * <p>评论区的昵称就是会员名，公开页原样展示等于把用户名摊开。只保留前三个字符（不足三个就只留第一个），
     * 其余用 {@code ***} 顶掉。</p>
     *
     * <p>放在服务端做：昵称一旦原文发出去，前端哪个位置漏处理就等于泄露，出口只有这一处才管得住。</p>
     *
     * @param nickName 原始昵称，允许为 {@code null} 或空白
     * @return 打码后的昵称；没有昵称时返回"匿名会员"
     */
    private static String maskNickName(String nickName) {
        if (nickName == null || nickName.isBlank()) {
            return "匿名会员";
        }

        String trimmed = nickName.trim();
        return trimmed.length() <= 2 ? trimmed.charAt(0) + "***" : trimmed.substring(0, 3) + "***";
    }

}
