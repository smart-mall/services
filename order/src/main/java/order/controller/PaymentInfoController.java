package order.controller;

import java.util.Arrays;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import order.entity.PaymentInfoEntity;
import order.service.PaymentInfoService;
import common.vo.PageVO;
import common.utils.R;



import common.query.PageQuery;
/**
 * 支付流水（{@code oms_payment_info} 表）的后台管理接口：分页列表、详情、新增、修改、删除。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权；支付宝支付回调会向本表写入交易流水。
 */
@RestController
@RequestMapping("order/paymentinfo")
public class PaymentInfoController {
    @Autowired
    private PaymentInfoService paymentInfoService;

    /**
     * 分页查询支付流水。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为支付流水列表
     */
    @RequestMapping("/list")
    public R<PageVO<PaymentInfoEntity>> list(PageQuery query){
        PageVO<PaymentInfoEntity> page = paymentInfoService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询单条支付流水。
     *
     * @param id 支付流水主键
     * @return 支付流水详情；id 不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<PaymentInfoEntity> info(@PathVariable("id") Long id){
		PaymentInfoEntity paymentInfo = paymentInfoService.getById(id);

        return R.ok(paymentInfo);
    }

    /**
     * 新增一条支付流水。
     *
     * @param paymentInfo 支付流水内容，主键由数据库生成
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody PaymentInfoEntity paymentInfo){
		paymentInfoService.save(paymentInfo);

        return R.ok();
    }

    /**
     * 按主键修改一条支付流水。
     *
     * @param paymentInfo 支付流水内容，主键必填；为 {@code null} 的字段不参与更新
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody PaymentInfoEntity paymentInfo){
		paymentInfoService.updateById(paymentInfo);

        return R.ok();
    }

    /**
     * 按主键批量删除支付流水。
     *
     * @param ids 待删除的支付流水主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		paymentInfoService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
