package coupon.controller;

import common.vo.PageVO;
import common.utils.R;
import coupon.entity.CouponEntity;
import coupon.service.CouponService;
import coupon.vo.CouponGrantVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

import common.query.KeyPageQuery;

/**
 * 优惠券的后台管理接口：分页列表、详情、新增、修改、删除、发布、停发与定向发券。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权。本类只做参数接收与转发，
 * 校验与业务规则都在 service 层。
 */
@RestController
@RequestMapping("coupon/coupon")
public class CouponController {
    @Autowired
    private CouponService couponService;

    /**
     * 分页查询优惠券。
     *
     * <p>每行都带现算的 {@code statusText} 与 {@code allowedActions}，列表页据此渲染状态与按钮。
     *
     * @param query 分页参数，{@code key} 模糊匹配券名或券 ID，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为优惠券列表
     */
    @GetMapping("/list")
    public R<PageVO<CouponEntity>> list(KeyPageQuery query){
        PageVO<CouponEntity> page = couponService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询单条优惠券，并带出适用范围关联明细供编辑时回显。
     *
     * @param id 优惠券主键
     * @return 优惠券详情；id 不存在时 {@code data} 为 {@code null}
     */
    @GetMapping("/info/{id}")
    public R<CouponEntity> info(@PathVariable("id") Long id){
		CouponEntity coupon = couponService.getDetail(id);

        return R.ok(coupon);
    }

    /**
     * 新增一条优惠券，同时写入适用范围关联。
     *
     * <p>新建的券一律是未发布的草稿，发布要另调 {@code /{id}/publish}。
     *
     * @param coupon 优惠券内容，主键留空时由数据库生成；字段约束由 {@code @Valid} 校验
     * @return 统一成功响应，不含业务数据
     */
    @PostMapping("/save")
    public R<Void> save(@Valid @RequestBody CouponEntity coupon){
		couponService.saveCoupon(coupon);

        return R.ok();
    }

    /**
     * 按主键修改一条优惠券，适用范围关联整体替换。
     *
     * <p>已有会员领取时，面额、门槛、适用范围与两个时间区间不可再改，请求会被拒绝。
     *
     * @param coupon 优惠券内容，主键必填
     * @return 统一成功响应，不含业务数据
     */
    @PostMapping("/update")
    public R<Void> update(@Valid @RequestBody CouponEntity coupon){
		couponService.updateCoupon(coupon);

        return R.ok();
    }

    /**
     * 按主键批量删除优惠券，同时清掉适用范围关联。
     *
     * <p>任一张券已有领取记录时整批拒绝，只能改为停发。
     *
     * @param ids 待删除的优惠券主键数组
     * @return 统一成功响应，不含业务数据
     */
    @PostMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		couponService.deleteCoupons(Arrays.asList(ids));

        return R.ok();
    }

    /**
     * 发布优惠券，使其可被会员领取。
     *
     * @param id 优惠券主键
     * @return 统一成功响应，不含业务数据
     */
    @PostMapping("/{id}/publish")
    public R<Void> publish(@PathVariable("id") Long id){
        couponService.publish(id);

        return R.ok();
    }

    /**
     * 停发优惠券，关闭领取入口；已经领到手的券不受影响。
     *
     * @param id 优惠券主键
     * @return 统一成功响应，不含业务数据
     */
    @PostMapping("/{id}/revoke")
    public R<Void> revoke(@PathVariable("id") Long id){
        couponService.revoke(id);

        return R.ok();
    }

    /**
     * 后台定向发券给选中的会员。
     *
     * <p>与会员主动领取共用同一套闸门，逐个会员独立判定：不满足条件的会员被跳过，
     * 已发出的部分不回滚。
     *
     * @param id 优惠券主键
     * @param vo 发券入参，{@code memberIds} 不能为空
     * @return 实际发出成功的张数
     */
    @PostMapping("/{id}/grant")
    public R<Integer> grant(@PathVariable("id") Long id, @Valid @RequestBody CouponGrantVO vo){
        List<Long> memberIds = vo.getMemberIds();

        return R.ok(couponService.grant(id, memberIds));
    }

}
