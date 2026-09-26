package ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import ware.entity.WareInfoEntity;
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
     * 按收货地址计算运费。
     *
     * <p>运费取收货地址手机号倒数第 10、9 位组成的两位数字；手机号为空或不足 10 位时按 0 处理并记一条日志。
     *
     * @param addrId 会员收货地址 ID，不能为 {@code null}
     * @return 收货地址与运费；地址查不到时返回 {@code null}
     */
    FareVo getFare(Long addrId);

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

