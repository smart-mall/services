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
/** 秒杀前台接口。浏览场次匿名可见；抢购挂在 {@code /jwt/} 段下，要登录。 */
@RestController
@RequestMapping("seckill/front")
public class SeckillFrontController {

    private final SeckillService seckillService;

    public SeckillFrontController(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    /** 当前秒杀场次里的商品。没有场次返回空数组；{@code randomCode} 只有进行中才有值 */
    @GetMapping("/current")
    public R<List<SeckillSkuRedisTo>> current() {
        return R.ok(seckillService.getCurrentSeckillSkus());
    }

    /** 抢购。成功返回秒杀订单号（订单由 MQ 异步创建），失败按原因返回 18xxx */
    @PostMapping("/jwt/kill")
    public R<String> kill(HttpServletRequest request, @Valid @RequestBody KillRequestVo vo) throws InterruptedException {
        String orderSn = seckillService.kill(LoginUserUtils.requireCurrentUser(request),
                vo.getKillId(), vo.getKey(), vo.getNum());
        return R.ok(orderSn);
    }
}
