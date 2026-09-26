package seckill.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;



/**
 * 秒杀场次及其关联 SKU 的查询结果，由 coupon 服务提供，是上架任务的数据来源。
 */
@Data
public class SeckillSessionWithSkusVo {

    /** 场次 ID。 */
    private Long id;

    /** 场次名称。 */
    private String name;

    /** 每日开始时间。 */
    private Date startTime;

    /** 每日结束时间。 */
    private Date endTime;

    /** 启用状态。 */
    private Integer status;

    /** 创建时间。 */
    private Date createTime;

    /** 该场次关联的 SKU 秒杀配置。 */
    private List<SeckillSkuVo> relationSkus;

}
