package ware.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.query.KeyPageQuery;
import common.vo.PageVO;
import org.springframework.stereotype.Service;
import ware.dao.WareOrderTaskDao;
import ware.entity.WareInfoEntity;
import ware.entity.WareOrderTaskDetailEntity;
import ware.entity.WareOrderTaskEntity;
import ware.service.WareInfoService;
import ware.service.WareOrderTaskDetailService;
import ware.service.WareOrderTaskService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 库存工作单服务的默认实现，基于 MyBatis-Plus 的 {@code ServiceImpl} 读写 {@code wms_ware_order_task}。
 *
 * <p>工作单由锁定库存时创建，订单关闭事件到达时按订单号回查。
 *
 * <p>列表在分页结果上补明细与仓库名，两者各查一次再按 ID 分组，不逐单查询。
 */
@Service("wareOrderTaskService")
public class WareOrderTaskServiceImpl extends ServiceImpl<WareOrderTaskDao, WareOrderTaskEntity> implements WareOrderTaskService {

    private final WareOrderTaskDetailService wareOrderTaskDetailService;

    private final WareInfoService wareInfoService;

    public WareOrderTaskServiceImpl(WareOrderTaskDetailService wareOrderTaskDetailService, WareInfoService wareInfoService) {
        this.wareOrderTaskDetailService = wareOrderTaskDetailService;
        this.wareInfoService = wareInfoService;
    }

    /** {@inheritDoc} */
    @Override
    public PageVO<WareOrderTaskEntity> queryPage(KeyPageQuery query) {
        LambdaQueryWrapper<WareOrderTaskEntity> wrapper = new LambdaQueryWrapper<>();

        String key = query.getKey();
        if (key != null && !key.isBlank()) {
            // 用 and 包住两个 or：条件成为一个整体括号，后续再加过滤条件不会被 or 吞掉
            wrapper.and(inner -> inner.like(WareOrderTaskEntity::getOrderSn, key)
                    .or().like(WareOrderTaskEntity::getConsignee, key));
        }

        // 新单排在最前；同一时刻建的单按主键兜底，否则翻页可能重复或漏单
        wrapper.orderByDesc(WareOrderTaskEntity::getCreateTime).orderByDesc(WareOrderTaskEntity::getId);

        IPage<WareOrderTaskEntity> page = this.page(query.toPage(), wrapper);
        fillDetails(page.getRecords());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    /** {@inheritDoc} */
    @Override
    public WareOrderTaskEntity getOrderTaskByOrderSn(String orderSn) {

        return this.baseMapper.selectOne(
                new LambdaQueryWrapper<WareOrderTaskEntity>().eq(WareOrderTaskEntity::getOrderSn, orderSn));
    }

    /**
     * 为当前页的工作单补上明细，明细按工作单 ID 分组。
     *
     * <p>{@code task_id} 在库里允许为 NULL，分组前先滤掉这类行：分组函数的入参为 NULL 会直接抛异常，
     * 一条脏数据就能让整个列表接口失败。
     *
     * @param tasks 当前页的工作单，可以为空列表
     */
    private void fillDetails(List<WareOrderTaskEntity> tasks) {
        if (tasks.isEmpty()) {
            return;
        }

        List<Long> taskIds = tasks.stream().map(WareOrderTaskEntity::getId).toList();
        List<WareOrderTaskDetailEntity> details = wareOrderTaskDetailService.list(
                        new LambdaQueryWrapper<WareOrderTaskDetailEntity>()
                                .in(WareOrderTaskDetailEntity::getTaskId, taskIds))
                .stream()
                .filter(detail -> detail.getTaskId() != null)
                .toList();

        fillWareNames(details);

        Map<Long, List<WareOrderTaskDetailEntity>> detailsByTaskId = details.stream()
                .collect(Collectors.groupingBy(WareOrderTaskDetailEntity::getTaskId));
        tasks.forEach(task -> task.setDetailList(detailsByTaskId.getOrDefault(task.getId(), List.of())));
    }

    /**
     * 按明细上的仓库 ID 批量补仓库名。
     *
     * <p>明细只存 {@code wareId}，展示给运维的是仓库名；一张工作单可能跨仓库，所以按整页的明细一起查。
     *
     * @param details 当前页的全部明细，可以为空列表
     */
    private void fillWareNames(List<WareOrderTaskDetailEntity> details) {
        List<Long> wareIds = details.stream()
                .map(WareOrderTaskDetailEntity::getWareId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (wareIds.isEmpty()) {
            return;
        }

        // 逐个 put 而不是 Collectors.toMap：仓库名为 NULL 时 toMap 会抛空指针
        Map<Long, String> wareNames = new HashMap<>();
        for (WareInfoEntity ware : wareInfoService.listByIds(wareIds)) {
            wareNames.put(ware.getId(), ware.getName());
        }
        details.forEach(detail -> detail.setWareName(wareNames.get(detail.getWareId())));
    }

}
