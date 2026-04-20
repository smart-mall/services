package common.constant;

/**
 * @Description: 商品常量属性
 * @Created: with IntelliJ IDEA.
 * @author: 夏沫止水
 * @createTime: 2020-05-29 16:23
 **/
public class ProductConstant {

    public enum AttrEnum {
        TYPE_BASE(1,"基本属性"),
        TYPE_SALE(0,"销售属性");

        private int code;

        private String msg;

        public int getCode() {
            return code;
        }

        public String getMsg() {
            return msg;
        }

        AttrEnum(int code, String msg) {
            this.code = code;
            this.msg = msg;
        }

    }


    public enum ProductStatusEnum {
        NEW(0,"新建"),
        UP(1,"商品上架"),
        DOWN(2,"商品下架"),
        ;

        private int code;

        private String msg;

        public int getCode() {
            return code;
        }

        public String getMsg() {
            return msg;
        }

        ProductStatusEnum(int code, String msg) {
            this.code = code;
            this.msg = msg;
        }

    }


}
