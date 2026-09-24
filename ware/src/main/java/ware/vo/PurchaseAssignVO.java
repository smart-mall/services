package ware.vo;

import lombok.Data;

/**
 * 分配采购人员。只带采购单 id 和要指派的采购员，别的字段不让前端传 ——
 * 原来"分配"是走 /ware/purchase/update 全量更新，连 status 都能改，等于绕过状态机。
 */
@Data
public class PurchaseAssignVO {

    private Long id;

    private Long assigneeId;

    private String assigneeName;

    private String phone;
}
