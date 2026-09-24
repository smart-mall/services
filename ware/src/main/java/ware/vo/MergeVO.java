package ware.vo;

import lombok.Data;

import java.util.List;

@Data
public class MergeVO {

    /** 不传就自动新建一张采购单，传了就并进这张 */
    private Long purchaseId;

    private List<Long> items;

    /**
     * 优先级。新建采购单时是它的初始值（不传按 1）；
     * 并入已有单时是"把这张单的优先级改成这个值"，不传就保持原样。
     */
    private Integer priority;
}
