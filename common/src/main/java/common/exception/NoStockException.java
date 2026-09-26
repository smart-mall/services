package common.exception;

import lombok.Getter;
import lombok.Setter;

/**
 * 库存不足异常：扣减库存时可用量小于需求量时抛出。
 *
 * <p>没有对应的 {@code @ExceptionHandler}，因此不跨服务传播：ware 侧本地 try/catch 处理，
 * order 侧改用 {@code BaseException} 携带 {@link BaseCodeEnum#NO_STOCK_EXCEPTION}。
 */

public class NoStockException extends RuntimeException {

    /** 库存不足的 SKU ID；按文案构造时为 {@code null} */
    @Getter @Setter
    private Long skuId;

    /**
     * 按 SKU ID 构造，提示文案由本类拼出。
     *
     * @param skuId 库存不足的 SKU ID
     */
    public NoStockException(Long skuId) {
        super("商品id："+ skuId + "库存不足！");
    }

    /**
     * 按自定义文案构造，不记录 SKU ID。
     *
     * @param msg 提示文案
     */
    public NoStockException(String msg) {
        super(msg);
    }


}
