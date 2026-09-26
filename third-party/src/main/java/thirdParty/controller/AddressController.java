package thirdParty.controller;

import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import thirdParty.service.AddressService;

import java.util.List;
import thirdParty.vo.AreaTreeNode;
/** 省市区地址树。前后台共用且不需要登录态，挂在 front 约定下的公开路径上 */
@RestController
@RequestMapping("thirdParty/front/address")
@Slf4j
public class AddressController {
    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    // 获取地址树形结构信息
    @RequestMapping("/tree")
    public R<List<AreaTreeNode>> getAddressTree() {
        log.info("获取地址树形结构信息");
        return R.ok(addressService.getAddressTree());
    }
}
