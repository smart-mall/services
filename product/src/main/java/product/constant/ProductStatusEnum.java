package product.constant;

import lombok.Getter;

@Getter
public enum ProductStatusEnum {
    NEW(0, "新建"),
    UP(1, "上架"),
    DOWN(2, "下架");
    private final Integer code;
    private final String message;

    ProductStatusEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

}
