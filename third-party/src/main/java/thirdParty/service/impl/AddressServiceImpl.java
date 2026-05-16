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

@Service
public class AddressServiceImpl extends ServiceImpl<AddressMapper, AddressEntity> implements AddressService {
    @Override
    public List<AreaTreeNode> getAddressTree() {
        List<AddressEntity> addressEntities = baseMapper.selectList(null);

        // 全部转成 Map<nodeCode, AreaTreeNode>（所有节点）
        Map<String, AreaTreeNode> allNodeMap = addressEntities.stream()
                .collect(Collectors.toMap(
                        AddressEntity::getNodeCode,
                        item -> new AreaTreeNode(item.getNodeCode(), item.getNodeName(), null)
                ));

        List<AreaTreeNode> result = new ArrayList<>();

        addressEntities.forEach(item -> {
            AreaTreeNode node = allNodeMap.get(item.getNodeCode());
            String parentCode = item.getCodeParent();

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
