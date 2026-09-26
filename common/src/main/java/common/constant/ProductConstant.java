package common.constant;

/** 商品常量：属性类型与 SPU 上下架状态。 */
public class ProductConstant {

    /**
     * 属性类型，取值对应 {@code AttrEntity#attrType}。
     */
    public enum AttrEnum {
        /** 基本属性：规格参数，会关联属性分组。 */
        TYPE_BASE(1,"基本属性"),
        /** 销售属性：按 SKU 记录取值。 */
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


    /**
     * SPU 上下架状态，取值对应 {@code SpuInfoEntity#publishStatus}。
     */
    public enum ProductStatusEnum {
        /** 新建：SPU 已创建，尚未上架。 */
        NEW(0,"新建"),
        /** 已上架：SKU 已同步到 Elasticsearch。 */
        UP(1,"商品上架"),
        /** 已下架：发布下架消息，搜索侧移除对应文档。 */
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
