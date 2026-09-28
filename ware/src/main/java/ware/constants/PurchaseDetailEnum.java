package ware.constants;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 采购需求单状态，以及各状态允许的操作。
 *
 * <p>流转顺序：新建 → 已分配 → 正在采购 → 已完成 / 采购失败。新建表示还没并入采购单，
 * 并入后随采购单一起被领取、完成；判断方法集中在这里，service 守卫与列表接口的
 * {@code allowedActions} 共用同一份规则。
 */
@Getter
public enum PurchaseDetailEnum {

    /** 新建：还没并入任何采购单，可以改数量与仓库。 */
    CREATED(0, "新建"),

    /** 已分配：已并入采购单，等待采购员领取该单。 */
    ASSIGNED(1, "已分配"),

    /** 正在采购：所属采购单已被领取，采购员正在采购。 */
    BUYING(2, "正在采购"),

    /** 已完成：该条需求已采购成功并入库。 */
    FINISH(3, "已完成"),

    /** 采购失败：采购员提交完成时把这条标记为失败，不再入库。 */
    HASERROR(4, "采购失败");

    private final int code;
    private final String msg;
    PurchaseDetailEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    /**
     * 判断需求单是否还没并入采购单。
     *
     * <p>只有这个状态能改数量/仓库、能删、能被合并。库里的 {@code status} 可能为 {@code null}
     * （生成器建出来的行没写 status），按新建处理。并入之后采购员已经照着它在买了，
     * 这时改数量会出现"买 10 件、系统入库 100 件"。</p>
     *
     * @param code 需求单状态码，允许为 {@code null}
     * @return {@code true} 表示 {@code null} 或新建；其他状态返回 {@code false}
     */
    public static boolean isNew(Integer code) {
        return code == null || code == CREATED.code;
    }

    /**
     * 判断需求单能否取消分配。
     *
     * <p>只有已并入采购单但还没开始采购的能摘出来，正在采购的已由采购员在执行。</p>
     *
     * @param code 需求单状态码，允许为 {@code null}
     * @return {@code true} 仅当状态为已分配；{@code null} 及其他状态返回 {@code false}
     */
    public static boolean canUnassign(Integer code) {
        return code != null && code == ASSIGNED.code;
    }

    /**
     * 判断需求单是否已到终态，即已完成或采购失败。
     *
     * <p>提交完成时每条只接受这两个结果，也是"这条需求可以随仓库一起删掉"的前提。</p>
     *
     * @param code 需求单状态码，允许为 {@code null}
     * @return {@code true} 表示已完成或采购失败；{@code null} 及其他状态返回 {@code false}
     */
    public static boolean isFinal(Integer code) {
        return code != null && (code == FINISH.code || code == HASERROR.code);
    }

    /**
     * 判断需求单是否还没走完，即非终态。
     *
     * <p>{@code null}（生成器建的行没写 status）、新建、已分配、正在采购都算没走完。
     * 商品删除时只要还有这种需求就不能删 —— 货还在路上，商品先没了的话，
     * 到货入库会落在一个不存在的商品上。</p>
     *
     * @param code 需求单状态码，允许为 {@code null}
     * @return {@code true} 表示 {@code null} 或非终态；已完成、采购失败返回 {@code false}
     */
    public static boolean isOpen(Integer code) {
        return !isFinal(code);
    }

    /**
     * 返回该状态下允许的操作标识。
     *
     * <p>列表接口按行返回，前端据此决定按钮显不显示。</p>
     *
     * @param code 需求单状态码，允许为 {@code null}
     * @return 操作标识列表；{@code null} 或未知状态返回空列表
     */
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
