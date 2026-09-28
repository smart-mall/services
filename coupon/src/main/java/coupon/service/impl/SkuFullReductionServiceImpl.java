package coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.exception.BaseException;
import common.to.SkuReductionTo;
import common.vo.PageVO;
import common.utils.R;
import coupon.dao.MemberPriceDao;
import coupon.dao.SkuFullReductionDao;
import coupon.dao.SkuLadderDao;
import coupon.entity.MemberPriceEntity;
import coupon.entity.SkuFullReductionEntity;
import coupon.entity.SkuLadderEntity;
import coupon.feign.ProductFeignService;
import coupon.service.SkuFullReductionService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;


import common.query.KeyPageQuery;
/**
 * 商品满减信息的分页查询，以及发布商品时满减、阶梯价、会员价三张表的一次性写入与按 SKU 删除。
 *
 * <p>无状态，线程安全。
 */
@Service("skuFullReductionService")
public class SkuFullReductionServiceImpl extends ServiceImpl<SkuFullReductionDao, SkuFullReductionEntity> implements SkuFullReductionService {
    private final SkuLadderDao skuLadderDao;
    private final MemberPriceDao memberPriceDao;
    private final ProductFeignService productFeignService;

    /**
     * 创建商品满减服务实例，注入阶梯价、会员价数据访问对象与商品远程查询客户端。
     *
     * @param skuLadderDao 阶梯价数据访问对象
     * @param memberPriceDao 会员价数据访问对象
     * @param productFeignService 商品服务远程调用客户端，用于回填 SKU 名称
     */
    public SkuFullReductionServiceImpl(SkuLadderDao skuLadderDao, MemberPriceDao memberPriceDao, ProductFeignService productFeignService) {
        this.skuLadderDao = skuLadderDao;
        this.memberPriceDao = memberPriceDao;
        this.productFeignService = productFeignService;
    }


    /**
     * {@inheritDoc}
     *
     * <p>当前页的 SKU 名称一次远程批量取回，不逐行调用。
     */
    @Override
    public PageVO<SkuFullReductionEntity> queryPage(KeyPageQuery query) {
        String key = query.getKey();

        IPage<SkuFullReductionEntity> page = this.page(
                query.toPage(),
                new LambdaQueryWrapper<>()
        );

        List<Long> spuIds = page.getRecords().stream()
                .map(SkuFullReductionEntity::getSkuId)
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

        List<SkuFullReductionEntity> collect = page.getRecords().stream()
                .filter(item -> key.equals(item.getId().toString()) || item.getSkuName().contains(key))
                .toList();

        PageVO<SkuFullReductionEntity> pageUtils = new PageVO<>(page.getTotal(), page.getRecords());
        pageUtils.setRows(collect);
        return pageUtils;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void saveSkuReduction(SkuReductionTo skuReductionTo) {
        if (skuReductionTo.getFullCount() > 0) {
            SkuLadderEntity skuLadderEntity = new SkuLadderEntity();
            BeanUtils.copyProperties(skuReductionTo, skuLadderEntity);
            skuLadderEntity.setAddOther(skuReductionTo.getCountStatus());
            skuLadderDao.insert(skuLadderEntity);
        }

        if (skuReductionTo.getFullPrice().compareTo(new BigDecimal("0")) > 0) {
            SkuFullReductionEntity skuFullReductionEntity = new SkuFullReductionEntity();
            BeanUtils.copyProperties(skuReductionTo, skuFullReductionEntity);
            skuFullReductionEntity.setAddOther(skuReductionTo.getPriceStatus());
            baseMapper.insert(skuFullReductionEntity);
        }



        List<SkuReductionTo.MemberPrice> memberPrice = skuReductionTo.getMemberPrice();
        if (memberPrice != null && !memberPrice.isEmpty()) {
            List<MemberPriceEntity>  memberPriceEntityList = memberPrice.stream().map(item -> {
                MemberPriceEntity memberPriceEntity = new MemberPriceEntity();
                memberPriceEntity.setSkuId(skuReductionTo.getSkuId());
                memberPriceEntity.setMemberLevelId(item.getId());
                memberPriceEntity.setMemberLevelName(item.getName());
                memberPriceEntity.setMemberPrice(item.getPrice());
                memberPriceEntity.setAddOther(1);
                return memberPriceEntity;
            }).filter( item -> item.getMemberPrice().compareTo(new BigDecimal("0")) > 0).toList();
            memberPriceDao.insert(memberPriceEntityList);
        }
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void deleteBySkuIds(List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return;
        }
        // 发布商品时 saveSkuReduction 一次写入这三张表，删除也一次清掉，
        // 和写入路径对称。少删一张，skuId 被新商品复用时就会带上别人的优惠。
        skuLadderDao.delete(new LambdaQueryWrapper<SkuLadderEntity>()
                .in(SkuLadderEntity::getSkuId, skuIds));
        baseMapper.delete(new LambdaQueryWrapper<SkuFullReductionEntity>()
                .in(SkuFullReductionEntity::getSkuId, skuIds));
        memberPriceDao.delete(new LambdaQueryWrapper<MemberPriceEntity>()
                .in(MemberPriceEntity::getSkuId, skuIds));
    }

}