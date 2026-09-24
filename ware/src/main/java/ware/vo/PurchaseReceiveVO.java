package ware.vo;

import lombok.Data;

import java.util.List;

/**
 * 领取采购单。
 *
 * <p>带上领取人是因为"分配给谁就该谁领"：采购单上已经记了 assigneeId，
 * 别人（包括分配的人自己）不能替他把单领走。</p>
 */
@Data
public class PurchaseReceiveVO {

    private List<Long> ids;

    /** 领取人 = 当前登录用户，必须和采购单上的采购员一致 */
    private Long assigneeId;
}
