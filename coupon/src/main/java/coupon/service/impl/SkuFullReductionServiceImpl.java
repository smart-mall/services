package coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.exception.BaseException;
import common.to.SkuReductionTo;
import common.vo.PageVO;
import common.utils.Query;
import common.utils.R;
import coupon.dao.MemberPriceDao;
import coupon.dao.SkuFullReductionDao;
import coupon.dao.SkuLadderDao;
import coupon.entity.MemberPriceEntity;
import coupon.entity.SkuFullReductionEntity;
import coupon.entity.SkuLadderEntity;
import coupon.fegin.ProductFeignService;
import coupon.service.SkuFullReductionService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@Service("skuFullReductionService")
public class SkuFullReductionServiceImpl extends ServiceImpl<SkuFullReductionDao, SkuFullReductionEntity> implements SkuFullReductionService {
    private final SkuLadderDao skuLadderDao;
    private final MemberPriceDao memberPriceDao;
    private final ProductFeignService productFeignService;

    public SkuFullReductionServiceImpl(SkuLadderDao skuLadderDao, MemberPriceDao memberPriceDao, ProductFeignService productFeignService) {
        this.skuLadderDao = skuLadderDao;
        this.memberPriceDao = memberPriceDao;
        this.productFeignService = productFeignService;
    }


    @Override
    public PageVO<SkuFullReductionEntity> queryPage(Map<String, Object> params) {
        String key = (String) params.get("key");

        IPage<SkuFullReductionEntity> page = this.page(
                new Query<SkuFullReductionEntity>().getPage(params),
                new LambdaQueryWrapper<>()
        );

        // 去重 + 过滤 null
        List<Long> spuIds = page.getRecords().stream()
                .map(SkuFullReductionEntity::getSkuId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();


        R<Map<Long, String>> r = productFeignService.getSkuNames(spuIds);

        if (r.getCode() != 0) {
            throw new BaseException("远程服务调用失败" + r.getMsg() );
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