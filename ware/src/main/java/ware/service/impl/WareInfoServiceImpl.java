package ware.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.exception.ValidationException;
import common.utils.PageUtils;
import common.utils.Query;
import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ware.dao.PurchaseDao;
import ware.dao.WareInfoDao;
import ware.dao.WareOrderTaskDao;
import ware.dao.WareSkuDao;
import ware.entity.PurchaseEntity;
import ware.entity.WareInfoEntity;
import ware.entity.WareOrderTaskEntity;
import ware.entity.WareSkuEntity;
import ware.feign.MemberFeignService;
import ware.service.WareInfoService;
import ware.vo.FareVo;
import ware.vo.MemberAddressVo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service("wareInfoService")
public class WareInfoServiceImpl extends ServiceImpl<WareInfoDao, WareInfoEntity> implements WareInfoService {
    public final MemberFeignService memberFeignService;

    private final WareSkuDao wareSkuDao;
    private final PurchaseDao purchaseDao;
    private final WareOrderTaskDao wareOrderTaskDao;

    public WareInfoServiceImpl(MemberFeignService memberFeignService, WareSkuDao wareSkuDao,
                               PurchaseDao purchaseDao, WareOrderTaskDao wareOrderTaskDao) {
        this.memberFeignService = memberFeignService;
        this.wareSkuDao = wareSkuDao;
        this.purchaseDao = purchaseDao;
        this.wareOrderTaskDao = wareOrderTaskDao;
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

    /**
     * 删除仓库。有库存行或关联单据的仓库不能删。
     *
     * <p>库里没有外键，删掉之后 {@code wms_ware_sku} / {@code wms_purchase} /
     * {@code wms_ware_order_task} 里的 {@code ware_id} 就变成悬空引用 —— 商品库存页那边
     * 按 ware_id 找不到仓库名，会直接 500。</p>
     */
    @Override
    @Transactional
    public void deleteByIds(List<Long> ids) {
        List<Long> distinctIds = ids == null
                ? List.of()
                : ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) {
            throw new ValidationException("ids", "请先选择要删除的仓库");
        }

        List<WareInfoEntity> wares = this.listByIds(distinctIds);
        if (wares.size() != distinctIds.size()) {
            throw new BaseException(BaseCodeEnum.WARE_NOT_FOUND);
        }

        for (WareInfoEntity ware : wares) {
            Long stockCount = wareSkuDao.selectCount(new LambdaQueryWrapper<WareSkuEntity>()
                    .eq(WareSkuEntity::getWareId, ware.getId()));
            Long purchaseCount = purchaseDao.selectCount(new LambdaQueryWrapper<PurchaseEntity>()
                    .eq(PurchaseEntity::getWareId, ware.getId()));
            Long taskCount = wareOrderTaskDao.selectCount(new LambdaQueryWrapper<WareOrderTaskEntity>()
                    .eq(WareOrderTaskEntity::getWareId, ware.getId()));

            if (stockCount > 0 || purchaseCount > 0 || taskCount > 0) {
                throw new BaseException(BaseCodeEnum.WARE_IN_USE,
                        "仓库「" + ware.getName() + "」还有库存 " + stockCount
                                + " 条、采购单 " + purchaseCount
                                + " 张、库存工作单 " + taskCount + " 张，不能删除");
            }
        }

        this.removeByIds(distinctIds);
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
            if (phone == null || phone.length() < 10) {
                // 运费算法本身是 demo：取手机号倒数第 10~8 位当金额。
                // 但手机号可能为空或长度不足（历史数据、测试数据），原来直接 substring 会
                // StringIndexOutOfBoundsException —— 在 order 那边表现为一个没有 code 的 500，
                // 整个结算页打不开。这里按 0 处理并留一条日志。
                log.warn("收货地址 {} 的手机号无法用于计算运费，按 0 处理：{}", addrId, phone);
                fareVo.setFare(BigDecimal.ZERO);
            } else {
                //截取用户手机号码最后一位作为我们的运费计算
                String fare = phone.substring(phone.length() - 10, phone.length()-8);
                fareVo.setFare(new BigDecimal(fare));
            }

            fareVo.setAddress(memberAddressVo);

            return fareVo;
        }
        return null;
    }
}
