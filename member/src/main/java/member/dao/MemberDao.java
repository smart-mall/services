package member.dao;

import member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * {@code ums_member} 表的 MyBatis-Plus Mapper，映射会员账号 {@link MemberEntity}。
 *
 * <p>除通用增删改查外只声明积分与成长值的几条原子增减语句，以及等级归属的写入：
 * 判断与写入必须落在同一条 SQL 里，先查余额再写会出现"两个并发请求各自读到够用的余额、
 * 双双扣成功"的窗口。
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

    /**
     * 增加成长值。
     *
     * <p>只按主键定位，不带条件：成长值没有"够不够"的概念，只有积分余额才有。
     * {@code growth} 列允许为 {@code null}，统一按 0 参与运算。
     *
     * @param memberId 会员 id，不能为 {@code null}
     * @param points 增加数量，必须为正数
     * @return 受影响行数：1 表示入账成功，0 表示会员不存在
     */
    @Update("UPDATE ums_member SET growth = COALESCE(growth, 0) + #{points} WHERE id = #{memberId}")
    int addGrowth(@Param("memberId") Long memberId, @Param("points") int points);

    /**
     * 把会员的等级改为指定等级。
     *
     * <p>只写 {@code level_id} 一列：等级归属由成长值门槛算出，别的列不参与。
     * "只升不降"的判定在调用方，本方法不做比较。
     *
     * @param memberId 会员 id，不能为 {@code null}
     * @param levelId 目标等级 id，不能为 {@code null}
     * @return 受影响行数：1 表示改动成功，0 表示会员不存在
     */
    @Update("UPDATE ums_member SET level_id = #{levelId} WHERE id = #{memberId}")
    int updateLevel(@Param("memberId") Long memberId, @Param("levelId") Long levelId);

    /**
     * 查今天生日的会员 id。
     *
     * <p>"今天"由数据库的 {@code NOW()} 判定，不由应用算好月日再传进来：应用与库的时区不一致时，
     * 传进来的月日会与 {@code birth} 落在两套时间上。
     *
     * <p>{@code birth} 为 {@code null} 的会员不参与 —— 资料里没填生日就等于没有这份权益。
     *
     * @return 会员 id 列表；今天没有人过生日时为空列表
     */
    @Select("SELECT id FROM ums_member WHERE birth IS NOT NULL "
            + "AND DATE_FORMAT(birth, '%m-%d') = DATE_FORMAT(NOW(), '%m-%d')")
    List<Long> listBirthdayMemberIds();

}
