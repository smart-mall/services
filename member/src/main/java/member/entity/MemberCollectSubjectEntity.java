package member.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 会员收藏的专题活动，对应 {@code ums_member_collect_subject} 表。
 *
 * <p>活动名称、图片与跳转地址随收藏记录一起保存。
 */
@Data
@TableName("ums_member_collect_subject")
public class MemberCollectSubjectEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键。 */
	@TableId
	private Long id;
	/** 被收藏专题活动的 ID。 */
	private Long subjectId;
	/** 活动名称。 */
	private String subjectName;
	/** 活动图片地址。 */
	private String subjectImg;
	/** 活动跳转地址。 */
	private String subjectUrll;

}
