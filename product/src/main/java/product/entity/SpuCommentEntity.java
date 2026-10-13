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
 *
 * <p>会员端提交的直接评价带来源订单项 {@link #orderItemId}，它是"一件商品在一张订单里只能评价一次"
 * 的判据来源；会员归属记在 {@link #memberId}，昵称只是展示快照。回复行两者都不带，保持 {@code null}。
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
	/**
	 * 来源订单项 ID，指向 {@code oms_order_item.id}。
	 *
	 * <p>会员端的直接评价必须带它：同一张订单里的同一件商品只能评价一次，靠本列的唯一索引
	 * {@code uk_order_item} 兜底（唯一索引允许多个 {@code null}，所以回复行不受影响）。
	 */
	private Long orderItemId;
	/**
	 * 提交评价的会员 ID，指向 {@code ums_member.id}。
	 *
	 * <p>会员归属以本列为准，{@code memberNickName} 只是展示快照：昵称可以改、可以重名，
	 * 也可能为空（秒杀单没有会员名）。"我的评论"按本列分页，删除自己的评价也按本列做归属校验。
	 */
	private Long memberId;
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
