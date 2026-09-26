package ware.vo;

import lombok.Data;

import java.util.List;

/**
 * 合并采购需求单的入参，对应 {@code POST /ware/purchase/merge}：把一批还没并入采购单的需求单并到同一张采购单上。
 *
 * <p>这批需求单必须同仓库，否则并进来就破坏了"一张采购单一个仓库"。
 */
@Data
public class MergeVO {

    /** 目标采购单 ID；不传就自动新建一张，传了就并进这张。 */
    private Long purchaseId;

    /** 要合并的采购需求单 ID 列表，不能为空，且每条都必须还没并入任何采购单。 */
    private List<Long> items;

    /**
     * 优先级，必须大于 0。新建采购单时作为初始值，不传按 1；
     * 并入已有单时用于覆盖该单的优先级，不传保持原样。
     */
    private Integer priority;
}
