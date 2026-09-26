package coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.exception.BaseException;
import common.vo.PageVO;
import common.utils.R;
import coupon.dao.SeckillPromotionDao;
import coupon.entity.SeckillPromotionEntity;
import coupon.fegin.RenrenFeignService;
import coupon.service.SeckillPromotionService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;


import common.query.KeyPageQuery;
/**
 * 秒杀活动分页查询实现，{@code key} 非空时同时模糊匹配活动标题与活动 ID，并回填创建人名称。
 *
 * <p>无状态，线程安全。
 */
@Service("seckillPromotionService")
public class SeckillPromotionServiceImpl extends ServiceImpl<SeckillPromotionDao, SeckillPromotionEntity> implements SeckillPromotionService {
    private final RenrenFeignService renrenFeignService;

    /**
     * 创建秒杀活动服务实例，注入用户远程查询客户端。
     *
     * @param renrenFeignService 用户服务远程调用客户端，用于回填活动创建人名称
     */
    public SeckillPromotionServiceImpl(RenrenFeignService renrenFeignService) {
        this.renrenFeignService = renrenFeignService;
    }

    /**
     * {@inheritDoc}
     *
     * <p>当前页的创建人名称一次远程批量取回，不逐行调用。
     */
    @Override
    public PageVO<SeckillPromotionEntity> queryPage(KeyPageQuery query) {
        String key = query.getKey();
        LambdaQueryWrapper<SeckillPromotionEntity> wrapper = new LambdaQueryWrapper<>();

        if (key != null && !key.isEmpty()) {
            wrapper.like(SeckillPromotionEntity::getTitle, key)
                    .or()
                    .like(SeckillPromotionEntity::getId, key);
        }

        IPage<SeckillPromotionEntity> page = this.page(
                query.toPage(),
                wrapper
        );

        List<Long> userIds = page.getRecords().stream()
                .map(SeckillPromotionEntity::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        R<Map<Long, String>> r = renrenFeignService.getUserNames(userIds);
        if (r.getCode() != 0) {
            throw new BaseException(r.getCode(), "查询用户名失败：" + r.getMsg());
        }

        Map<Long, String> data = r.getData();

        page.getRecords().forEach(item -> {
            item.setUserName(data.get(item.getUserId()));
        });

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}