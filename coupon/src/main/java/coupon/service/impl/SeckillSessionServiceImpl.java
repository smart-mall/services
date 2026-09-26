package coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;
import coupon.dao.SeckillSessionDao;
import coupon.entity.SeckillSessionEntity;
import coupon.entity.SeckillSkuRelationEntity;
import coupon.service.SeckillSessionService;
import coupon.service.SeckillSkuRelationService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;


import common.query.KeyPageQuery;
/**
 * 秒杀场次的分页查询，以及近三天场次的关联商品装配实现。
 *
 * <p>无状态，线程安全。
 */
@Service("seckillSessionService")
public class SeckillSessionServiceImpl extends ServiceImpl<SeckillSessionDao, SeckillSessionEntity> implements SeckillSessionService {
    private final SeckillSkuRelationService seckillSkuRelationService;

    /**
     * 创建秒杀场次服务实例，注入秒杀商品关联服务。
     *
     * @param seckillSkuRelationService 秒杀商品关联服务，用于装配场次下的商品
     */
    public SeckillSessionServiceImpl(SeckillSkuRelationService seckillSkuRelationService) {
        this.seckillSkuRelationService = seckillSkuRelationService;
    }

    /** {@inheritDoc} */
    @Override
    public PageVO<SeckillSessionEntity> queryPage(KeyPageQuery query) {

        LambdaQueryWrapper<SeckillSessionEntity> queryWrapper = new LambdaQueryWrapper<>();

        String key = query.getKey();

        if (!StringUtils.isEmpty(key)) {
            queryWrapper.like(SeckillSessionEntity::getName, key)
                    .or()
                    .eq(SeckillSessionEntity::getId, key);
        }

        IPage<SeckillSessionEntity> page = this.page(
                query.toPage(),
                queryWrapper
        );

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    /**
     * {@inheritDoc}
     *
     * <p>时间窗口按服务器本地时区取自然日：起点为今天 00:00:00，终点为第三天 23:59:59。
     */
    @Override
    public List<SeckillSessionEntity> getLates3DaySession() {

        List<SeckillSessionEntity> list = this.baseMapper.selectList(
                new LambdaQueryWrapper<SeckillSessionEntity>()
                        .between(SeckillSessionEntity::getStartTime, startTime(), endTime()));

        if (list != null && !list.isEmpty()) {
            return list.stream().peek(session -> {
                Long id = session.getId();
                List<SeckillSkuRelationEntity> relationSkus = seckillSkuRelationService.list(
                        new LambdaQueryWrapper<SeckillSkuRelationEntity>()
                                .eq(SeckillSkuRelationEntity::getPromotionSessionId, id));
                session.setRelationSkus(relationSkus);
            }).collect(Collectors.toList());
        }

        return null;
    }

    /**
     * 返回今天 00:00:00 的字符串，作为场次开始时间的查询下界。
     *
     * @return {@code yyyy-MM-dd HH:mm:ss} 格式的当天起始时刻
     */
    private String startTime() {
        LocalDate now = LocalDate.now();
        LocalTime min = LocalTime.MIN;
        LocalDateTime start = LocalDateTime.of(now, min);

        return start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 返回第三天 23:59:59 的字符串，作为场次开始时间的查询上界。
     *
     * <p>格式串只到秒，{@code LocalTime.MAX} 的纳秒部分会被丢掉，上界因此不含最后一秒的小数部分。
     *
     * @return {@code yyyy-MM-dd HH:mm:ss} 格式的第三天结束时刻
     */
    private String endTime() {
        LocalDate now = LocalDate.now();
        LocalDate plus = now.plusDays(2);
        LocalTime max = LocalTime.MAX;
        LocalDateTime end = LocalDateTime.of(plus, max);

        return end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }


}