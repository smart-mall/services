package member.dao;

import member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * {@code ums_member} 表的 MyBatis-Plus Mapper，映射会员账号 {@link MemberEntity}。
 *
 * <p>除通用增删改查外只声明积分的两条原子增减语句：判断与写入必须落在同一条 SQL 里，
 * 先查余额再写会出现"两个并发请求各自读到够用的余额、双双扣成功"的窗口。
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {

    /**
     * 扣减积分余额，余额不足时不扣。
     *
     * <p>把"够不够"写进 {@code WHERE}：受影响 0 行即余额不足，调用方据此报错，
     * 因此不需要在应用层先读一次余额。{@code integration} 列允许为 {@code null}，
     * 统一按 0 参与比较与运算。
     *
     * @param memberId 会员 id，不能为 {@code null}
     * @param points 扣减数量，必须为正数
     * @return 受影响行数：1 表示扣减成功，0 表示会员不存在或余额不足
     */
    @Update("UPDATE ums_member SET integration = COALESCE(integration, 0) - #{points} "
            + "WHERE id = #{memberId} AND COALESCE(integration, 0) >= #{points}")
    int deductIntegration(@Param("memberId") Long memberId, @Param("points") int points);

    /**
     * 增加积分余额。
     *
     * <p>只按主键定位，不带余额条件：退回与赠送都是无条件入账。
     *
     * @param memberId 会员 id，不能为 {@code null}
     * @param points 增加数量，必须为正数
     * @return 受影响行数：1 表示入账成功，0 表示会员不存在
     */
    @Update("UPDATE ums_member SET integration = COALESCE(integration, 0) + #{points} WHERE id = #{memberId}")
    int addIntegration(@Param("memberId") Long memberId, @Param("points") int points);

}
