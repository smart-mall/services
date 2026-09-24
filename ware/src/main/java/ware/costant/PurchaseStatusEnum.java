package ware.costant;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    /**
     * 还没被领取。合并需求单、分配采购员、取消分配、领取、删除采购单都只允许在这个阶段 ——
     * 一旦领取，采购员就照着这张单在买了，再动它就会让他手上的和系统里的对不上。
     *
     * <p>状态规则集中在这个类里：service 的守卫和列表接口返回的 {@code allowedActions}
     * 用的是同一份判断，前端按钮不要再自己写 {@code status == 0 || status == 1}。</p>
     */
    public static boolean isOpen(Integer code) {
        return code != null && (code == CREATED.code || code == ASSIGNED.code);
    }

    /** 完成采购必须先领取 */
    public static boolean canDone(Integer code) {
        return code != null && code == RECEIVE.code;
    }

    /** isOpen 的状态码集合，给"查所有还没领取的单"这种查询条件用，免得在 SQL 条件里再抄一遍规则 */
    public static List<Integer> openCodes() {
        return Arrays.stream(values()).filter(status -> isOpen(status.code)).map(status -> status.code).toList();
    }

    /** 这个状态下允许的操作，列表接口按行返回，前端据此决定按钮显不显示 */
    public static List<String> allowedActions(Integer code) {
        List<String> actions = new ArrayList<>();
        if (isOpen(code)) {
            actions.add("assign");
            actions.add("receive");
            actions.add("delete");
        }
        if (canDone(code)) {
            actions.add("done");
        }
        return actions;
    }

}
