package common.constant;

/**
 * 仓库与采购常量：采购单与采购需求单的状态取值。
 */
public class WareConstant {

    /**
     * 采购单状态，取值对应 {@code PurchaseEntity#status}。
     */
    public enum PurchaseStatusEnum {
        /** 新建：采购单已生成，尚未分配采购员 */
        CREATED(0,"新建"),
        /** 已分配：已指派采购员，尚未领取 */
        ASSIGNED(1,"已分配"),
        /** 已领取：采购员已领取，单子在途，不能再改动或删除 */
        RECEIVE(2,"已领取"),
        /** 已完成：终态，不再参与流程 */
        FINISH(3,"已完成"),
        /** 有异常：终态，需要人工处理 */
        HASERROR(4,"有异常"),

        ;

        private int code;

        private String msg;

        public int getCode() {
            return code;
        }

        public String getMsg() {
            return msg;
        }

        PurchaseStatusEnum(int code, String msg) {
            this.code = code;
            this.msg = msg;
        }

    }


    /**
     * 采购需求单状态，取值对应 {@code PurchaseDetailEntity#status}。
     */
    public enum PurchaseDetailStatusEnum {
        /** 新建：未并入采购单，可改数量/仓库、可删、可合并 */
        CREATED(0,"新建"),
        /** 已分配：已并入采购单但未开始采购，可取消分配 */
        ASSIGNED(1,"已分配"),
        /** 正在采购：采购员已开始采购 */
        BUYING(2,"正在采购"),
        /** 已完成：终态，不再参与流程 */
        FINISH(3,"已完成"),
        /** 采购失败：终态，不再参与流程 */
        HASERROR(4,"采购失败"),

        ;

        private int code;

        private String msg;

        public int getCode() {
            return code;
        }

        public String getMsg() {
            return msg;
        }

        PurchaseDetailStatusEnum(int code, String msg) {
            this.code = code;
            this.msg = msg;
        }

    }


}
