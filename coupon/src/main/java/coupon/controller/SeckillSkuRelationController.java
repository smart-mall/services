package coupon.controller;

import java.util.Arrays;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import coupon.entity.SeckillSkuRelationEntity;
import coupon.service.SeckillSkuRelationService;
import common.vo.PageVO;
import common.utils.R;



import coupon.vo.SeckillSkuRelationPageQuery;
/**
 * 秒杀活动商品关联的后台管理接口：分页列表、详情、新增、修改、删除。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权。
 */
@RestController
@RequestMapping("coupon/seckillskurelation")
public class SeckillSkuRelationController {
    @Autowired
    private SeckillSkuRelationService seckillSkuRelationService;

    /**
     * 分页查询秒杀活动商品关联。
     *
     * @param query 分页参数，{@code promotionSessionId} 为场次 ID 的精确筛选条件，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为该场次下的活动商品关联列表
     */
    @RequestMapping("/list")
    public R<PageVO<SeckillSkuRelationEntity>> list(SeckillSkuRelationPageQuery query){
        PageVO<SeckillSkuRelationEntity> page = seckillSkuRelationService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询单条秒杀活动商品关联。
     *
     * @param id 关联关系主键
     * @return 关联关系详情；id 不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<SeckillSkuRelationEntity> info(@PathVariable("id") Long id){
		SeckillSkuRelationEntity seckillSkuRelation = seckillSkuRelationService.getById(id);

        return R.ok(seckillSkuRelation);
    }

    /**
     * 新增一条秒杀活动商品关联。
     *
     * @param seckillSkuRelation 关联关系内容，主键留空时由数据库生成
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody SeckillSkuRelationEntity seckillSkuRelation){
		seckillSkuRelationService.save(seckillSkuRelation);

        return R.ok();
    }

    /**
     * 按主键修改一条秒杀活动商品关联。
     *
     * @param seckillSkuRelation 关联关系内容，主键必填；为 {@code null} 的字段不参与更新
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody SeckillSkuRelationEntity seckillSkuRelation){
		seckillSkuRelationService.updateById(seckillSkuRelation);

        return R.ok();
    }

    /**
     * 按主键批量删除秒杀活动商品关联。
     *
     * @param ids 待删除的关联关系主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		seckillSkuRelationService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
