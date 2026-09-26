package member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import member.entity.MemberLevelEntity;
import member.vo.MemberSelectVO;

import java.util.List;
import java.util.Map;

import common.query.KeyPageQuery;
/**
 * 会员等级，维护等级名称、成长值门槛与各项会员特权。
 *
 * <p>继承 {@link IService}，通用增删改查由 MyBatis-Plus 提供；本接口补管理端分页查询与下拉选项。
 */
public interface MemberLevelService extends IService<MemberLevelEntity> {

    /**
     * 分页查询会员等级，{@code key} 同时匹配等级名称与等级 ID。
     *
     * <p>{@code key} 为空时不加筛选条件，返回全部等级。
     *
     * @param query 分页参数与关键字，不能为 {@code null}；{@code key} 可以为 {@code null} 或空白
     * @return 分页结果，无数据时 {@code rows} 为空列表、{@code total} 为 0，不返回 {@code null}
     */
    PageVO<MemberLevelEntity> queryPage(KeyPageQuery query);

    /**
     * 查询全部会员等级的下拉选项。
     *
     * <p>不分页、不带筛选条件，返回顺序由数据库决定；等级数量少，适合一次全量返回。
     *
     * @return 等级选项列表，只含 ID 与名称；无等级时返回空列表，不返回 {@code null}
     */
    List<MemberSelectVO> getMemberSelect();
}

