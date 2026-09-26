package product.controller;

import common.vo.PageVO;
import common.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import product.entity.UndoLogEntity;
import product.service.UndoLogService;

import java.util.Arrays;


import common.query.PageQuery;
/**
 * undo_log 回滚日志的后台管理接口：分页列表、详情、新增、修改、删除。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权。
 */
@RestController
@RequestMapping("product/undolog")
public class UndoLogController {
    @Autowired
    private UndoLogService undoLogService;

    /**
     * 分页查询 undo_log 回滚日志。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为回滚日志列表
     */
    @RequestMapping("/list")
    public R<PageVO<UndoLogEntity>> list(PageQuery query){
        PageVO<UndoLogEntity> page = undoLogService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询回滚日志详情。
     *
     * @param id 回滚日志主键
     * @return 回滚日志详情；不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<UndoLogEntity> info(@PathVariable("id") Long id){
		UndoLogEntity undoLog = undoLogService.getById(id);

        return R.ok(undoLog);
    }

    /**
     * 新增一条回滚日志。
     *
     * @param undoLog 回滚日志内容，含分支事务 ID、全局事务 XID 与回滚镜像
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody UndoLogEntity undoLog){
		undoLogService.save(undoLog);

        return R.ok();
    }

    /**
     * 按主键修改回滚日志。
     *
     * @param undoLog 回滚日志内容，主键必填
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody UndoLogEntity undoLog){
		undoLogService.updateById(undoLog);

        return R.ok();
    }

    /**
     * 按主键批量删除回滚日志。
     *
     * @param ids 待删除的回滚日志主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		undoLogService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
