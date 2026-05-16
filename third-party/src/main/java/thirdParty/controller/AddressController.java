package thirdParty.controller;

import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import thirdParty.service.AddressService;

@RestController
@RequestMapping("/thirdParty/address")
@Slf4j
public class AddressController {
    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    // 获取地址树形结构信息
    @RequestMapping("/tree")
    public R getAddressTree() {
        log.info("获取地址树形结构信息");
        return R.ok().setData(addressService.getAddressTree());
    }
}
