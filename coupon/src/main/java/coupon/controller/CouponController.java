package coupon.controller;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import coupon.entity.CouponEntity;
import coupon.service.CouponService;
import common.vo.PageVO;
import common.utils.R;



import common.query.KeyPageQuery;
/**
 * 优惠券的后台管理接口：分页列表、详情、新增、修改、删除。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权。
 */
@RestController
@RequestMapping("coupon/coupon")
public class CouponController {
    @Autowired
    private CouponService couponService;

    /**
     * 分页查询优惠券。
     *
     * @param query 分页参数，{@code key} 模糊匹配券名或券 ID，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为优惠券列表
     */
    @RequestMapping("/list")
    public R<PageVO<CouponEntity>> list(KeyPageQuery query){
        PageVO<CouponEntity> page = couponService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询单条优惠券。
     *
     * @param id 优惠券主键
     * @return 优惠券详情；id 不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<CouponEntity> info(@PathVariable("id") Long id){
		CouponEntity coupon = couponService.getById(id);

        return R.ok(coupon);
    }

    /**
     * 新增一条优惠券。
     *
     * @param coupon 优惠券内容，主键留空时由数据库生成
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody CouponEntity coupon){
		couponService.save(coupon);

        return R.ok();
    }

    /**
     * 按主键修改一条优惠券。
     *
     * @param coupon 优惠券内容，主键必填；为 {@code null} 的字段不参与更新
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody CouponEntity coupon){
		couponService.updateById(coupon);

        return R.ok();
    }

    /**
     * 按主键批量删除优惠券。
     *
     * @param ids 待删除的优惠券主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		couponService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
