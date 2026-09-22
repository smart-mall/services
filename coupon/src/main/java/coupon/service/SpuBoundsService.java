package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.utils.PageUtils;
import coupon.entity.SpuBoundsEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品spu积分设置
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:10:43
 */
public interface SpuBoundsService extends IService<SpuBoundsEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 按 spuId 批量删除。商品删除时由 product 服务远程调用。
     * 注意是按 spuId 而不是行主键 id —— 调用方手里没有这张表的行 id。
     */
    void deleteBySpuIds(List<Long> spuIds);
}

