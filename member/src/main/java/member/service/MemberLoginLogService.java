package member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.to.LoginLogTo;
import common.vo.PageVO;
import member.entity.MemberLoginLogEntity;


import common.query.PageQuery;
/**
 * 会员登录记录，负责登录成功的落库与管理端、会员端两个方向的查询。
 *
 * <p>写入方是 auth 服务：登录成功后经服务间接口把 {@link LoginLogTo} 送过来。
 */
public interface MemberLoginLogService extends IService<MemberLoginLogEntity> {

    /**
     * 分页查询全部会员的登录记录。
     *
     * <p>不带筛选条件，返回全部记录；实现方不保证行序。
     *
     * @param query 分页参数，不能为 {@code null}；{@code page} / {@code limit} 非法时取默认值并截断上限
     * @return 分页结果，无数据时 {@code rows} 为空列表、{@code total} 为 0，不返回 {@code null}
     */
    PageVO<MemberLoginLogEntity> queryPage(PageQuery query);

    /**
     * 落一条登录记录。
     *
     * <p>登录时间由实现方取当前时间填入，调用方不用传；不校验是否重复，同一会员多次调用会落多条。
     *
     * @param to 登录信息，不能为 {@code null}；{@code memberId} 必须非空，{@code ip} / {@code city} 可以为 {@code null}
     */
    void record(LoginLogTo to);

    /**
     * 分页查询某个会员自己的登录记录，按登录时间倒序。
     *
     * <p>调用方负责传自己的 {@code memberId}，实现方不校验归属。
     *
     * @param memberId 会员 ID，不能为 {@code null}
     * @param query 分页参数，不能为 {@code null}；{@code page} / {@code limit} 非法时取默认值并截断上限
     * @return 分页结果，无记录时 {@code rows} 为空列表、{@code total} 为 0，不返回 {@code null}
     */
    PageVO<MemberLoginLogEntity> queryMine(Long memberId, PageQuery query);
}
