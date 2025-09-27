package coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.to.SkuReductionTo;
import common.utils.PageUtils;
import common.utils.Query;
import coupon.dao.MemberPriceDao;
import coupon.dao.SkuFullReductionDao;
import coupon.dao.SkuLadderDao;
import coupon.entity.MemberPriceEntity;
import coupon.entity.SkuFullReductionEntity;
import coupon.entity.SkuLadderEntity;
import coupon.service.SkuFullReductionService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


@Service("skuFullReductionService")
public class SkuFullReductionServiceImpl extends ServiceImpl<SkuFullReductionDao, SkuFullReductionEntity> implements SkuFullReductionService {
    private final SkuLadderDao skuLadderDao;
    private final MemberPriceDao memberPriceDao;

    public SkuFullReductionServiceImpl(SkuLadderDao skuLadderDao, MemberPriceDao memberPriceDao) {
        this.skuLadderDao = skuLadderDao;
        this.memberPriceDao = memberPriceDao;
    }


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuFullReductionEntity> page = this.page(
                new Query<SkuFullReductionEntity>().getPage(params),
                new QueryWrapper<>()
        );

        return new PageUtils(page);
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

}