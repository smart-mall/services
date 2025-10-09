package ware.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.utils.PageUtils;
import common.utils.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ware.costant.PurchaseDetailEnum;
import ware.costant.PurchaseStatusEnum;
import ware.dao.PurchaseDao;
import ware.dao.PurchaseDetailDao;
import ware.entity.PurchaseDetailEntity;
import ware.entity.PurchaseEntity;
import ware.service.PurchaseService;
import ware.service.WareSkuService;
import ware.vo.MergeVO;
import ware.vo.PurchaseDoneVO;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {
    private final PurchaseDetailDao purchaseDetailDao;
    private final WareSkuService wareSkuService;

    public PurchaseServiceImpl(PurchaseDetailDao purchaseDetailDao, WareSkuService wareSkuService) {
        this.purchaseDetailDao = purchaseDetailDao;
        this.wareSkuService = wareSkuService;
    }


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageUnreceive(Map<String, Object> params) {
        LambdaQueryWrapper<PurchaseEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PurchaseEntity::getStatus, 0)
                .or()
                .eq(PurchaseEntity::getStatus, 1);

        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    @Override
    @Transactional
    public void merge(MergeVO mergeVO) {
        Long purchaseId = mergeVO.getPurchaseId();
        if (purchaseId != null) {
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            Date now = new Date();
            purchaseEntity.setUpdateTime(now);
            purchaseEntity.setCreateTime(now);
            purchaseEntity.setStatus(PurchaseStatusEnum.CREATED.getCode());
            baseMapper.insert(purchaseEntity);

            purchaseId = purchaseEntity.getId();

        }

        PurchaseEntity purchaseEntity = baseMapper.selectById(purchaseId);
        if (purchaseEntity.getStatus() == PurchaseStatusEnum.CREATED.getCode() || purchaseEntity.getStatus() == PurchaseStatusEnum.ASSIGNED.getCode()) {
            List<Long> items = mergeVO.getItems();

            LambdaUpdateWrapper<PurchaseDetailEntity> updateChainWrapper = new LambdaUpdateWrapper<>(PurchaseDetailEntity.class);

            updateChainWrapper.set(PurchaseDetailEntity::getStatus, PurchaseDetailEnum.ASSIGNED.getCode())
                    .set(PurchaseDetailEntity::getPurchaseId, purchaseId)
                    .in(PurchaseDetailEntity::getId, items);
            purchaseDetailDao.update(updateChainWrapper);
        }


    }

    @Override
    @Transactional
    public void receive(List<Long> ids) {
        List<Long> list = baseMapper.selectByIds(ids).stream()
                .filter(purchase -> purchase.getStatus() == PurchaseStatusEnum.CREATED.getCode() || purchase.getStatus() == PurchaseStatusEnum.ASSIGNED.getCode())
                .map(PurchaseEntity::getId).toList();

        LambdaUpdateWrapper<PurchaseEntity> updateChainWrapper = new LambdaUpdateWrapper<>(PurchaseEntity.class);
        updateChainWrapper.set(PurchaseEntity::getUpdateTime, new Date())
                .set(PurchaseEntity::getStatus, PurchaseStatusEnum.RECEIVE.getCode())
                .set(PurchaseEntity::getUpdateTime, new Date())
                .in(PurchaseEntity::getId, list);

        baseMapper.update(updateChainWrapper);

        LambdaUpdateWrapper<PurchaseDetailEntity> updateWrapper = new LambdaUpdateWrapper<>(PurchaseDetailEntity.class);
        updateWrapper.set(PurchaseDetailEntity::getStatus, PurchaseDetailEnum.BUYING.getCode())
                .in(PurchaseDetailEntity::getPurchaseId, list);
        purchaseDetailDao.update(updateWrapper);
    }

    @Override
    public void done(PurchaseDoneVO purchaseDoneVO) {
        Long purchaseId = purchaseDoneVO.getId();

        List<PurchaseDoneVO.PurchaseItemVO> items = purchaseDoneVO.getItems();
        boolean flag = true;
        List<PurchaseDetailEntity> updates = new ArrayList<>();
        for (PurchaseDoneVO.PurchaseItemVO item : items) {
            if (item.getStatus() == PurchaseDetailEnum.HASERROR.getCode()) {
                flag = false;
            }
            PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
            purchaseDetailEntity.setId(item.getItemId());
            purchaseDetailEntity.setStatus(item.getStatus());
            updates.add(purchaseDetailEntity);
        }

        purchaseDetailDao.updateById(updates);

        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(purchaseId);
        purchaseEntity.setStatus(flag ? PurchaseStatusEnum.FINISH.getCode() : PurchaseStatusEnum.HASERROR.getCode());
        purchaseEntity.setUpdateTime(new Date());
        baseMapper.updateById(purchaseEntity);

        List<PurchaseDetailEntity> addStockList = updates.stream().filter(item -> item.getStatus() == PurchaseDetailEnum.HASERROR.getCode()).toList();

        addStockList.forEach(item -> {
            wareSkuService.addStock(item.getSkuId(), item.getWareId(), item.getSkuNum());
        });
    }

}