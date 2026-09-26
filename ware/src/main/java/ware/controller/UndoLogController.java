package ware.controller;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ware.entity.UndoLogEntity;
import ware.service.UndoLogService;
import common.vo.PageVO;
import common.utils.R;



import common.query.PageQuery;
/**
 * 回滚日志接口：对 {@code undo_log} 表做分页查询与增删改。
 *
 * <p>表里存的是分支事务的 {@code xid}、回滚信息与日志状态。本服务内没有业务代码写这张表，
 * 这里只是生成器产出的维护入口，改动前需确认没有事务组件在依赖它。
 */
@RestController
@RequestMapping("ware/undolog")
public class UndoLogController {
    @Autowired
    private UndoLogService undoLogService;

    /**
     * 分页查询回滚日志。
     *
     * @param query 分页参数
     * @return 回滚日志分页数据
     */
    @RequestMapping("/list")
    public R<PageVO<UndoLogEntity>> list(PageQuery query){
        PageVO<UndoLogEntity> page = undoLogService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 查询单条回滚日志。
     *
     * @param id 日志主键 ID
     * @return 回滚日志；不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<UndoLogEntity> info(@PathVariable("id") Long id){
		UndoLogEntity undoLog = undoLogService.getById(id);

        return R.ok(undoLog);
    }

    /**
     * 新增一条回滚日志。
     *
     * @param undoLog 日志内容，{@code id} 由数据库生成
     * @return 成功响应，无数据体
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody UndoLogEntity undoLog){
		undoLogService.save(undoLog);

        return R.ok();
    }

    /**
     * 按主键更新回滚日志，只更新入参中非 {@code null} 的字段。
     *
     * @param undoLog 日志内容，必须带 {@code id}
     * @return 成功响应，无数据体
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody UndoLogEntity undoLog){
		undoLogService.updateById(undoLog);

        return R.ok();
    }

    /**
     * 按主键批量删除回滚日志。
     *
     * @param ids 日志主键 ID 数组，不能为 {@code null}
     * @return 成功响应，无数据体
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		undoLogService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
