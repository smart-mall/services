package coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.exception.BaseException;
import common.vo.PageVO;
import common.utils.R;
import coupon.dao.SpuBoundsDao;
import coupon.entity.SpuBoundsEntity;
import coupon.fegin.ProductFeignService;
import coupon.service.SpuBoundsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;


import common.query.KeyPageQuery;
@Service("spuBoundsService")
@Slf4j
public class SpuBoundsServiceImpl extends ServiceImpl<SpuBoundsDao, SpuBoundsEntity> implements SpuBoundsService {
    private final ProductFeignService productFeignService;

    public SpuBoundsServiceImpl(ProductFeignService productFeignService) {
        this.productFeignService = productFeignService;
    }

    @Override
    public PageVO<SpuBoundsEntity> queryPage(KeyPageQuery query) {
        String key = query.getKey();

        IPage<SpuBoundsEntity> page = this.page(
                query.toPage(),
                new LambdaQueryWrapper<>()
        );

        // 去重 + 过滤 null
        List<Long> spuIds = page.getRecords().stream()
                .map(SpuBoundsEntity::getSpuId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();


        R<Map<Long, String>> r = productFeignService.getSpuNames(spuIds);

        if (r.getCode() != 0) {
            throw new BaseException("远程服务调用失败" + r.getMsg() );
        }

        Map<Long, String> spuNameMap = r.getData();

        page.getRecords().forEach(item -> item.setSpuName(spuNameMap.get(item.getSpuId())));



        if (key == null || key.trim().isEmpty()) {
            return new PageVO<>(page.getTotal(), page.getRecords());
        }

        List<SpuBoundsEntity> collect = page.getRecords().stream()
                .filter(item -> key.equals(item.getId().toString()) || item.getSpuName().contains(key))
                .toList();

        PageVO<SpuBoundsEntity> pageUtils = new PageVO<>(page.getTotal(), page.getRecords());
        pageUtils.setRows(collect);
        return pageUtils;
    }

    @Override
    @Transactional
    public void deleteBySpuIds(List<Long> spuIds) {
        if (spuIds == null || spuIds.isEmpty()) {
            return;
        }
        this.remove(new LambdaQueryWrapper<SpuBoundsEntity>().in(SpuBoundsEntity::getSpuId, spuIds));
    }

}