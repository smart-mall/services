package ware.controller;

import java.util.Arrays;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ware.entity.UndoLogEntity;
import ware.service.UndoLogService;
import common.vo.PageVO;
import common.utils.R;



import common.query.PageQuery;
@RestController
@RequestMapping("ware/undolog")
public class UndoLogController {
    @Autowired
    private UndoLogService undoLogService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R<PageVO<UndoLogEntity>> list(PageQuery query){
        PageVO<UndoLogEntity> page = undoLogService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R<UndoLogEntity> info(@PathVariable("id") Long id){
		UndoLogEntity undoLog = undoLogService.getById(id);

        return R.ok(undoLog);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody UndoLogEntity undoLog){
		undoLogService.save(undoLog);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody UndoLogEntity undoLog){
		undoLogService.updateById(undoLog);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		undoLogService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
