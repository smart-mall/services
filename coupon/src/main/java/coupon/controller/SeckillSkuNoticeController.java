package coupon.controller;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import coupon.entity.SeckillSkuNoticeEntity;
import coupon.service.SeckillSkuNoticeService;
import common.vo.PageVO;
import common.utils.R;



import common.query.PageQuery;
/**
 * 秒杀商品通知订阅的后台管理接口：分页列表、详情、新增、修改、删除。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权。
 */
@RestController
@RequestMapping("coupon/seckillskunotice")
public class SeckillSkuNoticeController {
    @Autowired
    private SeckillSkuNoticeService seckillSkuNoticeService;

    /**
     * 分页查询秒杀商品通知订阅。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为订阅记录列表
     */
    @RequestMapping("/list")
    public R<PageVO<SeckillSkuNoticeEntity>> list(PageQuery query){
        PageVO<SeckillSkuNoticeEntity> page = seckillSkuNoticeService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询单条秒杀商品通知订阅。
     *
     * @param id 订阅记录主键
     * @return 订阅详情；id 不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<SeckillSkuNoticeEntity> info(@PathVariable("id") Long id){
		SeckillSkuNoticeEntity seckillSkuNotice = seckillSkuNoticeService.getById(id);

        return R.ok(seckillSkuNotice);
    }

    /**
     * 新增一条秒杀商品通知订阅。
     *
     * @param seckillSkuNotice 订阅内容，主键留空时由数据库生成
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody SeckillSkuNoticeEntity seckillSkuNotice){
		seckillSkuNoticeService.save(seckillSkuNotice);

        return R.ok();
    }

    /**
     * 按主键修改一条秒杀商品通知订阅。
     *
     * @param seckillSkuNotice 订阅内容，主键必填；为 {@code null} 的字段不参与更新
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody SeckillSkuNoticeEntity seckillSkuNotice){
		seckillSkuNoticeService.updateById(seckillSkuNotice);

        return R.ok();
    }

    /**
     * 按主键批量删除秒杀商品通知订阅。
     *
     * @param ids 待删除的订阅记录主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		seckillSkuNoticeService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
