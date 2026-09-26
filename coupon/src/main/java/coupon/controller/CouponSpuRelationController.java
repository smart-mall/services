package coupon.controller;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import coupon.entity.CouponSpuRelationEntity;
import coupon.service.CouponSpuRelationService;
import common.vo.PageVO;
import common.utils.R;



import common.query.PageQuery;
/**
 * 优惠券与 SPU 关联关系的后台管理接口：分页列表、详情、新增、修改、删除。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权。
 */
@RestController
@RequestMapping("coupon/couponspurelation")
public class CouponSpuRelationController {
    @Autowired
    private CouponSpuRelationService couponSpuRelationService;

    /**
     * 分页查询优惠券与 SPU 的关联关系。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为关联关系列表
     */
    @RequestMapping("/list")
    public R<PageVO<CouponSpuRelationEntity>> list(PageQuery query){
        PageVO<CouponSpuRelationEntity> page = couponSpuRelationService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询单条优惠券与 SPU 的关联关系。
     *
     * @param id 关联关系主键
     * @return 关联关系详情；id 不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<CouponSpuRelationEntity> info(@PathVariable("id") Long id){
		CouponSpuRelationEntity couponSpuRelation = couponSpuRelationService.getById(id);

        return R.ok(couponSpuRelation);
    }

    /**
     * 新增一条优惠券与 SPU 的关联关系。
     *
     * @param couponSpuRelation 关联关系内容，主键留空时由数据库生成
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody CouponSpuRelationEntity couponSpuRelation){
		couponSpuRelationService.save(couponSpuRelation);

        return R.ok();
    }

    /**
     * 按主键修改一条优惠券与 SPU 的关联关系。
     *
     * @param couponSpuRelation 关联关系内容，主键必填；为 {@code null} 的字段不参与更新
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody CouponSpuRelationEntity couponSpuRelation){
		couponSpuRelationService.updateById(couponSpuRelation);

        return R.ok();
    }

    /**
     * 按主键批量删除优惠券与 SPU 的关联关系。
     *
     * @param ids 待删除的关联关系主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		couponSpuRelationService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
