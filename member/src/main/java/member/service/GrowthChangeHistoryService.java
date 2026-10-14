package member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import member.entity.GrowthChangeHistoryEntity;
import member.vo.GrowthHistoryAdminVo;
import member.vo.GrowthHistoryPageQuery;
import member.vo.GrowthHistoryVo;


import common.query.PageQuery;
/**
 * 会员成长值变动流水，记录每次成长值增减的数值与来源。
 *
 * <p>继承 {@link IService}，通用增删改查由 MyBatis-Plus 提供；本接口只补两种分页查询：
 * 管理端可带会员条件下沉到某个人的台账，以及会员端按登录态取本人的分页。
 * 流水行的写入由 {@code GrowthAccountService} 负责。
 */
public interface GrowthChangeHistoryService extends IService<GrowthChangeHistoryEntity> {

    /**
     * 分页查询成长值变动流水台账，按变动时间倒序，每行补上会员昵称与变动类型文案。
     *
     * <p>{@code query} 的 {@code memberId} 不传时返回全部会员的流水；传入则只返回该会员的流水。
     *
     * @param query 分页与筛选条件，不能为 {@code null}；{@code page} / {@code limit} 非法时取默认值并截断上限
     * @return 分页结果，无数据时 {@code rows} 为空列表、{@code total} 为 0，不返回 {@code null}
     */
    PageVO<GrowthHistoryAdminVo> queryPage(GrowthHistoryPageQuery query);

    /**
     * 分页查询指定会员自己的成长值变动流水，按变动时间倒序。
     *
     * <p>会员维度由调用方从登录态取得，不接受前端传参；实现方必须按 {@code memberId} 过滤，
     * 否则改一个参数就能翻到别人的流水。
     *
     * @param memberId 会员 id，不能为 {@code null}
     * @param query 分页参数，不能为 {@code null}；{@code page} / {@code limit} 非法时取默认值并截断上限
     * @return 分页结果，无数据时 {@code rows} 为空列表、{@code total} 为 0，不返回 {@code null}
     */
    PageVO<GrowthHistoryVo> queryMine(Long memberId, PageQuery query);
}
