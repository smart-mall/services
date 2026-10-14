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
import org.springframework.transaction.annotation.Transactional;
import product.dao.CommentReplayDao;
import product.dao.SpuCommentDao;
import product.entity.CommentReplayEntity;
import product.entity.SpuCommentEntity;
import product.feign.MemberFeignService;
import product.feign.OrderFeignService;
import product.service.SpuCommentService;
import product.vo.CommentAuditReplyVo;
import product.vo.CommentAuditVo;
import product.vo.CommentReplySubmitVo;
import product.vo.CommentReplyVo;
import product.vo.CommentSubmitVo;
import product.vo.CommentSummaryVo;
import product.vo.MyCommentVo;
import product.vo.SpuCommentPageQuery;
import product.vo.SpuCommentVo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import common.query.PageQuery;
/**
 * 商品评价服务的默认实现：评价与回复内容读写 {@code pms_spu_comment}，回复关系读写 {@code pms_comment_replay}。
 *
 * <p>会员端：{@link #submitComment} 先向订单服务核对来源再落库；{@link #queryMyComments}、
 * {@link #deleteComment} 按评价行的 {@code member_id} 判归属；{@link #replyComment} 与
 * {@link #likeComment}/{@link #unlikeComment} 面向评论区的公开内容（评价与回复），同样要求登录。
 *
 * <p>{@link #submitComment} 落库后向会员服务赠送成长值：赠送失败只记日志，不回滚评价 ——
 * 评价本身已经成立，而 member 侧按订单号幂等，照日志重放一次就能补上。
 *
 * <p>公开接口：{@link #querySpuComments} 与 {@link #summaryOfSpu} 按 SPU 聚合，共同过滤
 * {@code show_status = 1} 与 {@code comment_type = 0}，对外只给公开投影
 * （{@link #maskNickName} 打码昵称，会员 ID 与 IP 只留在后台审核投影里）。
 *
 * <p>回复与直接评价同表，靠 {@code comment_type} 区分：回复行不带星级与订单项，商品字段只复制
 * {@code spu_id}，挂在哪条评价下记在 {@code pms_comment_replay}，只挂一层。
 *
 * <p>后台走 {@link #queryAuditPage} 与 {@link #auditComment}：列表按直接评价分页、回复不过滤显示状态，
 * 审核只改 {@code show_status}，不碰内容与计数。
 */
@Slf4j
@Service("spuCommentService")
public class SpuCommentServiceImpl extends ServiceImpl<SpuCommentDao, SpuCommentEntity> implements SpuCommentService {

    /** 直接评价的 {@code comment_type} 取值。 */
    private static final int COMMENT_TYPE_DIRECT = 0;

    /** 对评价的回复的 {@code comment_type} 取值。 */
    private static final int COMMENT_TYPE_REPLY = 1;

    /** 下架的 {@code show_status} 取值；后台审核把要撤下的行置为该值。 */
    private static final int SHOW_STATUS_HIDDEN = 0;

    /** 可见的 {@code show_status} 取值；0 是后台下架，公开接口一律不列。 */
    private static final int SHOW_STATUS_VISIBLE = 1;

    /** 点赞时的计数增量。 */
    private static final int LIKE_DELTA_ADD = 1;

    /** 取消点赞时的计数增量。 */
    private static final int LIKE_DELTA_REMOVE = -1;

    private final OrderFeignService orderFeignService;

    private final MemberFeignService memberFeignService;

    private final CommentReplayDao commentReplayDao;

    /**
     * 由 Spring 注入订单服务客户端、会员服务客户端与回复关系 Mapper，创建后即可直接调用。
     *
     * @param orderFeignService 订单服务客户端
     * @param memberFeignService 会员服务客户端，用于评价成功后赠送成长值
     * @param commentReplayDao 回复关系 Mapper
     */
    public SpuCommentServiceImpl(OrderFeignService orderFeignService, MemberFeignService memberFeignService,
                                 CommentReplayDao commentReplayDao) {
        this.orderFeignService = orderFeignService;
        this.memberFeignService = memberFeignService;
        this.commentReplayDao = commentReplayDao;
    }

    /** {@inheritDoc} */
    @Override
    public PageVO<CommentAuditVo> queryAuditPage(SpuCommentPageQuery query) {
        String key = query.getKey();
        boolean hasKey = key != null && !key.isBlank();

        IPage<SpuCommentEntity> page = this.page(query.toPage(), new LambdaQueryWrapper<SpuCommentEntity>()
                // 列表只按直接评价分页：回复挂在父评价的 replies 里，自己占一行会让总数虚高
                .eq(SpuCommentEntity::getCommentType, COMMENT_TYPE_DIRECT)
                .eq(query.getSkuId() != null, SpuCommentEntity::getSkuId, query.getSkuId())
                .eq(query.getSpuId() != null, SpuCommentEntity::getSpuId, query.getSpuId())
                .eq(query.getShowStatus() != null, SpuCommentEntity::getShowStatus, query.getShowStatus())
                .and(hasKey, wrapper -> wrapper.like(SpuCommentEntity::getContent, key)
                        .or().like(SpuCommentEntity::getSpuName, key)
                        .or().like(SpuCommentEntity::getMemberNickName, key))
                // 不过滤 show_status 与 star：已下架、没有星级的行都必须查得到，否则没有入口处理它们
                .orderByDesc(SpuCommentEntity::getCreateTime));

        Map<Long, List<CommentAuditReplyVo>> repliesByComment = queryAllRepliesByCommentIds(
                page.getRecords().stream().map(SpuCommentEntity::getId).toList());
        List<CommentAuditVo> rows = page.getRecords().stream()
                .map(comment -> toAuditVo(comment, repliesByComment))
                .toList();

        return new PageVO<>(page.getTotal(), rows);
    }

    /** {@inheritDoc} */
    @Override
    public void auditComment(Long commentId, Integer showStatus) {
        // 只认 0 与 1：别的值写进 tinyint(1) 会让可见性判断恒为 false，这一行在前台凭空消失且查不出原因
        if (!Integer.valueOf(SHOW_STATUS_HIDDEN).equals(showStatus)
                && !Integer.valueOf(SHOW_STATUS_VISIBLE).equals(showStatus)) {
            throw new BaseException(BaseCodeEnum.VALID_EXCEPTION);
        }

        if (this.getById(commentId) == null) {
            throw new BaseException(BaseCodeEnum.COMMENT_NOT_FOUND);
        }

        // 只带 id 与 show_status 走 updateById：其余字段为 null 不参与更新，审核不会顺带改写评价内容
        SpuCommentEntity target = new SpuCommentEntity();
        target.setId(commentId);
        target.setShowStatus(showStatus);
        this.updateById(target);
    }

    /** {@inheritDoc} */
    @Override
    public PageVO<MyCommentVo> queryMyComments(Long memberId, PageQuery query) {
        IPage<SpuCommentEntity> page = this.page(query.toPage(), new LambdaQueryWrapper<SpuCommentEntity>()
                .eq(SpuCommentEntity::getMemberId, memberId)
                // 只看直接评价：回复行的 member_id 也是本人，不过滤会把"我回复别人的"当成"我的评价"
                .eq(SpuCommentEntity::getCommentType, COMMENT_TYPE_DIRECT)
                .orderByDesc(SpuCommentEntity::getCreateTime));

        Map<Long, List<CommentReplyVo>> repliesByComment = queryRepliesByCommentIds(
                page.getRecords().stream().map(SpuCommentEntity::getId).toList());
        List<MyCommentVo> rows = page.getRecords().stream()
                .map(comment -> toMyCommentVo(comment, repliesByComment))
                .toList();

        return new PageVO<>(page.getTotal(), rows);
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
        comment.setShowStatus(SHOW_STATUS_VISIBLE);
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

        // 5. 赠送成长值，同样失败只记日志：评价已经落库，member 侧按订单号幂等，
        // 日志里留了订单号就能照着重放一次
        try {
            memberFeignService.grantGrowthByComment(memberId, target.getOrderSn());
        } catch (Exception e) {
            log.error("赠送评价成长值失败，orderSn={}", target.getOrderSn(), e);
        }
    }

    /** {@inheritDoc} */
    @Transactional
    @Override
    public void deleteComment(Long commentId, Long memberId) {
        // 先读一次是为了拿 orderItemId：评价行里没有订单号，撤回订单时间只能按订单项定位。
        // 归属不靠这里读到的内容来保证 —— 真正的闸门是下面 DELETE 语句里的 member_id
        SpuCommentEntity existing = this.getById(commentId);
        if (existing == null || !memberId.equals(existing.getMemberId())) {
            throw new BaseException(BaseCodeEnum.COMMENT_NOT_FOUND);
        }

        if (this.baseMapper.deleteOwn(commentId, memberId) == 0) {
            // 读到之后被别人删掉了，或这一行是回复（回复没有删除入口）：两种都按"评论不存在"处理
            throw new BaseException(BaseCodeEnum.COMMENT_NOT_FOUND);
        }

        // 级联清掉挂在这条评价下的回复，顺序不能反：回复内容行要靠关系行定位，先删关系行就找不到它们了
        this.baseMapper.deleteReplies(commentId);
        commentReplayDao.deleteByCommentId(commentId);

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
    public Integer likeComment(Long commentId, Long memberId) {
        return changeLikesCount(commentId, memberId, LIKE_DELTA_ADD);
    }

    /** {@inheritDoc} */
    @Override
    public Integer unlikeComment(Long commentId, Long memberId) {
        return changeLikesCount(commentId, memberId, LIKE_DELTA_REMOVE);
    }

    /** {@inheritDoc} */
    @Transactional
    @Override
    public CommentReplyVo replyComment(Long commentId, Long memberId, String memberNickName, CommentReplySubmitVo vo) {
        // 1. 目标必须是一条可见的直接评价：对回复再回复会让层级失控，一律拒绝
        SpuCommentEntity root = this.getById(commentId);
        if (!isVisibleDirectComment(root)) {
            throw new BaseException(BaseCodeEnum.COMMENT_NOT_FOUND);
        }

        // 2. 回复与直接评价同表，靠 comment_type 区分。商品字段只复制 spu_id：
        //    展示只用到内容、昵称与时间，其余快照留在根评价那一行，复制过来只会两处不一致
        SpuCommentEntity reply = new SpuCommentEntity();
        reply.setSpuId(root.getSpuId());
        reply.setMemberId(memberId);
        reply.setMemberNickName(memberNickName);
        reply.setContent(vo.getContent());
        reply.setCreateTime(new Date());
        reply.setCommentType(COMMENT_TYPE_REPLY);
        reply.setShowStatus(SHOW_STATUS_VISIBLE);
        reply.setLikesCount(0);
        reply.setReplyCount(0);
        this.save(reply);

        // 3. 关系行是"回复挂在哪条评价下"的唯一线索，级联删除也靠它定位
        CommentReplayEntity relation = new CommentReplayEntity();
        relation.setCommentId(commentId);
        relation.setReplyId(reply.getId());
        commentReplayDao.insert(relation);

        // 4. 根评价的回复数加一。与上面两步同一事务：计数写不进去，列表里的回复数就对不上
        this.baseMapper.increaseReplyCount(commentId);

        return toReplyVo(reply);
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

        Map<Long, List<CommentReplyVo>> repliesByComment = queryRepliesByCommentIds(
                page.getRecords().stream().map(SpuCommentEntity::getId).toList());
        List<SpuCommentVo> rows = page.getRecords().stream()
                .map(comment -> toPublicVo(comment, repliesByComment))
                .toList();

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
     * 点赞与取消点赞共用的实现：校验 → 按增量改计数 → 回读。
     *
     * @param commentId 评价或回复主键，不能为 {@code null}
     * @param memberId 当前登录会员 ID，不能为 {@code null}
     * @param delta 计数增量，点赞为 1、取消点赞为 -1
     * @return 更新后的获赞数，不会小于 0
     * @throws BaseException 这一行不可见或不是评价/回复时抛 {@code COMMENT_NOT_FOUND}；
     *         作者是当前会员时抛 {@code COMMENT_LIKE_SELF}
     */
    private Integer changeLikesCount(Long commentId, Long memberId, int delta) {
        SpuCommentEntity existing = this.getById(commentId);
        if (!isVisibleComment(existing)) {
            throw new BaseException(BaseCodeEnum.COMMENT_NOT_FOUND);
        }

        // 点赞与取消都要过这道闸：自己写的评价或回复谈不上点赞，取消也不该由自己按下
        if (memberId.equals(existing.getMemberId())) {
            throw new BaseException(BaseCodeEnum.COMMENT_LIKE_SELF);
        }

        if (this.baseMapper.updateLikesCount(commentId, delta) == 0) {
            // 读与写之间被后台下架：可见性条件写在 UPDATE 里，这里按"不可见"拒绝
            throw new BaseException(BaseCodeEnum.COMMENT_NOT_FOUND);
        }

        // 回读而不是在本地加减：别人可能同时也在点，本地算出的数会与库里的值越差越多
        SpuCommentEntity updated = this.getById(commentId);
        return updated == null ? 0 : nullToZero(updated.getLikesCount());
    }

    /**
     * 批量查这些评价下的回复，按评价 ID 分组。
     *
     * <p>分两次查而不是一条 join：回复挂在哪条评价下记在关系表，内容在评价表，
     * 先按评价 ID 取关系行、再按回复 ID 取内容行，各查各的表。</p>
     *
     * @param commentIds 根评价 ID 列表，允许为空；为空时返回空 Map
     * @return 评价 ID → 回复列表，按回复时间正序；没有回复的评价不在返回里
     */
    private Map<Long, List<CommentReplyVo>> queryRepliesByCommentIds(List<Long> commentIds) {
        // 空集合直接返回：拼出 IN () 是非法 SQL，这个坑在项目里出现过不止一次
        if (commentIds.isEmpty()) {
            return Map.of();
        }

        List<CommentReplayEntity> relations = commentReplayDao.selectList(new LambdaQueryWrapper<CommentReplayEntity>()
                .in(CommentReplayEntity::getCommentId, commentIds));
        if (relations.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> commentIdByReplyId = new LinkedHashMap<>();
        for (CommentReplayEntity relation : relations) {
            commentIdByReplyId.put(relation.getReplyId(), relation.getCommentId());
        }

        // 只给可见的回复：被后台下架的那些既不下发，也不该出现在计数之外的任何地方
        List<SpuCommentEntity> replies = this.list(new LambdaQueryWrapper<SpuCommentEntity>()
                .in(SpuCommentEntity::getId, commentIdByReplyId.keySet())
                .eq(SpuCommentEntity::getCommentType, COMMENT_TYPE_REPLY)
                .eq(SpuCommentEntity::getShowStatus, SHOW_STATUS_VISIBLE)
                .orderByAsc(SpuCommentEntity::getCreateTime));

        Map<Long, List<CommentReplyVo>> grouped = new LinkedHashMap<>();
        for (SpuCommentEntity reply : replies) {
            grouped.computeIfAbsent(commentIdByReplyId.get(reply.getId()), key -> new ArrayList<>()).add(toReplyVo(reply));
        }
        return grouped;
    }

    /**
     * 批量查这些评价下的全部回复，按评价 ID 分组，含已下架的。
     *
     * <p>公开链路那条 {@link #queryRepliesByCommentIds} 只给可见回复，后台这条不能这么筛：
     * 下架过的回复得回给审核页，否则没有任何入口把它们恢复。</p>
     *
     * @param commentIds 根评价 ID 列表，允许为空；为空时返回空 Map
     * @return 评价 ID → 回复列表，按回复时间正序；没有回复的评价不在返回里
     */
    private Map<Long, List<CommentAuditReplyVo>> queryAllRepliesByCommentIds(List<Long> commentIds) {
        // 空集合直接返回：拼出 IN () 是非法 SQL，这个坑在项目里出现过不止一次
        if (commentIds.isEmpty()) {
            return Map.of();
        }

        List<CommentReplayEntity> relations = commentReplayDao.selectList(new LambdaQueryWrapper<CommentReplayEntity>()
                .in(CommentReplayEntity::getCommentId, commentIds));
        if (relations.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> commentIdByReplyId = new LinkedHashMap<>();
        for (CommentReplayEntity relation : relations) {
            commentIdByReplyId.put(relation.getReplyId(), relation.getCommentId());
        }

        List<SpuCommentEntity> replies = this.list(new LambdaQueryWrapper<SpuCommentEntity>()
                .in(SpuCommentEntity::getId, commentIdByReplyId.keySet())
                .eq(SpuCommentEntity::getCommentType, COMMENT_TYPE_REPLY)
                .orderByAsc(SpuCommentEntity::getCreateTime));

        Map<Long, List<CommentAuditReplyVo>> grouped = new LinkedHashMap<>();
        for (SpuCommentEntity reply : replies) {
            grouped.computeIfAbsent(commentIdByReplyId.get(reply.getId()), key -> new ArrayList<>())
                    .add(toAuditReplyVo(reply));
        }
        return grouped;
    }

    /**
     * 判断一行是不是公开区里能看到的内容：有星级的可见直接评价，或可见的回复。
     *
     * <p>点赞以它为前提 —— 列表里看不到的行不该能点赞。直接评价要不要星级，口径与
     * {@link #querySpuComments} 的过滤条件一致；回复没有星级，但本来就是公开区的一部分。</p>
     *
     * @param comment 评价或回复行，允许为 {@code null}
     * @return {@code true} 表示这一行公开可见
     */
    private static boolean isVisibleComment(SpuCommentEntity comment) {
        if (comment == null || !Integer.valueOf(SHOW_STATUS_VISIBLE).equals(comment.getShowStatus())) {
            return false;
        }

        if (Integer.valueOf(COMMENT_TYPE_DIRECT).equals(comment.getCommentType())) {
            // 星级为空的行不进公开区：会员端提交时强制 1~5，这类行只可能来自后台脚手架
            return comment.getStar() != null;
        }
        return Integer.valueOf(COMMENT_TYPE_REPLY).equals(comment.getCommentType());
    }

    /**
     * 判断一条评价是不是公开区里能看到的那种：有星级的可见直接评价。
     *
     * <p>回复以它为前提：只允许对评价回复，不允许对回复再回复，层级才守得住一层。</p>
     *
     * @param comment 评价行，允许为 {@code null}
     * @return {@code true} 表示可见的直接评价
     */
    private static boolean isVisibleDirectComment(SpuCommentEntity comment) {
        return isVisibleComment(comment) && Integer.valueOf(COMMENT_TYPE_DIRECT).equals(comment.getCommentType());
    }

    /**
     * 把评价行转成公开投影，并挂上它的回复。
     *
     * @param comment 评价行，不能为 {@code null}
     * @param repliesByComment 评价 ID → 回复列表，见 {@link #queryRepliesByCommentIds}
     * @return 只含公开字段的投影，昵称已打码
     */
    private SpuCommentVo toPublicVo(SpuCommentEntity comment, Map<Long, List<CommentReplyVo>> repliesByComment) {
        SpuCommentVo vo = new SpuCommentVo();
        vo.setId(comment.getId());
        vo.setStar(comment.getStar());
        vo.setContent(comment.getContent());
        vo.setMemberNickName(maskNickName(comment.getMemberNickName()));
        vo.setSpuAttributes(comment.getSpuAttributes());
        vo.setCreateTime(comment.getCreateTime());
        vo.setLikesCount(nullToZero(comment.getLikesCount()));
        vo.setReplyCount(nullToZero(comment.getReplyCount()));
        vo.setReplies(repliesByComment.getOrDefault(comment.getId(), List.of()));
        return vo;
    }

    /**
     * 把评价行转成评论中心的投影，并挂上它的回复。
     *
     * @param comment 评价行，不能为 {@code null}
     * @param repliesByComment 评价 ID → 回复列表，见 {@link #queryRepliesByCommentIds}
     * @return 给评价人自己看的投影，含获赞数与回复
     */
    private MyCommentVo toMyCommentVo(SpuCommentEntity comment, Map<Long, List<CommentReplyVo>> repliesByComment) {
        MyCommentVo vo = new MyCommentVo();
        vo.setId(comment.getId());
        vo.setOrderItemId(comment.getOrderItemId());
        vo.setSkuId(comment.getSkuId());
        vo.setSpuName(comment.getSpuName());
        vo.setSpuAttributes(comment.getSpuAttributes());
        vo.setStar(comment.getStar());
        vo.setContent(comment.getContent());
        vo.setCreateTime(comment.getCreateTime());
        vo.setLikesCount(nullToZero(comment.getLikesCount()));
        vo.setReplyCount(nullToZero(comment.getReplyCount()));
        vo.setReplies(repliesByComment.getOrDefault(comment.getId(), List.of()));
        return vo;
    }

    /**
     * 把回复行转成公开投影。
     *
     * @param reply 回复行，不能为 {@code null}
     * @return 只含回复自身字段的投影，昵称已打码
     */
    private CommentReplyVo toReplyVo(SpuCommentEntity reply) {
        CommentReplyVo vo = new CommentReplyVo();
        vo.setId(reply.getId());
        vo.setMemberNickName(maskNickName(reply.getMemberNickName()));
        vo.setContent(reply.getContent());
        vo.setCreateTime(reply.getCreateTime());
        vo.setLikesCount(nullToZero(reply.getLikesCount()));
        return vo;
    }

    /**
     * 把评价行转成后台审核投影，并挂上它的回复。
     *
     * @param comment 评价行，不能为 {@code null}
     * @param repliesByComment 评价 ID → 回复列表，见 {@link #queryAllRepliesByCommentIds}
     * @return 含会员 ID 与显示状态的投影，昵称不打码
     */
    private CommentAuditVo toAuditVo(SpuCommentEntity comment, Map<Long, List<CommentAuditReplyVo>> repliesByComment) {
        CommentAuditVo vo = new CommentAuditVo();
        vo.setId(comment.getId());
        vo.setSpuId(comment.getSpuId());
        vo.setSkuId(comment.getSkuId());
        vo.setSpuName(comment.getSpuName());
        vo.setMemberId(comment.getMemberId());
        vo.setMemberNickName(comment.getMemberNickName());
        vo.setStar(comment.getStar());
        vo.setContent(comment.getContent());
        vo.setCreateTime(comment.getCreateTime());
        vo.setShowStatus(comment.getShowStatus());
        vo.setLikesCount(nullToZero(comment.getLikesCount()));
        vo.setReplyCount(nullToZero(comment.getReplyCount()));
        vo.setReplies(repliesByComment.getOrDefault(comment.getId(), List.of()));
        return vo;
    }

    /**
     * 把回复行转成后台审核投影。
     *
     * @param reply 回复行，不能为 {@code null}
     * @return 含会员 ID 与显示状态的投影，昵称不打码
     */
    private CommentAuditReplyVo toAuditReplyVo(SpuCommentEntity reply) {
        CommentAuditReplyVo vo = new CommentAuditReplyVo();
        vo.setId(reply.getId());
        vo.setMemberId(reply.getMemberId());
        vo.setMemberNickName(reply.getMemberNickName());
        vo.setContent(reply.getContent());
        vo.setCreateTime(reply.getCreateTime());
        vo.setShowStatus(reply.getShowStatus());
        vo.setLikesCount(nullToZero(reply.getLikesCount()));
        return vo;
    }

    /**
     * 把计数列的 {@code null} 归一成 0。
     *
     * @param value 计数列的值，允许为 {@code null}
     * @return 非空计数，{@code null} 时为 0
     */
    private static int nullToZero(Integer value) {
        return value == null ? 0 : value;
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
