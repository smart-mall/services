package ware.vo;

import lombok.Data;

import java.util.List;

@Data
public class PurchaseDoneVO {
    private Long id;
    private List<PurchaseItemVO> items;

    @Data
    public static class PurchaseItemVO {
        private Long itemId;
        private Integer status;
        private String reason;
    }
}
