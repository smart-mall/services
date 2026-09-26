package coupon.controller;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import coupon.entity.CouponHistoryEntity;
import coupon.service.CouponHistoryService;
import common.vo.PageVO;
import common.utils.R;



import common.query.PageQuery;
/**
 * 优惠券领取记录的后台管理接口：分页列表、详情、新增、修改、删除。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权。
 */
@RestController
@RequestMapping("coupon/couponhistory")
public class CouponHistoryController {
    @Autowired
    private CouponHistoryService couponHistoryService;

    /**
     * 分页查询优惠券领取记录。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为领取记录列表
     */
    @RequestMapping("/list")
    public R<PageVO<CouponHistoryEntity>> list(PageQuery query){
        PageVO<CouponHistoryEntity> page = couponHistoryService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询单条优惠券领取记录。
     *
     * @param id 领取记录主键
     * @return 领取记录详情；id 不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<CouponHistoryEntity> info(@PathVariable("id") Long id){
		CouponHistoryEntity couponHistory = couponHistoryService.getById(id);

        return R.ok(couponHistory);
    }

    /**
     * 新增一条优惠券领取记录。
     *
     * @param couponHistory 领取记录内容，主键留空时由数据库生成
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody CouponHistoryEntity couponHistory){
		couponHistoryService.save(couponHistory);

        return R.ok();
    }

    /**
     * 按主键修改一条优惠券领取记录。
     *
     * @param couponHistory 领取记录内容，主键必填；为 {@code null} 的字段不参与更新
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody CouponHistoryEntity couponHistory){
		couponHistoryService.updateById(couponHistory);

        return R.ok();
    }

    /**
     * 按主键批量删除优惠券领取记录。
     *
     * @param ids 待删除的领取记录主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		couponHistoryService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
