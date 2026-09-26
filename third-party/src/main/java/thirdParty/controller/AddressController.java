package thirdParty.controller;

import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import thirdParty.service.AddressService;

import java.util.List;
import thirdParty.vo.AreaTreeNode;

/**
 * 省市区地址树查询接口。
 *
 * <p>前后台共用且不需要登录态，因此挂在 {@code thirdParty/front} 这个公开路径下。
 */
@RestController
@RequestMapping("thirdParty/front/address")
@Slf4j
public class AddressController {
    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    /**
     * 查询完整的地址树。
     *
     * <p>每次请求都全量读 {@code address} 表并在内存中组装，结果不缓存。
     *
     * @return 顶层节点列表，子节点按层级挂在 {@code children} 下；表中无数据时返回空列表
     */
    @RequestMapping("/tree")
    public R<List<AreaTreeNode>> getAddressTree() {
        log.info("获取地址树形结构信息");
        return R.ok(addressService.getAddressTree());
    }
}
