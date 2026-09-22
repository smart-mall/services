package product.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 本地消息表（outbox）。product 库自己的那张，和 order 库的 {@code order.entity.MqMessageEntity}
 * 是两张互不相干的表 —— outbox 必须和业务变更在同一个库、同一个事务里，所以不能共用。
 *
 * <p>写入时机：和业务数据在<b>同一个事务</b>里 insert。这是整套可靠消息的地基 ——
 * 提交成功就意味着"业务改完了"和"有一条消息待投递"同时成立，没有中间态。</p>
 */
@Data
@TableName("mq_message")
public class MqMessageEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 消息 id，同时作为 RabbitMQ 的 CorrelationData id，confirm 回调靠它定位记录 */
	@TableId
	private String messageId;
	/** 消息体 JSON */
	private String content;
	/** 目标交换机 */
	private String toExchange;
	/** 路由键。比 order 库那张多这一列 —— 光有交换机是发不出去的 */
	private String routingKey;
	/** 消息体的全限定类名，重投时靠它反序列化回对象 */
	private String classType;
	/** 见 {@link MqMessageStatus} */
	private Integer messageStatus;
	private Date createTime;
	private Date updateTime;
}
