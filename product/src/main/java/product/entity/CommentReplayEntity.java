package product.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 商品评价的回复关系，对应 {@code pms_comment_replay} 表：记录一条回复对应的是哪条评论。
 */
@Data
@TableName("pms_comment_replay")
public class CommentReplayEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键。 */
	@TableId
	private Long id;
	/**
	 * 被回复的评论 ID，指向 {@code pms_spu_comment.id}。
	 */
	private Long commentId;
	/** 回复 ID。 */
	private Long replyId;

}
