package coupon.service;

import common.to.ProductReferenceBlockerTo;

import java.util.List;

/**
 * 商品删除前的营销引用核对与失效引用清理。
 *
 * <p>拦截与清理是两件事，但共用同一条判据 —— 引用是否仍然有效：
 * <ul>
 *   <li>{@link #listActiveBlockers} 只读，供 product 删除前的同步守卫调用，返回还活着的引用；</li>
 *   <li>{@link #deleteDeadReferences} 在商品确实删掉之后由异步清理调用，把已过使用期的券、
 *       已结束的场次留下的关联行删掉。</li>
 * </ul>
 *
 * <p><b>是否仍然有效只看时间</b>：券看 {@code sms_coupon.end_time}、场次看 {@code sms_seckill_session.end_time}，
 * 为 {@code null} 表示永久有效／没有结束时间，算有效。父行已经不存在的孤儿关联行算失效。这条判据与
 * 券的可用判定同源 —— 券的可用判定先看状态与时间、再看范围，所以一张已过使用期的券对任何人都不可用，
 * 它的范围行确实是失效数据。
 *
 * <p>本接口无状态、线程安全。
 */
public interface ProductReferenceService {

    /**
     * 列出这个商品还被哪些活着的营销配置引用。
     *
     * <p>只读，无副作用。调用方（product）拿到非空结果就整批拒绝删除。
     *
     * @param spuIds 待删除的 SPU ID 列表，允许为 {@code null} 或空集合
     * @param skuIds 待删除的 SKU ID 列表，允许为 {@code null} 或空集合
     * @return 阻塞清单；两类都没有时两个列表都是空列表，不会返回 {@code null}
     */
    ProductReferenceBlockerTo listActiveBlockers(List<Long> spuIds, List<Long> skuIds);

    /**
     * 删掉这个商品在营销侧的失效引用：已过使用期的券的范围行、已结束或已不存在的场次的关联行。
     *
     * <p><b>只删判定为失效的行</b>，仍然有效的一行都不动 —— 有效的引用在同步守卫那里就把删除拦住了，
     * 商品根本删不掉。幂等：全是不带状态的 DELETE，消息重投不会出错。
     *
     * @param spuIds 已删除商品的 SPU ID 列表，允许为 {@code null} 或空集合
     * @param skuIds 已删除商品的 SKU ID 列表，允许为 {@code null} 或空集合
     * @return 实际删除的关联行数，两类相加
     */
    int deleteDeadReferences(List<Long> spuIds, List<Long> skuIds);
}
