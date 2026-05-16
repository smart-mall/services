package coupon.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.exception.BaseException;
import common.utils.PageUtils;
import common.utils.Query;
import common.utils.R;
import coupon.dao.MemberPriceDao;
import coupon.entity.MemberPriceEntity;
import coupon.fegin.ProductFeignService;
import coupon.service.MemberPriceService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;


@Service("memberPriceService")
public class MemberPriceServiceImpl extends ServiceImpl<MemberPriceDao, MemberPriceEntity> implements MemberPriceService {
private final ProductFeignService productFeignService;

    public MemberPriceServiceImpl(ProductFeignService productFeignService) {
        this.productFeignService = productFeignService;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String key = (String) params.get("key");

        IPage<MemberPriceEntity> page = this.page(
                new Query<MemberPriceEntity>().getPage(params),
                new LambdaQueryWrapper<>()
        );

        // 去重 + 过滤 null
        List<Long> spuIds = page.getRecords().stream()
                .map(MemberPriceEntity::getSkuId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();


        R r = productFeignService.getSkuNames(spuIds);

        if (r.getCode() != 0) {
            throw new BaseException("远程服务调用失败" + r.getMsg() );
        }

        Map<Long, String> spuNameMap = r.getData(new TypeReference<>() {
        });

        page.getRecords().forEach(item -> item.setSkuName(spuNameMap.get(item.getSkuId())));



        if (key == null || key.trim().isEmpty()) {
            return new PageUtils(page);
        }

        List<MemberPriceEntity> collect = page.getRecords().stream()
                .filter(item -> key.equals(item.getId().toString()) || item.getSkuName().contains(key))
                .toList();

        PageUtils pageUtils = new PageUtils(page);
        pageUtils.setList(collect);
        return pageUtils;
    }
}