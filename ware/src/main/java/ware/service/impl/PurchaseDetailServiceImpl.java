package ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.exception.BaseException;
import common.utils.PageUtils;
import common.utils.Query;
import common.utils.R;
import org.springframework.stereotype.Service;
import ware.dao.PurchaseDetailDao;
import ware.entity.PurchaseDetailEntity;
import ware.entity.WareInfoEntity;
import ware.feign.ProductFeignService;
import ware.service.PurchaseDetailService;
import ware.service.WareInfoService;

import java.util.List;
import java.util.Map;
import java.util.Objects;


@Service("purchaseDetailService")
public class PurchaseDetailServiceImpl extends ServiceImpl<PurchaseDetailDao, PurchaseDetailEntity> implements PurchaseDetailService {
    private final ProductFeignService productFeignService;
    private final WareInfoService wareInfoService;

    public PurchaseDetailServiceImpl(ProductFeignService productFeignService, WareInfoService wareInfoService) {
        this.productFeignService = productFeignService;
        this.wareInfoService = wareInfoService;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        LambdaQueryWrapper<PurchaseDetailEntity> queryWrapper = new LambdaQueryWrapper<>();

        String key = (String) params.get("key");

        String status = (String) params.get("status");
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq(PurchaseDetailEntity::getStatus, status);
        }

        String wareId = (String) params.get("wareId");
        if (wareId != null && !wareId.isEmpty()) {
            queryWrapper.eq(PurchaseDetailEntity::getWareId, wareId);
        }

        IPage<PurchaseDetailEntity> page = this.page(
                new Query<PurchaseDetailEntity>().getPage(params),
                queryWrapper
        );

        // 添加wareName
        List<WareInfoEntity> list = wareInfoService.list();
        page.getRecords().forEach(item -> item.setWareName(list.stream()
                .filter(ware -> ware.getId().equals(item.getWareId()))
                .findFirst()
                .map(WareInfoEntity::getName)
                .orElse("")));

        // 添加skuName
        List<Long> skuIds = page.getRecords().stream()
                .map(PurchaseDetailEntity::getSkuId)
                .filter(Objects::nonNull)      // 过滤 null 值
                .distinct()                    // 去重
                .toList();

        if (!skuIds.isEmpty()) {
            R r = productFeignService.getSkuNames(skuIds);

            if (r.getCode() != 0) {
                throw new BaseException("远程服务异常" + r.getMsg());
            }

            Map<Long, String> skuNameMap = r.getData(new TypeReference<>() {
            });

            page.getRecords().forEach(item -> item.setSkuName(skuNameMap.get(item.getSkuId())));
        }


        // 关键字过滤
        if (key != null && !key.trim().isEmpty()) {
            page.getRecords().removeIf(item -> !item.getSkuName().contains(key) && !item.getSkuId().toString().contains(key));
        }

        return new PageUtils(page);
    }

}