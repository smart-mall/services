package ware.costant;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 采购单状态，以及各状态允许的操作。
 *
 * <p>流转顺序：新建 → 已分配 → 已领取 → 已完成 / 有异常。判断方法集中在这里，
 * service 的守卫与列表接口返回的 {@code allowedActions} 共用同一份规则，前端按钮不要另写状态比较。
 */
@Getter
public enum PurchaseStatusEnum {

    /** 新建：需求单已并入本单，还没指定采购员。 */
    CREATED(0, "新建"),

    /** 已分配：已指定采购员，等待其领取。 */
    ASSIGNED(1, "已分配"),

    /** 已领取：采购员已领取，正在按单采购。 */
    RECEIVE(2, "已领取"),

    /** 已完成：采购员提交完成，且明细全部采购成功。 */
    FINISH(3, "已完成"),

    /** 有异常：采购员提交完成，但有明细被标记为采购失败。 */
    HASERROR(4, "有异常");

    private final int code;
    private final String msg;
    PurchaseStatusEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    /**
     * 判断采购单是否还没被领取，即处于新建或已分配。
     *
     * <p>合并需求单、分配采购员、取消分配都以它作为前置条件 —— 一旦领取，
     * 采购员就照着这张单在买了，再动它就会让他手上的和系统里的对不上。</p>
     *
     * @param code 采购单状态码，允许为 {@code null}
     * @return {@code true} 表示新建或已分配；{@code null} 及其他状态返回 {@code false}
     */
    public static boolean isOpen(Integer code) {
        return code != null && (code == CREATED.code || code == ASSIGNED.code);
    }

    /**
     * 判断采购单能否提交完成，只有已领取的单可以。
     *
     * @param code 采购单状态码，允许为 {@code null}
     * @return {@code true} 仅当状态为已领取；{@code null} 及其他状态返回 {@code false}
     */
    public static boolean canDone(Integer code) {
        return code != null && code == RECEIVE.code;
    }

    /**
     * 判断采购单能否被领取，必须先分配采购员。
     *
     * <p>新建的单直接领走等于跳过分配，单上就没有明确的采购员。</p>
     *
     * @param code 采购单状态码，允许为 {@code null}
     * @return {@code true} 仅当状态为已分配；{@code null} 及其他状态返回 {@code false}
     */
    public static boolean canReceive(Integer code) {
        return code != null && code == ASSIGNED.code;
    }

    /**
     * 判断采购单是否已到终态，即已完成或有异常。
     *
     * <p>终态的单不再参与流程，可以连同明细一起随仓库删除。</p>
     *
     * @param code 采购单状态码，允许为 {@code null}
     * @return {@code true} 表示已完成或有异常；{@code null} 及其他状态返回 {@code false}
     */
    public static boolean isFinal(Integer code) {
        return code != null && (code == FINISH.code || code == HASERROR.code);
    }

    /**
     * 返回所有还没被领取的状态码。
     *
     * <p>给"查所有未领取的单"这类查询条件用，避免在 SQL 条件里重抄一遍状态规则。</p>
     *
     * @return 新建与已分配的状态码列表，顺序与枚举声明一致
     */
    public static List<Integer> openCodes() {
        return Arrays.stream(values()).filter(status -> isOpen(status.code)).map(status -> status.code).toList();
    }

    /**
     * 返回该状态下允许的操作标识。
     *
     * <p>列表接口按行返回，前端据此决定按钮显不显示。</p>
     *
     * @param code 采购单状态码，允许为 {@code null}
     * @return 操作标识列表；{@code null} 或未知状态返回空列表
     */
    public static List<String> allowedActions(Integer code) {
        List<String> actions = new ArrayList<>();
        // 还没开始采购的才能分配采购员
        if (isOpen(code)) {
            actions.add("assign");
        }
        // 能删的两种：还没开始的（明细退回新建）、终态的（明细一起删）。
        // 已领取的不行 —— 那是在途，删了采购员手上的单就凭空消失了
        if (isOpen(code) || isFinal(code)) {
            actions.add("delete");
        }
        if (canReceive(code)) {
            actions.add("receive");
        }
        if (canDone(code)) {
            actions.add("done");
        }
        return actions;
    }

}
