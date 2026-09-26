package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import product.entity.SpuInfoEntity;
import product.vo.SpuSelectVO;
import product.vo.SpuVO;

import java.util.List;
import java.util.Map;

/**
 * spu信息
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 09:58:33
 */
public interface SpuInfoService extends IService<SpuInfoEntity> {

    PageVO<SpuInfoEntity> queryPage(Map<String, Object> params);

    void saveSpuInfo(SpuVO spuInfo);

    PageVO<SpuInfoEntity> queryPageByCondition(Map<String, Object> params);

    void up(Long spuId);

    /**
     * 下架 spu：本地只改状态并落一条 {@code product.down} 事件，ES 里的文档由 search 异步清掉。
     */
    void down(Long spuId);

    SpuInfoEntity getSpuInfoBySkuId(Long skuId);

    Map<Long, String> getUserNames(List<Long> list);

    List<SpuSelectVO> getSpuSelect();

    /**
     * 级联删除 spu。
     *
     * <p>本方法<b>只在本地事务里</b>删商品自己的 7 张表（spu、spu 描述、spu 图集、规格参数、
     * sku、sku 图集、sku 销售属性），同时往本地消息表落一条 {@code product.deleted} 事件。
     * coupon 的积分/满减/打折/会员价、MinIO 里的图片由消费方异步清理 —— 远程删除是不可回滚的
     * 副作用，塞进这个事务里无论怎么排顺序都会留下不一致。</p>
     *
     * <p>已上架的商品不允许删除（11001），需要先下架。</p>
     */
    void removeSpuInfo(List<Long> spuIds);

}

