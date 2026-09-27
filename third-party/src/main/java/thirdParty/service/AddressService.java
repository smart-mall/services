package thirdParty.service;

import thirdParty.vo.AreaTreeNode;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 行政区划服务：省市区地址树查询，以及按行政区划编码计算直线距离。
 *
 * <p>两者都只读 {@code address} 表，无状态、线程安全。
 */
public interface AddressService {

    /**
     * 查询完整的地址树。
     *
     * <p>实现方需保证返回值是一棵完整的树：子节点挂在 {@code codeParent} 指向的父节点下，
     * 父节点不存在的节点会被丢弃。
     *
     * @return 顶层节点列表；表中无数据时返回空列表，不返回 {@code null}
     */
    List<AreaTreeNode> getAddressTree();

    /**
     * 计算一个行政区划到多个行政区划的直线距离。
     *
     * <p>距离按两点的经纬度用半正矢公式计算，是球面直线距离，与真实路网里程无关；
     * 坐标取自 {@code address} 表的 {@code LNG} / {@code LAT}，精度是区划中心点，
     * 因此同区划内任意两点算出来是 0 公里。
     *
     * <p>实现方需保证：起点取不到坐标时直接报错；终点取不到坐标时只跳过该终点，
     * 不因为它让整批失败——一个仓库没配坐标不该让整单算不出运费。
     *
     * @param fromNode 起点行政区划编码，不能为 {@code null}，且必须在表中存在
     * @param toNodes 终点行政区划编码列表，不能为 {@code null}；允许为空列表
     * @return 终点编码到直线距离（单位公里，保留 3 位小数）的映射；
     *         编码不存在或没有坐标的终点不会出现在结果里，调用方需按缺失处理
     * @throws common.exception.BaseException 起点编码不存在时抛
     *         {@code GEO_NODE_NOT_FOUND}，起点没有坐标时抛 {@code GEO_COORDINATE_MISSING}
     */
    Map<String, BigDecimal> getDistanceKm(String fromNode, List<String> toNodes);
}
