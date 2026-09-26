package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.to.SkuReductionTo;
import common.vo.PageVO;
import coupon.entity.SkuFullReductionEntity;

import java.util.List;
import java.util.Map;

import common.query.KeyPageQuery;
/**
 * 商品满减信息
 */
public interface SkuFullReductionService extends IService<SkuFullReductionEntity> {

    PageVO<SkuFullReductionEntity> queryPage(KeyPageQuery query);

    void saveSkuReduction(SkuReductionTo skuReductionTo);

    /**
     * 按 skuId 批量删除这张表以及发布时一起写入的打折（sms_sku_ladder）、
     * 会员价（sms_member_price）。商品删除时由 product 服务远程调用。
     */
    void deleteBySkuIds(List<Long> skuIds);
}

