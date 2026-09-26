package ware.controller;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ware.entity.WareOrderTaskDetailEntity;
import ware.service.WareOrderTaskDetailService;
import common.vo.PageVO;
import common.utils.R;



import common.query.PageQuery;
/**
 * 库存工作单明细接口：记录工作单下每个 SKU 在哪个仓锁定了多少件，以及锁定状态。
 *
 * <p>{@code lockStatus} 取 1 已锁定、2 已解锁、3 扣减；库存解锁只处理 1 的明细，
 * 因此重复投递解锁消息不会重复减库存。
 */
@RestController
@RequestMapping("ware/wareordertaskdetail")
public class WareOrderTaskDetailController {
    @Autowired
    private WareOrderTaskDetailService wareOrderTaskDetailService;

    /**
     * 分页查询库存工作单明细。
     *
     * @param query 分页参数
     * @return 工作单明细分页数据
     */
    @RequestMapping("/list")
    public R<PageVO<WareOrderTaskDetailEntity>> list(PageQuery query){
        PageVO<WareOrderTaskDetailEntity> page = wareOrderTaskDetailService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 查询库存工作单明细详情。
     *
     * @param id 明细 ID
     * @return 明细；不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<WareOrderTaskDetailEntity> info(@PathVariable("id") Long id){
		WareOrderTaskDetailEntity wareOrderTaskDetail = wareOrderTaskDetailService.getById(id);

        return R.ok(wareOrderTaskDetail);
    }

    /**
     * 新增库存工作单明细。
     *
     * @param wareOrderTaskDetail 明细内容，{@code taskId}、{@code skuId}、{@code wareId} 必填
     * @return 成功响应，无数据体
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody WareOrderTaskDetailEntity wareOrderTaskDetail){
		wareOrderTaskDetailService.save(wareOrderTaskDetail);

        return R.ok();
    }

    /**
     * 按主键更新库存工作单明细，只更新入参中非 {@code null} 的字段。
     *
     * @param wareOrderTaskDetail 明细内容，必须带 {@code id}
     * @return 成功响应，无数据体
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody WareOrderTaskDetailEntity wareOrderTaskDetail){
		wareOrderTaskDetailService.updateById(wareOrderTaskDetail);

        return R.ok();
    }

    /**
     * 按主键批量删除库存工作单明细。
     *
     * @param ids 明细 ID 数组，不能为 {@code null}
     * @return 成功响应，无数据体
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		wareOrderTaskDetailService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
