package thirdParty.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 地址树节点，只承载前端渲染树所需的编码、名称与子节点。 */
@Data
@AllArgsConstructor
public class AreaTreeNode {

    /** 节点编码，对应 {@code address.NODE_CODE}。 */
    private String code;

    /** 节点名称。 */
    private String name;

    /** 子节点，首次添加时惰性创建；叶子节点为 {@code null}。 */
    private List<AreaTreeNode> children;

    /**
     * 追加一个子节点，{@code children} 为 {@code null} 时惰性创建列表。
     *
     * @param child 子节点，不能为 {@code null}
     */
    public void addChildren(AreaTreeNode child) {
        if (children == null) {
            children = new ArrayList<>();
        }
        children.add(child);
    }
}
