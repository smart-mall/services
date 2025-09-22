package product.constant;

import lombok.Getter;

@Getter
public enum AttrEnum {
    TYPE_SALE(0, "销售属性"),
    TYPE_BASE(1, "基本属性");

    private final int code;
    private final String msg;


    AttrEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

}
