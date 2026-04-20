package order;

import order.entity.OrderEntity;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

@SpringBootTest
class OrderApplicationTests {
    @Autowired
    AmqpAdmin amqpAdmin;
    @Autowired
    RabbitTemplate rabbitTemplate;

    @Test
    void contextLoads() {
        amqpAdmin.declareExchange(
                new DirectExchange("hello-java-exchange",true,false)
        );
        amqpAdmin.declareQueue(
                new Queue("hello-java-queue",true,false,false)
        );
        amqpAdmin.declareBinding(
                new Binding("hello-java-queue",
                        Binding.DestinationType.QUEUE,
                        "hello-java-exchange",
                        "hello.java",null)
        );

    }

    @Test
    void sendMsg() {
        // 构建数据类
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setId(1L);
        orderEntity.setOrderSn("1");
        orderEntity.setMemberId(1L);
        orderEntity.setCreateTime(new Date());
        orderEntity.setMemberUsername("1");
        orderEntity.setTotalAmount(new BigDecimal("1"));
        orderEntity.setPayAmount(new BigDecimal("1"));
        orderEntity.setFreightAmount(new BigDecimal("1"));
        orderEntity.setPromotionAmount(new BigDecimal("1"));
        orderEntity.setIntegrationAmount(new BigDecimal("1"));
        orderEntity.setCouponAmount(new BigDecimal("1"));
        rabbitTemplate.convertAndSend("hello-java-exchange","hello.java",orderEntity, new CorrelationData(UUID.randomUUID().toString()));
    }

    @Test
    void getMsg() {
        OrderEntity order = rabbitTemplate.receiveAndConvert(
                "hello-java-queue",
                new ParameterizedTypeReference<>() {
                }
        );
        System.out.println("接收到消息：" + order);

    }

}
