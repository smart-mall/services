package order.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import order.entity.PaymentInfoEntity;
import order.service.PaymentInfoService;
import common.vo.PageVO;
import common.utils.R;



import common.query.PageQuery;
/**
 * 支付流水（{@code oms_payment_info} 表）的后台只读台账：分页列表与详情。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权；交易流水只由支付回调写入
 * （支付宝、微信与测试支付三条链路），后台没有新增、修改与删除 —— 凭证既不该手工落库，
 * 也不该被改金额或平台交易号，否则会绕过流水的查重口径。
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

}
