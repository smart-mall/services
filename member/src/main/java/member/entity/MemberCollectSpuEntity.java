package member.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 会员收藏的商品，对应 {@code ums_member_collect_spu} 表。
 *
 * <p>一条记录表示某会员收藏的一个 SPU，商品名称与主图随收藏记录一起保存。
 */
@Data
@TableName("ums_member_collect_spu")
public class MemberCollectSpuEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键。 */
	@TableId
	private Long id;
	/** 所属会员 ID。 */
	private Long memberId;
	/** 被收藏商品的 SPU ID。 */
	private Long spuId;
	/** 商品名称。 */
	private String spuName;
	/** 商品主图地址。 */
	private String spuImg;
	/** 收藏时间。 */
	private Date createTime;

}
