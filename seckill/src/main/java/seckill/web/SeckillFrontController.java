package seckill.web;

import common.utils.LoginUserUtils;
import common.utils.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import seckill.service.SeckillService;
import seckill.vo.KillRequestVo;

import java.util.List;
import seckill.to.SeckillSkuRedisTo;
/**
 * 秒杀前台接口。浏览场次匿名可见；抢购挂在 {@code /jwt/} 段下，要登录。
 */
@RestController
@RequestMapping("seckill/front")
public class SeckillFrontController {

    private final SeckillService seckillService;

    public SeckillFrontController(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    /**
     * 返回当前秒杀场次里的商品。
     *
     * @return 统一响应体，数据为当前场次的商品列表；没有场次时是空数组，
     *         {@code randomCode} 只有场次进行中才有值
     */
    @GetMapping("/current")
    public R<List<SeckillSkuRedisTo>> current() {
        return R.ok(seckillService.getCurrentSeckillSkus());
    }

    /**
     * 抢购指定场次的商品。
     *
     * <p>成功返回的订单号对应一张还没落库的订单，由 order 服务消费 MQ 后创建，前端需轮询订单列表等它出现。
     *
     * @param request 当前 HTTP 请求，用于取网关注入的登录会员身份
     * @param vo      抢购请求体，三个字段都由前端原样回传
     * @return 统一响应体，数据为秒杀订单号；抢不到时按原因返回 18xxx 错误码
     * @throws InterruptedException 等待库存信号量时线程被中断
     */
    @PostMapping("/jwt/kill")
    public R<String> kill(HttpServletRequest request, @Valid @RequestBody KillRequestVo vo) throws InterruptedException {
        String orderSn = seckillService.kill(LoginUserUtils.requireCurrentUser(request),
                vo.getKillId(), vo.getKey(), vo.getNum());
        return R.ok(orderSn);
    }
}
