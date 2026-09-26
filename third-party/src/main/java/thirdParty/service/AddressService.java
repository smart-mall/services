package thirdParty.service;

import thirdParty.vo.AreaTreeNode;

import java.util.List;

/**
 * 地址树查询服务。
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
}
