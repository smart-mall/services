package ware.costant;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * 还没并入采购单：只有这个状态能改数量/仓库、能删、能被合并。
     *
     * <p>库里可能是 null（生成器建出来的行没写 status），按"新建"算。
     * 并入采购单之后采购员已经照着它在买了，这时候改数量会出现"买 10 件、系统入库 100 件"。</p>
     */
    public static boolean isNew(Integer code) {
        return code == null || code == CREATED.code;
    }

    /** 已并入采购单但还没开始采购：只有这个状态能取消分配 */
    public static boolean canUnassign(Integer code) {
        return code != null && code == ASSIGNED.code;
    }

    /** 完成采购时每一条只接受这两个结果 */
    public static boolean isFinalResult(Integer code) {
        return code != null && (code == FINISH.code || code == HASERROR.code);
    }

    /** 这个状态下允许的操作，列表接口按行返回，前端据此决定按钮显不显示 */
    public static List<String> allowedActions(Integer code) {
        List<String> actions = new ArrayList<>();
        if (isNew(code)) {
            actions.add("edit");
            actions.add("delete");
            actions.add("merge");
        }
        if (canUnassign(code)) {
            actions.add("unassign");
        }
        return actions;
    }

}
