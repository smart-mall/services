package cart;

import cart.service.CartService;
import cart.vo.CartItemVo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.ExecutionException;

@Slf4j
@SpringBootTest
public class CartApplicationTests {

    @Autowired
    private CartService cartService;

    @Test
    public void contextLoads() throws ExecutionException, InterruptedException {

        CartItemVo cartItemVo = cartService.addToCart(1L, 2);

        log.info("cartItemVo:{}",cartItemVo);

    }

}
