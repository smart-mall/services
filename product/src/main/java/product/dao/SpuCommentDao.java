package product.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import product.entity.SpuCommentEntity;

/**
 * {@code pms_spu_comment} 表的 MyBatis-Plus Mapper。
 *
 * <p>自定义 SQL 都落在"校验必须与写入写进同一条语句"的地方：{@link #deleteOwn} 的会员归属、
 * {@link #updateLikesCount} 的可见性。计数只做加减，不加行锁，并发点赞的最终值仍是对的。
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
     * @return 受影响行数，1 表示已删除，0 表示评价不存在、不属于该会员，或这一行是回复
     */
    int deleteOwn(Long id, Long memberId);

    /**
     * 按增量调整一条评价或回复的点赞数。
     *
     * <p>SQL 见 {@code mapper/product/SpuCommentDao.xml}：{@code WHERE} 带可见性条件，
     * 不可见或不是评价/回复的行改不动；计数用 {@code GREATEST} 兜底，取消点赞不会减成负数。</p>
     *
     * @param id 评价或回复主键，不能为 {@code null}
     * @param delta 增量，点赞为 1、取消点赞为 -1
     * @return 受影响行数，0 表示这一行不存在或已不可见
     */
    int updateLikesCount(Long id, int delta);

    /**
     * 把一条直接评价的回复数加一。
     *
     * @param commentId 评价主键，不能为 {@code null}
     * @return 受影响行数，0 表示评价不存在
     */
    int increaseReplyCount(Long commentId);

    /**
     * 删除一条评价下的全部回复内容行。
     *
     * <p>回复挂在哪条评价下记在 {@code pms_comment_replay}，所以本语句要 join 那张表定位；
     * 调用方必须在删除关系行<b>之前</b>调用它，关系行没了就再也找不到这些回复。</p>
     *
     * @param commentId 根评价主键，不能为 {@code null}
     * @return 受影响行数，没有回复时为 0
     */
    int deleteReplies(Long commentId);

}
