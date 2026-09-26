package cart;

import cart.service.CartService;
import cart.vo.CartItemVo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.ExecutionException;

/** 购物车服务的手工调试入口：直接调加购并把结果打到日志。 */
@Slf4j
@SpringBootTest
public class CartApplicationTests {

    @Autowired
    private CartService cartService;

    /** 对会员 1 的 SKU 2 执行加购，结果只打日志、不做断言。 */
    @Test
    public void contextLoads() throws ExecutionException, InterruptedException {

        CartItemVo cartItemVo = cartService.addToCart(1L, 2);

        log.info("cartItemVo:{}",cartItemVo);

    }

}
