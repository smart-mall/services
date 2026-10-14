package member.vo;

import common.query.PageQuery;
import lombok.Getter;
import lombok.Setter;

/**
 * 积分流水的查询条件，对应管理端的 {@code GET /member/integrationchangehistory/list}。
 *
 * <p>条件可不传：不传时返回全部会员的流水；传 {@code memberId} 则把台账收窄到一个会员，
 * 用来回答"这个会员的积分是怎么变成现在这个数的"。
 */
@Getter
@Setter
public class IntegrationHistoryPageQuery extends PageQuery {

    /** 按会员主键筛选；不传则返回全部会员的流水。 */
    private Long memberId;
}
