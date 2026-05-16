package thirdParty.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class AreaTreeNode {
    private String code;
    private String name;
    private List<AreaTreeNode> children;

    public void addChildren(AreaTreeNode child) {
        if (children == null) {
            children = new ArrayList<>();
        }
        children.add(child);
    }
}
