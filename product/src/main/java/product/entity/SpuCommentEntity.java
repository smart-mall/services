package product.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 商品评价，对应 {@code pms_spu_comment} 表：保存会员对商品的评价内容与回复内容。
 *
 * <p>评价与回复共用本表，靠 {@code commentType} 区分；回复挂在哪条评论下由 {@link CommentReplayEntity} 记录。
 */
@Data
@TableName("pms_spu_comment")
public class SpuCommentEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键。 */
	@TableId
	private Long id;
	/**
	 * 被评价的 SKU ID，指向 {@code pms_sku_info.sku_id}。
	 */
	private Long skuId;
	/**
	 * 被评价的 SPU ID，指向 {@code pms_spu_info.id}。
	 */
	private Long spuId;
	/** 商品名快照。 */
	private String spuName;
	/** 会员昵称。 */
	private String memberNickName;
	/** 星级评分。 */
	private Integer star;
	/** 会员 IP。 */
	private String memberIp;
	/** 评价创建时间。 */
	private Date createTime;
	/** 显示状态[0-不显示，1-显示]。 */
	private Integer showStatus;
	/** 购买时的销售属性组合。 */
	private String spuAttributes;
	/** 点赞数。 */
	private Integer likesCount;
	/** 回复数。 */
	private Integer replyCount;
	/**
	 * 评论图片或视频，JSON 数组，元素形如 [{type:文件类型,url:资源路径}]。
	 */
	private String resources;
	/** 评价内容。 */
	private String content;
	/** 会员头像地址。 */
	private String memberIcon;
	/** 评论类型[0-对商品的直接评论，1-对评论的回复]。 */
	private Integer commentType;

}
