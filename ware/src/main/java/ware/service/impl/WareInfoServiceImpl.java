package ware.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.utils.PageUtils;
import common.utils.Query;
import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ware.dao.WareInfoDao;
import ware.entity.WareInfoEntity;
import ware.feign.MemberFeignService;
import ware.service.WareInfoService;
import ware.vo.FareVo;
import ware.vo.MemberAddressVo;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service("wareInfoService")
public class WareInfoServiceImpl extends ServiceImpl<WareInfoDao, WareInfoEntity> implements WareInfoService {
    public final MemberFeignService memberFeignService;

    public WareInfoServiceImpl(MemberFeignService memberFeignService) {
        this.memberFeignService = memberFeignService;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        LambdaQueryWrapper<WareInfoEntity> queryWrapper = new LambdaQueryWrapper<>();

        String key = (String) params.get("key");
        if (key != null && !key.isEmpty()) {
            queryWrapper
                    .eq(WareInfoEntity::getId, key)
                    .or()
                    .like(WareInfoEntity::getName, key)
                    .or()
                    .like(WareInfoEntity::getAddress, key);
        }

        IPage<WareInfoEntity> page = this.page(
                new Query<WareInfoEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    @Override
    public FareVo getFare(Long addrId) {

        FareVo fareVo = new FareVo();

        //收获地址的详细信息
        R addrInfo = memberFeignService.info(addrId);
        log.info("收获地址信息：{}", JSON.toJSONString(addrInfo, SerializerFeature.PrettyFormat));

        MemberAddressVo memberAddressVo = addrInfo.getData("memberReceiveAddress",new TypeReference<MemberAddressVo>() {});

        if (memberAddressVo != null) {
            String phone = memberAddressVo.getPhone();
            //截取用户手机号码最后一位作为我们的运费计算
//            1558022051
            String fare = phone.substring(phone.length() - 10, phone.length()-8);

            fareVo.setFare(new BigDecimal(fare));
            fareVo.setAddress(memberAddressVo);

            return fareVo;
        }
        return null;
    }
}