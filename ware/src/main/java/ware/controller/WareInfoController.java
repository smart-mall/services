package ware.controller;

import common.vo.PageVO;
import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ware.entity.WareInfoEntity;
import ware.service.WareInfoService;
import ware.vo.FareVo;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * 仓库信息
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:20:17
 */
@RestController
@Slf4j
@RequestMapping("ware/wareinfo")
public class WareInfoController {
    @Autowired
    private WareInfoService wareInfoService;

    @GetMapping(value = "/fare")
    public R<FareVo> getFare(@RequestParam("addrId") Long addrId) {
        log.info("获取运费：{}", addrId);


        FareVo fare = wareInfoService.getFare(addrId);

        return R.ok(fare);
    }


    /**
     * 列表
     */
    @RequestMapping("/list")
    public R<PageVO<WareInfoEntity>> list(@RequestParam Map<String, Object> params){
        PageVO<WareInfoEntity> page = wareInfoService.queryPage(params);

        return R.ok(page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R<WareInfoEntity> info(@PathVariable("id") Long id){
		WareInfoEntity wareInfo = wareInfoService.getById(id);

        return R.ok(wareInfo);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody WareInfoEntity wareInfo){
		wareInfoService.save(wareInfo);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody WareInfoEntity wareInfo){
		wareInfoService.updateById(wareInfo);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		wareInfoService.deleteByIds(ids == null ? List.of() : Arrays.asList(ids));

        return R.ok();
    }

}
