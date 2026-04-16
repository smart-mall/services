package ware.costant;

import lombok.Getter;

@Getter
public enum PurchaseStatusEnum {
    CREATED(0, "新建"),
    ASSIGNED(1, "已分配"),
    RECEIVE(2, "已领取"),
    FINISH(3, "已完成"),
    HASERROR(4, "有异常");

    private final int code;
    private final String msg;
    PurchaseStatusEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

}
