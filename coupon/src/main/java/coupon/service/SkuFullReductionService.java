package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.to.SkuReductionTo;
import common.utils.PageUtils;
import coupon.entity.SkuFullReductionEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品满减信息
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:10:43
 */
public interface SkuFullReductionService extends IService<SkuFullReductionEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveSkuReduction(SkuReductionTo skuReductionTo);

    /**
     * 按 skuId 批量删除这张表以及发布时一起写入的打折（sms_sku_ladder）、
     * 会员价（sms_member_price）。商品删除时由 product 服务远程调用。
     */
    void deleteBySkuIds(List<Long> skuIds);
}

