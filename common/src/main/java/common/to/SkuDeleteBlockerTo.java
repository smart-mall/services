package common.to;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 某个 sku 在仓库侧删不掉的原因。
 *
 * <p>跨服务传输：ware 的判断接口返回它，product 拿它拼给用户看的提示。
 * 所以只回答"卡在哪、有多少"，措辞留给调用方。</p>
 */
@Data
public class SkuDeleteBlockerTo implements Serializable {

    private Long skuId;

    /** 还有量的库存行：stock > 0 或 stock_locked > 0 */
    private List<StockBlocker> stockBlockers;

    /** 还没走完的采购需求条数（新建 / 已分配 / 正在采购） */
    private Integer purchaseBlockerCount;

    @Data
    public static class StockBlocker implements Serializable {
        private Long wareId;
        private String wareName;
        private Integer stock;
        private Integer stockLocked;
    }
}
