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


import common.query.KeyPageQuery;
/**
 * 仓库信息接口：仓库的增删改查，以及按收货地址计算运费。
 */
@RestController
@Slf4j
@RequestMapping("ware/wareinfo")
public class WareInfoController {
    @Autowired
    private WareInfoService wareInfoService;

    /**
     * 计算指定收货地址的运费。
     *
     * <p>运费按收货地址手机号推算，手机号缺失或长度不足 10 位时按 0 处理；
     * 地址查不到时返回的 {@code data} 为 {@code null}。</p>
     *
     * @param addrId 会员收货地址 ID
     * @return 收货地址与运费；地址不存在时返回 {@code null}
     */
    @GetMapping(value = "/fare")
    public R<FareVo> getFare(@RequestParam("addrId") Long addrId) {
        log.info("获取运费：{}", addrId);


        FareVo fare = wareInfoService.getFare(addrId);

        return R.ok(fare);
    }


    /**
     * 分页查询仓库，支持按关键字过滤。
     *
     * @param query 分页与关键字条件
     * @return 仓库分页数据
     */
    @RequestMapping("/list")
    public R<PageVO<WareInfoEntity>> list(KeyPageQuery query){
        PageVO<WareInfoEntity> page = wareInfoService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 查询仓库详情。
     *
     * @param id 仓库 ID
     * @return 仓库；不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<WareInfoEntity> info(@PathVariable("id") Long id){
		WareInfoEntity wareInfo = wareInfoService.getById(id);

        return R.ok(wareInfo);
    }

    /**
     * 新建仓库。
     *
     * @param wareInfo 仓库内容，{@code id} 由数据库生成
     * @return 成功响应，无数据体
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody WareInfoEntity wareInfo){
		wareInfoService.save(wareInfo);

        return R.ok();
    }

    /**
     * 按主键更新仓库，只更新入参中非 {@code null} 的字段。
     *
     * @param wareInfo 仓库内容，必须带 {@code id}
     * @return 成功响应，无数据体
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody WareInfoEntity wareInfo){
		wareInfoService.updateById(wareInfo);

        return R.ok();
    }

    /**
     * 批量删除仓库。
     *
     * <p>删除前逐仓校验：还有没走完的采购需求/采购单、或有已锁定未解锁的库存时拒绝删除；
     * 校验通过后连同该仓的库存行、采购需求与采购单一并删除。</p>
     *
     * @param ids 仓库 ID 数组；为空时服务端按参数校验失败处理
     * @return 成功响应，无数据体
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		wareInfoService.deleteByIds(ids == null ? List.of() : Arrays.asList(ids));

        return R.ok();
    }

}
