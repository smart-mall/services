package product.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import product.entity.CommentReplayEntity;

/**
 * {@code pms_comment_replay} 表的 MyBatis-Plus Mapper。
 *
 * <p>自定义 SQL 只有 {@link #deleteByCommentId}：删除根评价时清掉它的关系行，
 * 回复内容行的删除要在这之前完成，见 {@code SpuCommentDao#deleteReplies}。
 */
@Mapper
public interface CommentReplayDao extends BaseMapper<CommentReplayEntity> {

    /**
     * 删除一条根评价的全部回复关系行。
     *
     * @param commentId 根评价主键，不能为 {@code null}
     * @return 受影响行数，没有回复时为 0
     */
    int deleteByCommentId(Long commentId);

}
