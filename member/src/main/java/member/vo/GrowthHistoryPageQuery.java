package member.vo;

import common.query.PageQuery;
import lombok.Getter;
import lombok.Setter;

/**
 * 成长值流水的查询条件，对应管理端的 {@code GET /member/growthchangehistory/list}。
 *
 * <p>条件可不传：不传时返回全部会员的流水；传 {@code memberId} 则把台账收窄到一个会员，
 * 与积分流水页保持同一套筛选口径。
 */
@Getter
@Setter
public class GrowthHistoryPageQuery extends PageQuery {

    /** 按会员主键筛选；不传则返回全部会员的流水。 */
    private Long memberId;
}
