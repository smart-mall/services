package seckill.vo;

import lombok.Data;

/**
 * 秒杀上架结果，由一次上架过程写入并返回给触发方（定时任务与后台手动触发）。
 *
 * <p>非线程安全，每个实例只由一次上架过程写入。
 */
@Data
public class SeckillUploadResultVo {

    /** 本次取到并落在时间窗口内的场次数。 */
    private int sessionCount;

    /** 本次真正写入缓存的商品条目数。 */
    private int uploadedSkuCount;

    /** 因缓存中已存在而跳过的商品条目数：重复上架不覆盖已下发的随机码与库存。 */
    private int skippedSkuCount;
}
