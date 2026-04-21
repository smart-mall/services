package coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.utils.PageUtils;
import common.utils.Query;
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
import java.util.Map;
import java.util.stream.Collectors;


@Service("seckillSessionService")
public class SeckillSessionServiceImpl extends ServiceImpl<SeckillSessionDao, SeckillSessionEntity> implements SeckillSessionService {
    private final SeckillSkuRelationService seckillSkuRelationService;

    public SeckillSessionServiceImpl(SeckillSkuRelationService seckillSkuRelationService) {
        this.seckillSkuRelationService = seckillSkuRelationService;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {

        QueryWrapper<SeckillSessionEntity> queryWrapper = new QueryWrapper<>();

        String key = (String) params.get("key");

        if (!StringUtils.isEmpty(key)) {
            queryWrapper.eq("id",key);
        }

        IPage<SeckillSessionEntity> page = this.page(
                new Query<SeckillSessionEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    @Override
    public List<SeckillSessionEntity> getLates3DaySession() {

        //计算最近三天
        //查出这三天参与秒杀活动
        List<SeckillSessionEntity> list = this.baseMapper.selectList(
                new QueryWrapper<SeckillSessionEntity>()
                        .between("start_time", startTime(), endTime()));

//        查询活动参加秒杀的商品
        if (list != null && !list.isEmpty()) {
            //                当前活动id
            //查出sms_seckill_sku_relation表中关联的skuId
            //                设置所有参加活动的商品
            return list.stream().peek(session -> {
//                当前活动id
                Long id = session.getId();
                //查出sms_seckill_sku_relation表中关联的skuId
                List<SeckillSkuRelationEntity> relationSkus = seckillSkuRelationService.list(
                        new QueryWrapper<SeckillSkuRelationEntity>()
                                .eq("promotion_session_id", id));
//                设置所有参加活动的商品
                session.setRelationSkus(relationSkus);
            }).collect(Collectors.toList());
        }

        return null;
    }

    private String startTime() {
        LocalDate now = LocalDate.now();
        LocalTime min = LocalTime.MIN;
        LocalDateTime start = LocalDateTime.of(now, min);

        //格式化时间
        return start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String endTime() {
        LocalDate now = LocalDate.now();
        LocalDate plus = now.plusDays(2);
        LocalTime max = LocalTime.MAX;
        LocalDateTime end = LocalDateTime.of(plus, max);

        //格式化时间
        return end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }


}