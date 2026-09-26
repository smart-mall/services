package ware.controller;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ware.entity.WareOrderTaskEntity;
import ware.service.WareOrderTaskService;
import common.vo.PageVO;
import common.utils.R;



import common.query.PageQuery;
/**
 * 库存工作单接口：记录一次订单的库存锁定任务（订单号、收货信息、任务状态）。
 *
 * <p>工作单是库存解锁的入口，解锁时按订单号反查它再逐条处理明细；日常不人工写，
 * 只保留查询与维护入口。
 */
@RestController
@RequestMapping("ware/wareordertask")
public class WareOrderTaskController {
    @Autowired
    private WareOrderTaskService wareOrderTaskService;

    /**
     * 分页查询库存工作单。
     *
     * @param query 分页参数
     * @return 工作单分页数据
     */
    @RequestMapping("/list")
    public R<PageVO<WareOrderTaskEntity>> list(PageQuery query){
        PageVO<WareOrderTaskEntity> page = wareOrderTaskService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 查询库存工作单详情。
     *
     * @param id 工作单 ID
     * @return 工作单；不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<WareOrderTaskEntity> info(@PathVariable("id") Long id){
		WareOrderTaskEntity wareOrderTask = wareOrderTaskService.getById(id);

        return R.ok(wareOrderTask);
    }

    /**
     * 新增库存工作单。
     *
     * @param wareOrderTask 工作单内容，{@code id} 由数据库生成
     * @return 成功响应，无数据体
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody WareOrderTaskEntity wareOrderTask){
		wareOrderTaskService.save(wareOrderTask);

        return R.ok();
    }

    /**
     * 按主键更新库存工作单，只更新入参中非 {@code null} 的字段。
     *
     * @param wareOrderTask 工作单内容，必须带 {@code id}
     * @return 成功响应，无数据体
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody WareOrderTaskEntity wareOrderTask){
		wareOrderTaskService.updateById(wareOrderTask);

        return R.ok();
    }

    /**
     * 按主键批量删除库存工作单。
     *
     * @param ids 工作单 ID 数组，不能为 {@code null}
     * @return 成功响应，无数据体
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		wareOrderTaskService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
