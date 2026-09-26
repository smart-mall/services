package coupon.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 首页专题与商品的关联，对应 {@code sms_home_subject_spu} 表：描述一个专题下挂了哪些商品。
 *
 * <p>专题本身由 {@link HomeSubjectEntity} 描述。
 */
@Data
@TableName("sms_home_subject_spu")
public class HomeSubjectSpuEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键。 */
	@TableId
	private Long id;
	/** 所属专题名称。 */
	private String name;
	/**
	 * 所属专题 ID，关联 {@code sms_home_subject.id}。
	 */
	private Long subjectId;
	/** 专题内展示的商品 SPU ID。 */
	private Long spuId;
	/** 专题内商品的排序值。 */
	private Integer sort;

}
