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
/**
 * 商品 SPU 积分设置的分页查询与按 spuId 批量删除实现。
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