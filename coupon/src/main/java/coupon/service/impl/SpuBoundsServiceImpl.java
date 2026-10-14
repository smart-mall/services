package coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.to.SkuBoundsVo;
import common.to.SkuScopeVo;
import common.vo.PageVO;
import common.utils.R;
import coupon.dao.SpuBoundsDao;
import coupon.entity.SpuBoundsEntity;
import coupon.feign.ProductFeignService;
import coupon.service.SpuBoundsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


import common.query.KeyPageQuery;
/**
 * 商品 SPU 积分设置的分页查询、按 skuId 批量读取、覆盖保存与按 spuId 批量删除。
 *
 * <p>无状态，线程安全。
 */
@Service("spuBoundsService")
@Slf4j
public class SpuBoundsServiceImpl extends ServiceImpl<SpuBoundsDao, SpuBoundsEntity> implements SpuBoundsService {
    private final ProductFeignService productFeignService;

    /**
     * 创建积分设置服务实例，注入商品远程查询客户端。
     *
     * @param productFeignService 商品服务远程调用客户端，用于回填 SPU 名称
     */
    public SpuBoundsServiceImpl(ProductFeignService productFeignService) {
        this.productFeignService = productFeignService;
    }

    /**
     * {@inheritDoc}
     *
     * <p>当前页的 SPU 名称一次远程批量取回，不逐行调用。
     */
    @Override
    public PageVO<SpuBoundsEntity> queryPage(KeyPageQuery query) {
        String key = query.getKey();

        IPage<SpuBoundsEntity> page = this.page(
                query.toPage(),
                new LambdaQueryWrapper<>()
        );

        List<Long> spuIds = page.getRecords().stream()
                .map(SpuBoundsEntity::getSpuId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();


        R<Map<Long, String>> r = productFeignService.getSpuNames(spuIds);

        if (r.getCode() != 0) {
            throw new BaseException(r.getCode(), "远程服务调用失败：" + r.getMsg());
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

    /**
     * {@inheritDoc}
     *
     * <p>skuId 到 spuId 的归属按既有的服务间接口一次批量取：积分设置挂在 SPU 上，而调用方手里
     * 只有 skuId，本服务不读 product 的表。
     */
    @Override
    public List<SkuBoundsVo> listBySkuIds(List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return List.of();
        }

        R<Map<Long, SkuScopeVo>> scopeResponse;
        try {
            scopeResponse = productFeignService.getSkuScopes(skuIds);
        } catch (Exception e) {
            log.error("查询商品层级归属失败，无法判定 SKU 的积分设置", e);
            throw new BaseException(BaseCodeEnum.COUPON_PRODUCT_UNAVAILABLE);
        }
        if (scopeResponse == null || scopeResponse.getCode() != 0 || scopeResponse.getData() == null) {
            log.error("查询商品层级归属失败，code={}", scopeResponse == null ? null : scopeResponse.getCode());
            throw new BaseException(BaseCodeEnum.COUPON_PRODUCT_UNAVAILABLE);
        }
        Map<Long, SkuScopeVo> skuScopes = scopeResponse.getData();

        List<Long> spuIds = skuScopes.values().stream()
                .map(SkuScopeVo::getSpuId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, SpuBoundsEntity> boundsBySpuId = new HashMap<>();
        if (!spuIds.isEmpty()) {
            for (SpuBoundsEntity row : this.list(new LambdaQueryWrapper<SpuBoundsEntity>()
                    .in(SpuBoundsEntity::getSpuId, spuIds))) {
                if (row.getSpuId() != null) {
                    boundsBySpuId.put(row.getSpuId(), row);
                }
            }
        }

        List<SkuBoundsVo> result = new ArrayList<>();
        for (Long skuId : skuIds) {
            SkuScopeVo scope = skuScopes.get(skuId);
            SpuBoundsEntity row = (scope == null || scope.getSpuId() == null)
                    ? null
                    : boundsBySpuId.get(scope.getSpuId());
            if (row == null) {
                continue;
            }
            SkuBoundsVo vo = new SkuBoundsVo();
            vo.setSkuId(skuId);
            vo.setBuyBounds(row.getBuyBounds());
            vo.setGrowBounds(row.getGrowBounds());
            result.add(vo);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>删除与插入在同一个事务内：中途失败时旧配置不会被先删掉。
     */
    @Override
    @Transactional
    public void saveSpuBounds(SpuBoundsEntity spuBounds) {
        if (spuBounds == null || spuBounds.getSpuId() == null) {
            throw new BaseException(BaseCodeEnum.VALID_EXCEPTION, "积分设置必须指定 SPU");
        }
        deleteBySpuIds(List.of(spuBounds.getSpuId()));
        this.save(spuBounds);
    }

    /**
     * {@inheritDoc}
     *
     * <p>整批 DELETE 在同一个事务内执行。
     */
    @Override
    @Transactional
    public void deleteBySpuIds(List<Long> spuIds) {
        if (spuIds == null || spuIds.isEmpty()) {
            return;
        }
        this.remove(new LambdaQueryWrapper<SpuBoundsEntity>().in(SpuBoundsEntity::getSpuId, spuIds));
    }

}