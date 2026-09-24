package common.mq.outbox;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 本地消息表。写入与业务数据在同一事务，提交成功即代表"业务改完了"和"有待投递消息"同时成立。
 */
@Data
@TableName("mq_message")
public class OutboxMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 同时作为 CorrelationData id，confirm 回调靠它定位记录 */
    @TableId
    private String messageId;

    /** 消息体 JSON */
    private String content;

    private String toExchange;

    private String routingKey;

    /** 消息体全限定类名，重投时靠它反序列化 */
    private String classType;

    /** 见 {@link OutboxStatus} */
    private Integer messageStatus;

    private Date createTime;

    private Date updateTime;
}
