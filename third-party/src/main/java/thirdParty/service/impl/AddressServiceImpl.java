package thirdParty.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import thirdParty.entity.AddressEntity;
import thirdParty.mapper.AddressMapper;
import thirdParty.service.AddressService;
import thirdParty.vo.AreaTreeNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 地址树查询实现：一次性读出全表，再在内存中按 {@code codeParent} 组装成树。
 *
 * <p>依赖 {@link AddressMapper} 的通用查询，无状态。
 */
@Service
public class AddressServiceImpl extends ServiceImpl<AddressMapper, AddressEntity> implements AddressService {

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
}
