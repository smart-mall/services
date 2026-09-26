package ware.dao;

import org.apache.ibatis.annotations.Param;
import ware.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * {@code wms_ware_sku} 表的 MyBatis-Plus Mapper，映射 SKU 在各仓库的库存 {@link WareSkuEntity}。
 *
 * <p>入库、锁定与解锁都声明了自定义 SQL：增量在数据库侧完成，先查后算会丢失并发更新。
 */
@Mapper
public interface WareSkuDao extends BaseMapper<WareSkuEntity> {

    /**
     * 汇总某个 SKU 在所有仓库的可用库存。
     *
     * <p>逐行按 {@code stock - stock_locked} 计算后求和，已锁定的部分不计入。
     *
     * @param skuId SKU ID，不能为 {@code null}
     * @return 可用库存总数；该 SKU 一条库存行都没有时返回 {@code null}
     */
    Long getSkuStock(Long skuId);

    /**
     * 增加指定仓库中某个 SKU 的库存。
     *
     * <p>增量在 SQL 里直接对 {@code stock} 做加法；该仓库还没有这个 SKU 的库存行时不会新增行，
     * 调用方需先保证行存在。
     *
     * @param skuId SKU ID，不能为 {@code null}
     * @param wareId 仓库 ID，不能为 {@code null}
     * @param skuNum 入库数量，必须大于 0
     */
    void addStock(@Param("skuId") Long skuId, @Param("wareId") Long wareId, @Param("skuNum") Integer skuNum);

    /**
     * 锁定指定仓库中某个 SKU 的库存。
     *
     * <p>判定条件只有可用量大于 0，不校验可用量够不够锁定的数量；返回 1 表示锁定成功，
     * 返回 0 表示该仓库已无可用量或没有库存行。
     *
     * @param skuId SKU ID，不能为 {@code null}
     * @param wareId 仓库 ID，不能为 {@code null}
     * @param num 锁定数量，必须大于 0
     * @return 受影响行数，1 表示锁定成功，0 表示没锁上
     */
    Long lockSkuStock(Long skuId, Long wareId, Integer num);

    /**
     * 释放指定仓库中某个 SKU 已锁定的库存。
     *
     * <p>直接扣减 {@code stock_locked}，不校验余额也不保证幂等：重复调用会重复扣减，
     * 幂等由调用方按工作单明细的锁定状态保证。
     *
     * @param skuId SKU ID，不能为 {@code null}
     * @param wareId 仓库 ID，不能为 {@code null}
     * @param num 释放数量，必须大于 0
     */
    void unLockStock(Long skuId, Long wareId, Integer num);

    /**
     * 查询还有可用库存的仓库 ID。
     *
     * @param skuId SKU ID，不能为 {@code null}
     * @return 可用量大于 0 的仓库 ID 列表；一个都没有时返回空列表
     */
    List<Long> listWareIdHasSkuStock(Long skuId);

}
