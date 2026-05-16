package coupon.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.exception.BaseException;
import common.utils.PageUtils;
import common.utils.Query;
import common.utils.R;
import coupon.dao.SpuBoundsDao;
import coupon.entity.SpuBoundsEntity;
import coupon.fegin.ProductFeignService;
import coupon.service.SpuBoundsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;


@Service("spuBoundsService")
@Slf4j
public class SpuBoundsServiceImpl extends ServiceImpl<SpuBoundsDao, SpuBoundsEntity> implements SpuBoundsService {
    private final ProductFeignService productFeignService;

    public SpuBoundsServiceImpl(ProductFeignService productFeignService) {
        this.productFeignService = productFeignService;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String key = (String) params.get("key");

        IPage<SpuBoundsEntity> page = this.page(
                new Query<SpuBoundsEntity>().getPage(params),
                new LambdaQueryWrapper<>()
        );

        // 去重 + 过滤 null
        List<Long> spuIds = page.getRecords().stream()
                .map(SpuBoundsEntity::getSpuId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();


        R r = productFeignService.getSpuNames(spuIds);

        if (r.getCode() != 0) {
            throw new BaseException("远程服务调用失败" + r.getMsg() );
        }

        Map<Long, String> spuNameMap = r.getData(new TypeReference<>() {
        });

        page.getRecords().forEach(item -> item.setSpuName(spuNameMap.get(item.getSpuId())));



        if (key == null || key.trim().isEmpty()) {
            return new PageUtils(page);
        }

        List<SpuBoundsEntity> collect = page.getRecords().stream()
                .filter(item -> key.equals(item.getId().toString()) || item.getSpuName().contains(key))
                .toList();

        PageUtils pageUtils = new PageUtils(page);
        pageUtils.setList(collect);
        return pageUtils;
    }

}