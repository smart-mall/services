package ware.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.exception.ValidationException;
import common.utils.PageUtils;
import common.utils.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ware.costant.PurchaseDetailEnum;
import ware.costant.PurchaseStatusEnum;
import ware.dao.PurchaseDao;
import ware.dao.PurchaseDetailDao;
import ware.entity.PurchaseDetailEntity;
import ware.entity.PurchaseEntity;
import ware.entity.WareInfoEntity;
import ware.service.PurchaseService;
import ware.service.WareInfoService;
import ware.service.WareSkuService;
import ware.vo.MergeVO;
import ware.vo.PurchaseAssignVO;
import ware.vo.PurchaseDoneVO;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {

    private final PurchaseDetailDao purchaseDetailDao;
    private final WareSkuService wareSkuService;
    private final WareInfoService wareInfoService;

    public PurchaseServiceImpl(PurchaseDetailDao purchaseDetailDao, WareSkuService wareSkuService, WareInfoService wareInfoService) {
        this.purchaseDetailDao = purchaseDetailDao;
        this.wareSkuService = wareSkuService;
        this.wareInfoService = wareInfoService;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        LambdaQueryWrapper<PurchaseEntity> queryWrapper = new LambdaQueryWrapper<>();

        String key = (String) params.get("key");
        if (key != null && !key.isEmpty()) {
            queryWrapper.and(item -> item.eq(PurchaseEntity::getId, key)
                    .or().like(PurchaseEntity::getAssigneeName, key));
        }

        String status = (String) params.get("status");
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq(PurchaseEntity::getStatus, status);
        }

        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                queryWrapper
        );

        List<WareInfoEntity> wareInfoEntities = wareInfoService.list();
        page.getRecords().forEach(item -> {
            item.setWareName(wareInfoEntities.stream()
                    .filter(wareInfoEntity -> wareInfoEntity.getId().equals(item.getWareId()))
                    .findFirst()
                    .map(WareInfoEntity::getName)
                    .orElse(null));
            item.setAllowedActions(PurchaseStatusEnum.allowedActions(item.getStatus()));
        });

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageUnreceive(Map<String, Object> params) {
        LambdaQueryWrapper<PurchaseEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(PurchaseEntity::getStatus, PurchaseStatusEnum.openCodes());

        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                queryWrapper
        );

        page.getRecords().forEach(item -> item.setAllowedActions(PurchaseStatusEnum.allowedActions(item.getStatus())));

        return new PageUtils(page);
    }

    /**
     * 合并采购需求单：没指定采购单就新建一张，指定了就并进去。
     *
     * <p>两边都要卡状态 —— 采购单必须还没被领取，需求单必须还没并入任何单。
     * 否则可以把已经并到别的单里、甚至已经在采购的需求单再并一次，等于把它从原单里偷走。</p>
     */
    @Override
    @Transactional
    public void merge(MergeVO mergeVO) {
        List<Long> items = mergeVO.getItems() == null
                ? List.of()
                : mergeVO.getItems().stream().filter(Objects::nonNull).distinct().toList();
        if (items.isEmpty()) {
            throw new ValidationException("items", "请先选择要合并的采购需求单");
        }

        Long purchaseId = mergeVO.getPurchaseId();
        if (purchaseId == null) {
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            Date now = new Date();
            purchaseEntity.setStatus(PurchaseStatusEnum.CREATED.getCode());
            purchaseEntity.setCreateTime(now);
            purchaseEntity.setUpdateTime(now);
            baseMapper.insert(purchaseEntity);
            purchaseId = purchaseEntity.getId();
        } else {
            PurchaseEntity purchaseEntity = baseMapper.selectById(purchaseId);
            if (purchaseEntity == null) {
                throw new BaseException(BaseCodeEnum.PURCHASE_NOT_FOUND);
            }
            if (!PurchaseStatusEnum.isOpen(purchaseEntity.getStatus())) {
                throw new BaseException(BaseCodeEnum.PURCHASE_STATUS_INVALID);
            }
        }

        List<PurchaseDetailEntity> details = purchaseDetailDao.selectByIds(items);
        if (details.size() != items.size()) {
            throw new BaseException(BaseCodeEnum.PURCHASE_DETAIL_NOT_FOUND);
        }

        boolean mergeable = details.stream().allMatch(detail ->
                detail.getPurchaseId() == null && PurchaseDetailEnum.isNew(detail.getStatus()));
        if (!mergeable) {
            throw new BaseException(BaseCodeEnum.PURCHASE_DETAIL_STATUS_INVALID);
        }

        LambdaUpdateWrapper<PurchaseDetailEntity> updateChainWrapper = new LambdaUpdateWrapper<>(PurchaseDetailEntity.class);
        updateChainWrapper.set(PurchaseDetailEntity::getStatus, PurchaseDetailEnum.ASSIGNED.getCode())
                .set(PurchaseDetailEntity::getPurchaseId, purchaseId)
                .in(PurchaseDetailEntity::getId, items);
        purchaseDetailDao.update(updateChainWrapper);
    }

    /**
     * 取消分配：把需求单从采购单里摘出来，退回"新建"，之后才能再改。
     *
     * <p>采购单一旦"已领取"，它下面的需求单就一起冻结 —— 采购员已经照着在买了，
     * 这时把明细抽走，他手上的单和系统里的就对不上。</p>
     */
    @Override
    @Transactional
    public void unassign(List<Long> itemIds) {
        List<Long> distinctIds = itemIds == null
                ? List.of()
                : itemIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) {
            throw new ValidationException("items", "请先选择要取消分配的采购需求单");
        }

        List<PurchaseDetailEntity> details = purchaseDetailDao.selectByIds(distinctIds);
        if (details.size() != distinctIds.size()) {
            throw new BaseException(BaseCodeEnum.PURCHASE_DETAIL_NOT_FOUND);
        }

        boolean allAssigned = details.stream().allMatch(detail ->
                detail.getPurchaseId() != null && PurchaseDetailEnum.canUnassign(detail.getStatus()));
        if (!allAssigned) {
            throw new BaseException(BaseCodeEnum.PURCHASE_DETAIL_STATUS_INVALID);
        }

        List<Long> purchaseIds = details.stream().map(PurchaseDetailEntity::getPurchaseId).distinct().toList();
        List<PurchaseEntity> purchases = baseMapper.selectByIds(purchaseIds);
        boolean allOpen = purchases.size() == purchaseIds.size()
                && purchases.stream().allMatch(purchase -> PurchaseStatusEnum.isOpen(purchase.getStatus()));
        if (!allOpen) {
            throw new BaseException(BaseCodeEnum.PURCHASE_STATUS_INVALID);
        }

        // purchase_id 要显式置成 null，所以用 setSql —— set(column, null) 对 null 的处理不好赌
        LambdaUpdateWrapper<PurchaseDetailEntity> updateWrapper = new LambdaUpdateWrapper<>(PurchaseDetailEntity.class);
        updateWrapper.set(PurchaseDetailEntity::getStatus, PurchaseDetailEnum.CREATED.getCode())
                .setSql("purchase_id = null")
                .in(PurchaseDetailEntity::getId, distinctIds);
        purchaseDetailDao.update(updateWrapper);
    }

    /**
     * 分配采购人员：只改采购员那三列，并把"新建"推进到"已分配"。
     *
     * <p>已领取的单不许换人 —— 采购员已经照着单在买了，中途换人会出现"谁买的"说不清。</p>
     */
    @Override
    @Transactional
    public void assign(PurchaseAssignVO assignVO) {
        Long purchaseId = assignVO.getId();
        if (purchaseId == null) {
            throw new ValidationException("id", "请先选择要分配的采购单");
        }
        if (assignVO.getAssigneeId() == null || !StringUtils.hasText(assignVO.getAssigneeName())) {
            throw new ValidationException("assigneeId", "请选择采购人员");
        }

        PurchaseEntity purchaseEntity = baseMapper.selectById(purchaseId);
        if (purchaseEntity == null) {
            throw new BaseException(BaseCodeEnum.PURCHASE_NOT_FOUND);
        }
        if (!PurchaseStatusEnum.isOpen(purchaseEntity.getStatus())) {
            throw new BaseException(BaseCodeEnum.PURCHASE_STATUS_INVALID);
        }

        PurchaseEntity update = new PurchaseEntity();
        update.setId(purchaseId);
        update.setAssigneeId(assignVO.getAssigneeId());
        update.setAssigneeName(assignVO.getAssigneeName());
        update.setPhone(assignVO.getPhone());
        update.setStatus(PurchaseStatusEnum.ASSIGNED.getCode());
        update.setUpdateTime(new Date());
        baseMapper.updateById(update);
    }

    /**
     * 删除采购单：只有"还没领取、且没有明细"的空单能删。
     *
     * <p>有明细的单删掉之后，明细的 purchase_id 会悬空、状态还停在"已分配"，
     * 而合并要求 purchase_id 为空 —— 这些需求单就再也合并不了了。</p>
     */
    @Override
    @Transactional
    public void removePurchase(List<Long> ids) {
        List<Long> distinctIds = ids == null
                ? List.of()
                : ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) {
            throw new ValidationException("ids", "请先选择要删除的采购单");
        }

        List<PurchaseEntity> purchases = baseMapper.selectByIds(distinctIds);
        if (purchases.size() != distinctIds.size()) {
            throw new BaseException(BaseCodeEnum.PURCHASE_NOT_FOUND);
        }

        boolean allOpen = purchases.stream().allMatch(purchase -> PurchaseStatusEnum.isOpen(purchase.getStatus()));
        if (!allOpen) {
            throw new BaseException(BaseCodeEnum.PURCHASE_STATUS_INVALID);
        }

        Long detailCount = purchaseDetailDao.selectCount(new LambdaQueryWrapper<PurchaseDetailEntity>()
                .in(PurchaseDetailEntity::getPurchaseId, distinctIds));
        if (detailCount > 0) {
            throw new BaseException(BaseCodeEnum.PURCHASE_HAS_DETAIL);
        }

        this.removeByIds(distinctIds);
    }

    /**
     * 领取采购单：单从"新建/已分配"变成"已领取"，单下明细一起进入"正在采购"。
     *
     * <p>明细只动还是"已分配"的那些，避免把已经完成或失败的明细又拉回采购中；
     * 一张没有明细的空单也不许领取，否则会卡在"已领取"且永远完不成。</p>
     */
    @Override
    @Transactional
    public void receive(List<Long> ids) {
        List<Long> distinctIds = ids == null
                ? List.of()
                : ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) {
            throw new ValidationException("ids", "请先选择要领取的采购单");
        }

        List<PurchaseEntity> purchases = baseMapper.selectByIds(distinctIds);
        if (purchases.size() != distinctIds.size()) {
            throw new BaseException(BaseCodeEnum.PURCHASE_NOT_FOUND);
        }

        List<Long> receivable = purchases.stream()
                .filter(purchase -> PurchaseStatusEnum.isOpen(purchase.getStatus()))
                .map(PurchaseEntity::getId)
                .toList();
        if (receivable.isEmpty()) {
            throw new BaseException(BaseCodeEnum.PURCHASE_STATUS_INVALID);
        }

        List<PurchaseDetailEntity> details = purchaseDetailDao.selectList(new LambdaQueryWrapper<PurchaseDetailEntity>()
                .in(PurchaseDetailEntity::getPurchaseId, receivable));
        Set<Long> purchaseIdsWithDetail = details.stream()
                .map(PurchaseDetailEntity::getPurchaseId)
                .collect(Collectors.toSet());
        if (!purchaseIdsWithDetail.containsAll(receivable)) {
            throw new BaseException(BaseCodeEnum.PURCHASE_DETAIL_EMPTY);
        }

        LambdaUpdateWrapper<PurchaseEntity> updateChainWrapper = new LambdaUpdateWrapper<>(PurchaseEntity.class);
        updateChainWrapper.set(PurchaseEntity::getStatus, PurchaseStatusEnum.RECEIVE.getCode())
                .set(PurchaseEntity::getUpdateTime, new Date())
                .in(PurchaseEntity::getId, receivable);
        baseMapper.update(updateChainWrapper);

        LambdaUpdateWrapper<PurchaseDetailEntity> updateWrapper = new LambdaUpdateWrapper<>(PurchaseDetailEntity.class);
        updateWrapper.set(PurchaseDetailEntity::getStatus, PurchaseDetailEnum.BUYING.getCode())
                .in(PurchaseDetailEntity::getPurchaseId, receivable)
                .in(PurchaseDetailEntity::getStatus, PurchaseDetailEnum.ASSIGNED.getCode());
        purchaseDetailDao.update(updateWrapper);
    }

    /**
     * 完成采购单：逐条记结果，全成功则单"已完成"，否则"有异常"，成功的那些入库。
     *
     * <p>只有"已领取"的单能完成，否则等于跳过"正在采购"这一段；明细必须属于这张单，
     * 否则可以拿别的单的明细 id 来改。</p>
     */
    @Override
    @Transactional
    public void done(PurchaseDoneVO purchaseDoneVO) {
        Long purchaseId = purchaseDoneVO.getId();
        if (purchaseId == null) {
            throw new ValidationException("id", "请先选择要完成的采购单");
        }

        PurchaseEntity purchaseEntity = baseMapper.selectById(purchaseId);
        if (purchaseEntity == null) {
            throw new BaseException(BaseCodeEnum.PURCHASE_NOT_FOUND);
        }
        if (!PurchaseStatusEnum.canDone(purchaseEntity.getStatus())) {
            throw new BaseException(BaseCodeEnum.PURCHASE_STATUS_INVALID);
        }

        List<PurchaseDoneVO.PurchaseItemVO> items = purchaseDoneVO.getItems() == null
                ? List.of()
                : purchaseDoneVO.getItems();
        if (items.isEmpty()) {
            throw new ValidationException("items", "请填写每一条采购需求单的结果");
        }

        List<Long> itemIds = items.stream()
                .map(PurchaseDoneVO.PurchaseItemVO::getItemId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (itemIds.size() != items.size()) {
            throw new ValidationException("items", "采购需求单 id 不能为空或重复");
        }

        Map<Long, PurchaseDetailEntity> detailMap = purchaseDetailDao.selectByIds(itemIds).stream()
                .collect(Collectors.toMap(PurchaseDetailEntity::getId, detail -> detail));
        boolean allBelongToThisPurchase = detailMap.size() == itemIds.size()
                && detailMap.values().stream()
                .allMatch(detail -> Objects.equals(detail.getPurchaseId(), purchaseId));
        if (!allBelongToThisPurchase) {
            throw new BaseException(BaseCodeEnum.PURCHASE_DETAIL_NOT_FOUND);
        }

        boolean allFinished = true;
        List<PurchaseDetailEntity> finishedDetails = new ArrayList<>();
        for (PurchaseDoneVO.PurchaseItemVO item : items) {
            // 只接受"已完成"和"采购失败"两种结果，别的一律不认
            if (!PurchaseDetailEnum.isFinalResult(item.getStatus())) {
                throw new BaseException(BaseCodeEnum.PURCHASE_DETAIL_STATUS_INVALID);
            }

            PurchaseDetailEntity detail = detailMap.get(item.getItemId());
            detail.setStatus(item.getStatus());
            if (Objects.equals(item.getStatus(), PurchaseDetailEnum.HASERROR.getCode())) {
                allFinished = false;
            } else {
                finishedDetails.add(detail);
            }
        }

        purchaseDetailDao.updateById(new ArrayList<>(detailMap.values()));

        PurchaseEntity update = new PurchaseEntity();
        update.setId(purchaseId);
        update.setStatus(allFinished ? PurchaseStatusEnum.FINISH.getCode() : PurchaseStatusEnum.HASERROR.getCode());
        update.setUpdateTime(new Date());
        baseMapper.updateById(update);

        // 只有采购成功的行才入库。原来这里筛的是 HASERROR，等于"失败才加库存"，正好反了
        finishedDetails.forEach(detail ->
                wareSkuService.addStock(detail.getSkuId(), detail.getWareId(), detail.getSkuNum()));
    }

}
