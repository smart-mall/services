package coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.exception.BaseException;
import common.vo.PageVO;
import common.utils.R;
import coupon.dao.MemberPriceDao;
import coupon.entity.MemberPriceEntity;
import coupon.feign.ProductFeignService;
import coupon.service.MemberPriceService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;


import common.query.KeyPageQuery;
/**
 * 商品会员价分页查询实现，分页后按 SKU 回填商品名称。
 *
 * <p>无状态，线程安全。
 */
@Service("memberPriceService")
public class MemberPriceServiceImpl extends ServiceImpl<MemberPriceDao, MemberPriceEntity> implements MemberPriceService {
private final ProductFeignService productFeignService;

    /**
     * 创建会员价服务实例，注入商品远程查询客户端。
     *
     * @param productFeignService 商品服务远程调用客户端，用于回填 SKU 名称
     */
    public MemberPriceServiceImpl(ProductFeignService productFeignService) {
        this.productFeignService = productFeignService;
    }

    /**
     * {@inheritDoc}
     *
     * <p>当前页的 SKU 名称一次远程批量取回，不逐行调用。
     */
    @Override
    public PageVO<MemberPriceEntity> queryPage(KeyPageQuery query) {
        String key = query.getKey();

        IPage<MemberPriceEntity> page = this.page(
                query.toPage(),
                new LambdaQueryWrapper<>()
        );

        List<Long> spuIds = page.getRecords().stream()
                .map(MemberPriceEntity::getSkuId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();


        R<Map<Long, String>> r = productFeignService.getSkuNames(spuIds);

        if (r.getCode() != 0) {
            throw new BaseException(r.getCode(), "远程服务调用失败：" + r.getMsg());
        }

        Map<Long, String> spuNameMap = r.getData();

        page.getRecords().forEach(item -> item.setSkuName(spuNameMap.get(item.getSkuId())));



        if (key == null || key.trim().isEmpty()) {
            return new PageVO<>(page.getTotal(), page.getRecords());
        }

        List<MemberPriceEntity> collect = page.getRecords().stream()
                .filter(item -> key.equals(item.getId().toString()) || item.getSkuName().contains(key))
                .toList();

        PageVO<MemberPriceEntity> pageUtils = new PageVO<>(page.getTotal(), page.getRecords());
        pageUtils.setRows(collect);
        return pageUtils;
    }
}