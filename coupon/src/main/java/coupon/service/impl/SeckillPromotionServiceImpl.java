package coupon.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.exception.BaseException;
import common.utils.PageUtils;
import common.utils.Query;
import common.utils.R;
import coupon.dao.SeckillPromotionDao;
import coupon.entity.SeckillPromotionEntity;
import coupon.fegin.RenrenFeignService;
import coupon.service.SeckillPromotionService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;


@Service("seckillPromotionService")
public class SeckillPromotionServiceImpl extends ServiceImpl<SeckillPromotionDao, SeckillPromotionEntity> implements SeckillPromotionService {
    private final RenrenFeignService renrenFeignService;

    public SeckillPromotionServiceImpl(RenrenFeignService renrenFeignService) {
        this.renrenFeignService = renrenFeignService;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String key = (String)params.get("key");
        LambdaQueryWrapper<SeckillPromotionEntity> wrapper = new LambdaQueryWrapper<>();

        if (key != null && !key.isEmpty()) {
            wrapper.like(SeckillPromotionEntity::getTitle, key)
                    .or()
                    .like(SeckillPromotionEntity::getId, key);
        }

        IPage<SeckillPromotionEntity> page = this.page(
                new Query<SeckillPromotionEntity>().getPage(params),
                wrapper
        );

        List<Long> userIds = page.getRecords().stream()
                .map(SeckillPromotionEntity::getUserId)
                .filter(Objects::nonNull)      // 过滤 null
                .distinct()                     // 去重
                .toList();

        R r = renrenFeignService.getUserNames(userIds);
        if (r.getCode() != 0) {
            throw new BaseException("查询用户名失败" + r.getMsg());
        }

        Map<Long, String> data = r.getData(new TypeReference<>() {
        });

        page.getRecords().forEach(item -> {
            item.setUserName(data.get(item.getUserId()));
        });

        return new PageUtils(page);
    }

}