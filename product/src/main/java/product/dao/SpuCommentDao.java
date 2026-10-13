package product.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import product.entity.SpuCommentEntity;

/**
 * {@code pms_spu_comment} 表的 MyBatis-Plus Mapper。
 *
 * <p>自定义 SQL 只有一条 {@link #deleteOwn}：会员删除自己的评价时，归属校验必须与删除落在同一条
 * SQL 里，不能只在服务层判断。其余使用 {@link BaseMapper} 提供的通用增删改查。
 */
@Mapper
public interface SpuCommentDao extends BaseMapper<SpuCommentEntity> {

    /**
     * 删除会员自己的一条评价。
     *
     * <p>SQL 见 {@code mapper/product/SpuCommentDao.xml}：{@code WHERE} 带 {@code member_id}，
     * 归属校验与删除是同一条 SQL，删别人的或删不存在的评价都只会影响 0 行。</p>
     *
     * <p>物理删除：删掉之后该项的 {@code uk_order_item} 唯一索引随之释放，同一订单项可以重新评价。</p>
     *
     * @param id 评价主键，不能为 {@code null}
     * @param memberId 当前登录会员 ID，不能为 {@code null}
     * @return 受影响行数，1 表示已删除，0 表示评价不存在或不属于该会员
     */
    int deleteOwn(Long id, Long memberId);

}
