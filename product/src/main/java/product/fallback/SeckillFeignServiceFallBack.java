package product.fallback;


import common.exception.BaseCodeEnum;
import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import product.feign.SeckillFeignService;


@Slf4j
@Component
public class SeckillFeignServiceFallBack implements SeckillFeignService {
    @Override
    public R getSkuSeckilInfo(Long skuId) {
        log.info("熔断方法调用：getSkuSeckilInfo");
        return R.error(BaseCodeEnum.TO_MANY_REQUEST.getCode(),BaseCodeEnum.TO_MANY_REQUEST.getMsg());
    }
}
