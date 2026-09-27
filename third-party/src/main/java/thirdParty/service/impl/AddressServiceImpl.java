package thirdParty.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import org.springframework.stereotype.Service;
import thirdParty.entity.AddressEntity;
import thirdParty.mapper.AddressMapper;
import thirdParty.service.AddressService;
import thirdParty.vo.AreaTreeNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 行政区划服务实现：地址树在内存中组装，距离按经纬度用半正矢公式计算。
 *
 * <p>两者都只读 {@code address} 表，依赖 {@link AddressMapper} 的通用查询，无状态。
 */
@Service
public class AddressServiceImpl extends ServiceImpl<AddressMapper, AddressEntity> implements AddressService {

    /** 地球平均半径，单位公里。 */
    private static final double EARTH_RADIUS_KM = 6371.0088;

    /** {@inheritDoc} */
    @Override
    public List<AreaTreeNode> getAddressTree() {
        List<AddressEntity> addressEntities = baseMapper.selectList(null);

        // 只读一次全表：组装时每个节点都要按父编码找父节点，逐层查库会退化成 N+1
        Map<String, AreaTreeNode> allNodeMap = addressEntities.stream()
                .collect(Collectors.toMap(
                        AddressEntity::getNodeCode,
                        item -> new AreaTreeNode(item.getNodeCode(), item.getNodeName(), null)
                ));

        List<AreaTreeNode> result = new ArrayList<>();

        addressEntities.forEach(item -> {
            AreaTreeNode node = allNodeMap.get(item.getNodeCode());
            String parentCode = item.getCodeParent();

            // codeParent 为 "0" 表示顶层节点，直接作为树根
            if ("0".equals(parentCode)) {
                result.add(node);
            } else {
                AreaTreeNode parent = allNodeMap.get(parentCode);
                if (parent != null) {
                    parent.addChildren(node);
                }
            }
        });

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, BigDecimal> getDistanceKm(String fromNode, List<String> toNodes) {
        double[] fromPoint = requirePoint(fromNode);

        Map<String, BigDecimal> distances = new HashMap<>();
        for (String toNode : toNodes) {
            if (toNode == null || toNode.isBlank()) {
                continue;
            }
            AddressEntity to = getById(toNode);
            double[] toPoint = to == null ? null : parsePoint(to);
            // 终点取不到坐标就跳过：某个仓库没配区域编码不该让整单算不出运费
            if (toPoint == null) {
                continue;
            }
            distances.put(toNode, haversineKm(fromPoint, toPoint));
        }
        return distances;
    }

    /**
     * 取行政区划的坐标，取不到直接报错。
     *
     * @param nodeCode 行政区划编码，不能为 {@code null}
     * @return 长度为 2 的数组，依次是经度、纬度
     * @throws BaseException 编码不存在时抛 {@code GEO_NODE_NOT_FOUND}；
     *         编码存在但没有坐标时抛 {@code GEO_COORDINATE_MISSING}
     */
    private double[] requirePoint(String nodeCode) {
        AddressEntity entity = getById(nodeCode);
        if (entity == null) {
            throw new BaseException(BaseCodeEnum.GEO_NODE_NOT_FOUND, "行政区划编码不存在：" + nodeCode);
        }
        double[] point = parsePoint(entity);
        if (point == null) {
            throw new BaseException(BaseCodeEnum.GEO_COORDINATE_MISSING, "行政区划没有坐标：" + nodeCode);
        }
        return point;
    }

    /**
     * 解析行政区划的经纬度。
     *
     * @param entity 行政区划行，不能为 {@code null}
     * @return 长度为 2 的数组，依次是经度、纬度；经纬度为空或不是合法数字时返回 {@code null}
     */
    private double[] parsePoint(AddressEntity entity) {
        String lng = entity.getLng();
        String lat = entity.getLat();
        if (lng == null || lat == null) {
            return null;
        }
        try {
            return new double[]{Double.parseDouble(lng), Double.parseDouble(lat)};
        } catch (NumberFormatException e) {
            // 表里存在经纬度为空串的行；当成 0 会算出一个看着正常的错误距离，必须按缺失处理
            return null;
        }
    }

    /**
     * 用半正矢公式计算两个经纬度点之间的球面距离。
     *
     * @param from 起点，长度为 2 的数组，依次是经度、纬度
     * @param to 终点，格式同 {@code from}
     * @return 球面直线距离，单位公里，保留 3 位小数
     */
    private BigDecimal haversineKm(double[] from, double[] to) {
        double fromLat = Math.toRadians(from[1]);
        double toLat = Math.toRadians(to[1]);
        double deltaLat = toLat - fromLat;
        double deltaLng = Math.toRadians(to[0] - from[0]);

        // 经度差要乘两端纬度的余弦：越靠近两极，同样的经度差对应的实际距离越短
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(fromLat) * Math.cos(toLat) * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return BigDecimal.valueOf(EARTH_RADIUS_KM * c).setScale(3, RoundingMode.HALF_UP);
    }
}
