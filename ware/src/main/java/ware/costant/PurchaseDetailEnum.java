package ware.costant;

import lombok.Getter;

@Getter
public enum PurchaseDetailEnum {
    CREATED(0, "新建"),
    ASSIGNED(1, "已分配"),
    BUYING(2, "正在采购"),
    FINISH(3, "已完成"),
    HASERROR(4, "采购失败");

    private final int code;
    private final String msg;
    PurchaseDetailEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

}
