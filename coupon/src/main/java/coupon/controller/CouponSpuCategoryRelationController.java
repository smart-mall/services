package coupon.controller;

import java.util.Arrays;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import coupon.entity.CouponSpuCategoryRelationEntity;
import coupon.service.CouponSpuCategoryRelationService;
import common.vo.PageVO;
import common.utils.R;



import common.query.PageQuery;
/**
 * 优惠券与商品分类关联关系的后台管理接口：分页列表、详情、新增、修改、删除。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权。
 */
@RestController
@RequestMapping("coupon/couponspucategoryrelation")
public class CouponSpuCategoryRelationController {
    @Autowired
    private CouponSpuCategoryRelationService couponSpuCategoryRelationService;

    /**
     * 分页查询优惠券与商品分类的关联关系。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为关联关系列表
     */
    @RequestMapping("/list")
    public R<PageVO<CouponSpuCategoryRelationEntity>> list(PageQuery query){
        PageVO<CouponSpuCategoryRelationEntity> page = couponSpuCategoryRelationService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询单条优惠券与商品分类的关联关系。
     *
     * @param id 关联关系主键
     * @return 关联关系详情；id 不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<CouponSpuCategoryRelationEntity> info(@PathVariable("id") Long id){
		CouponSpuCategoryRelationEntity couponSpuCategoryRelation = couponSpuCategoryRelationService.getById(id);

        return R.ok(couponSpuCategoryRelation);
    }

    /**
     * 新增一条优惠券与商品分类的关联关系。
     *
     * @param couponSpuCategoryRelation 关联关系内容，主键留空时由数据库生成
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody CouponSpuCategoryRelationEntity couponSpuCategoryRelation){
		couponSpuCategoryRelationService.save(couponSpuCategoryRelation);

        return R.ok();
    }

    /**
     * 按主键修改一条优惠券与商品分类的关联关系。
     *
     * @param couponSpuCategoryRelation 关联关系内容，主键必填；为 {@code null} 的字段不参与更新
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody CouponSpuCategoryRelationEntity couponSpuCategoryRelation){
		couponSpuCategoryRelationService.updateById(couponSpuCategoryRelation);

        return R.ok();
    }

    /**
     * 按主键批量删除优惠券与商品分类的关联关系。
     *
     * @param ids 待删除的关联关系主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		couponSpuCategoryRelationService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
