package ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import ware.entity.WareInfoEntity;
import ware.vo.FareQueryVo;
import ware.vo.FareVo;

import java.util.List;

import common.query.KeyPageQuery;
/**
 * 仓库信息服务：仓库的分页查询与删除，以及按收货地址计算运费。
 *
 * <p>删除仓库前要确认该仓没有库存占用、没有在途采购、也没有已锁定未解锁的库存工作单，
 * 通过后连同该仓的库存行、采购需求与采购单一并删除。
 */
public interface WareInfoService extends IService<WareInfoEntity> {

    /**
     * 分页查询仓库，支持按关键字过滤。
     *
     * @param query 分页与关键字条件，不能为 {@code null}；{@code key} 同时匹配仓库 ID、仓库名与仓库地址，
     *              为空时不加过滤
     * @return 仓库分页数据；无命中时 {@code rows} 为空列表，{@code total} 为 0
     */
    PageVO<WareInfoEntity> queryPage(KeyPageQuery query);

    /**
     * 按收货地区划计算一张订单的运费。
     *
     * <p>计费规则：每个商品在它所有有库存记录的仓库里选距离收货地最近的一个作为计费仓，
     * 基准运费按距离分档，同一商品多买的部分在第一件的基础上按比例加价。
     *
     * <p>实现方需保证同一组入参算出同一结果，不能依赖实时可售数量——确认页与提交订单各算一次，
     * 两次不一致会被订单侧判成价格变动而拒绝下单。因此候选仓只看有没有库存记录，不看当前库存。
     *
     * @param query 收货地区划编码与要计价的商品清单，不能为 {@code null}
     * @return 整单运费与按商品拆分的明细；{@code totalFare} 是明细之和
     * @throws common.exception.BaseException 商品没有任何库存记录时抛 {@code WARE_FARE_NO_WAREHOUSE}；
     *         候选仓都取不到距离时抛 {@code WARE_FARE_WAREHOUSE_NO_AREA}；
     *         地理服务不可用时抛 {@code WARE_FARE_DISTANCE_FAILED}
     */
    FareVo getFare(FareQueryVo query);

    /**
     * 批量删除仓库，并清理该仓的库存行、采购需求与采购单。
     *
     * <p>实现方必须保证在同一事务内逐仓校验：库存（含锁定库存）必须全为 0、关联的采购需求与采购单都在终态、
     * 且没有已锁定未解锁的库存工作单明细；任一仓不满足就整批失败，已通过校验的仓也不会被删。
     *
     * @param ids 仓库 ID 列表，不能为 {@code null}；空列表或全为 {@code null} 时按参数校验失败处理
     * @throws common.exception.ValidationException 仓库列表为空时抛出
     * @throws common.exception.BaseException 仓库不存在，或仍有库存、在途采购、已锁定未解锁的工作单时抛出
     */
    void deleteByIds(List<Long> ids);
}

