package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import coupon.entity.SpuBoundsEntity;

import java.util.List;
import java.util.Map;

import common.query.KeyPageQuery;
/**
 * 商品spu积分设置
 */
public interface SpuBoundsService extends IService<SpuBoundsEntity> {

    PageVO<SpuBoundsEntity> queryPage(KeyPageQuery query);

    /**
     * 按 spuId 批量删除。商品删除时由 product 服务远程调用。
     * 注意是按 spuId 而不是行主键 id —— 调用方手里没有这张表的行 id。
     */
    void deleteBySpuIds(List<Long> spuIds);
}

