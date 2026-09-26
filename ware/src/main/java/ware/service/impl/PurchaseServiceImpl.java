package ware.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.exception.ValidationException;
import common.vo.PageVO;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


import ware.vo.PurchasePageQuery;
import common.query.PageQuery;
@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {

    /** 没传优先级时新建采购单的默认值 */
    private static final int DEFAULT_PRIORITY = 1;

    private final PurchaseDetailDao purchaseDetailDao;
    private final WareSkuService wareSkuService;
    private final WareInfoService wareInfoService;

    public PurchaseServiceImpl(PurchaseDetailDao purchaseDetailDao, WareSkuService wareSkuService, WareInfoService wareInfoService) {
        this.purchaseDetailDao = purchaseDetailDao;
        this.wareSkuService = wareSkuService;
        this.wareInfoService = wareInfoService;
    }

    @Override
    public PageVO<PurchaseEntity> queryPage(PurchasePageQuery query) {
        LambdaQueryWrapper<PurchaseEntity> queryWrapper = new LambdaQueryWrapper<>();

        String key = query.getKey();
        if (key != null && !key.isEmpty()) {
            queryWrapper.and(item -> item.eq(PurchaseEntity::getId, key)
                    .or().like(PurchaseEntity::getAssigneeName, key));
        }

        String status = query.getStatus();
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq(PurchaseEntity::getStatus, status);
        }

        IPage<PurchaseEntity> page = this.page(
                query.toPage(),
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

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    @Override
    public PageVO<PurchaseEntity> queryPageUnreceive(PageQuery query) {
        LambdaQueryWrapper<PurchaseEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(PurchaseEntity::getStatus, PurchaseStatusEnum.openCodes());

        IPage<PurchaseEntity> page = this.page(
                query.toPage(),
                queryWrapper
        );

        page.getRecords().forEach(item -> item.setAllowedActions(PurchaseStatusEnum.allowedActions(item.getStatus())));

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    /**
     * 合并采购需求单：没指定采购单就新建一张，指定了就并进去。
     *
     * <p>三边都要卡：采购单必须还没被领取、需求单必须还没并入任何单，
     * 并且<b>所有需求单必须是同一个仓库</b>（一张采购单只对应一个仓库，它的 wareId 就是从这个来的）。</p>
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

        List<PurchaseDetailEntity> details = purchaseDetailDao.selectByIds(items);
        if (details.size() != items.size()) {
            throw new BaseException(BaseCodeEnum.PURCHASE_DETAIL_NOT_FOUND);
        }

        boolean mergeable = details.stream().allMatch(detail ->
                detail.getPurchaseId() == null && PurchaseDetailEnum.isNew(detail.getStatus()));
        if (!mergeable) {
            throw new BaseException(BaseCodeEnum.PURCHASE_DETAIL_STATUS_INVALID);
        }

        // 一张采购单只对应一个仓库，所以选中的需求单必须同仓库
        Set<Long> wareIds = details.stream()
                .map(PurchaseDetailEntity::getWareId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (wareIds.size() != 1) {
            throw new BaseException(BaseCodeEnum.PURCHASE_WARE_MISMATCH);
        }
        Long wareId = wareIds.iterator().next();

        Integer priority = mergeVO.getPriority();
        if (priority != null && priority < 1) {
            throw new ValidationException("priority", "优先级要大于 0");
        }

        Long purchaseId = mergeVO.getPurchaseId();
        if (purchaseId == null) {
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            Date now = new Date();
            purchaseEntity.setStatus(PurchaseStatusEnum.CREATED.getCode());
            purchaseEntity.setWareId(wareId);
            purchaseEntity.setPriority(priority == null ? DEFAULT_PRIORITY : priority);
            purchaseEntity.setAmount(BigDecimal.ZERO);
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
            // 单上已有的明细也必须是同一个仓库，否则并进来就破坏了"一张单一个仓库"
            List<PurchaseDetailEntity> existing = purchaseDetailDao.selectList(
                    new LambdaQueryWrapper<PurchaseDetailEntity>()
                            .eq(PurchaseDetailEntity::getPurchaseId, purchaseId));
            boolean sameWare = existing.stream().allMatch(detail -> Objects.equals(detail.getWareId(), wareId));
            if (!sameWare) {
                throw new BaseException(BaseCodeEnum.PURCHASE_WARE_MISMATCH);
            }
            // 传了优先级就顺手改掉，不传保持原样
            if (priority != null) {
                LambdaUpdateWrapper<PurchaseEntity> priorityWrapper = new LambdaUpdateWrapper<>(PurchaseEntity.class);
                priorityWrapper.eq(PurchaseEntity::getId, purchaseId)
                        .set(PurchaseEntity::getPriority, priority)
                        .set(PurchaseEntity::getUpdateTime, new Date());
                baseMapper.update(priorityWrapper);
            }
        }

        LambdaUpdateWrapper<PurchaseDetailEntity> updateChainWrapper = new LambdaUpdateWrapper<>(PurchaseDetailEntity.class);
        updateChainWrapper.set(PurchaseDetailEntity::getStatus, PurchaseDetailEnum.ASSIGNED.getCode())
                .set(PurchaseDetailEntity::getPurchaseId, purchaseId)
                .in(PurchaseDetailEntity::getId, items);
        purchaseDetailDao.update(updateChainWrapper);

        refreshPurchaseTotal(purchaseId);
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

        // purchase_id 要显式置成 null：set(column, null) 走 #{} 占位符，生成的就是 SET purchase_id = NULL
        LambdaUpdateWrapper<PurchaseDetailEntity> updateWrapper = new LambdaUpdateWrapper<>(PurchaseDetailEntity.class);
        updateWrapper.set(PurchaseDetailEntity::getStatus, PurchaseDetailEnum.CREATED.getCode())
                .set(PurchaseDetailEntity::getPurchaseId, null)
                .in(PurchaseDetailEntity::getId, distinctIds);
        purchaseDetailDao.update(updateWrapper);

        purchaseIds.forEach(this::refreshPurchaseTotal);
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
     * 删除采购单。三种状态分开处理：
     *
     * <ul>
     *   <li><b>新建 / 已分配</b>：能删，单下的需求退回"新建" —— 这些东西还是要买的，
     *       只是这张单不要了，不能把需求也一起删了</li>
     *   <li><b>已完成 / 有异常</b>：能删，单下的需求一起删掉 —— 已经是历史了</li>
     *   <li><b>已领取</b>：不能删 —— 采购员正照着它在买，删了他手上的单就凭空消失了</li>
     * </ul>
     *
     * <p>注意删采购单<b>不会回滚库存</b>：终态单的货已经通过"完成采购"入到
     * {@code wms_ware_sku} 了，删的是单据，不是那批货。</p>
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

        // 已领取的是在途，不能删
        List<Long> receiving = purchases.stream()
                .filter(purchase -> Objects.equals(purchase.getStatus(), PurchaseStatusEnum.RECEIVE.getCode()))
                .map(PurchaseEntity::getId)
                .toList();
        if (!receiving.isEmpty()) {
            throw new BaseException(BaseCodeEnum.PURCHASE_STATUS_INVALID,
                    "采购单[" + receiving.stream().map(String::valueOf).collect(Collectors.joining(","))
                            + "]已被领取，正在采购中，不能删除");
        }

        boolean statusOk = purchases.stream().allMatch(purchase ->
                PurchaseStatusEnum.isOpen(purchase.getStatus()) || PurchaseStatusEnum.isFinal(purchase.getStatus()));
        if (!statusOk) {
            throw new BaseException(BaseCodeEnum.PURCHASE_STATUS_INVALID);
        }

        List<PurchaseDetailEntity> details = purchaseDetailDao.selectList(
                new LambdaQueryWrapper<PurchaseDetailEntity>()
                        .in(PurchaseDetailEntity::getPurchaseId, distinctIds));

        // 终态的单：它下面的需求必须也走完了，然后跟着一起删
        List<Long> finalPurchaseIds = purchases.stream()
                .filter(purchase -> PurchaseStatusEnum.isFinal(purchase.getStatus()))
                .map(PurchaseEntity::getId)
                .toList();
        List<PurchaseDetailEntity> detailsOfFinal = details.stream()
                .filter(detail -> finalPurchaseIds.contains(detail.getPurchaseId()))
                .toList();
        if (detailsOfFinal.stream().anyMatch(detail -> !PurchaseDetailEnum.isFinal(detail.getStatus()))) {
            throw new BaseException(BaseCodeEnum.PURCHASE_DETAIL_STATUS_INVALID,
                    "采购单已经是终态，但它下面还有没走完的采购需求，不能删除");
        }

        // 还没开始采购的单：需求退回"新建"（purchase_id 显式置 null，所以用 setSql）
        List<Long> openPurchaseIds = purchases.stream()
                .filter(purchase -> PurchaseStatusEnum.isOpen(purchase.getStatus()))
                .map(PurchaseEntity::getId)
                .toList();
        if (!openPurchaseIds.isEmpty()) {
            LambdaUpdateWrapper<PurchaseDetailEntity> backToNew = new LambdaUpdateWrapper<>(PurchaseDetailEntity.class);
            backToNew.set(PurchaseDetailEntity::getStatus, PurchaseDetailEnum.CREATED.getCode())
                    .set(PurchaseDetailEntity::getPurchaseId, null)
                    .in(PurchaseDetailEntity::getPurchaseId, openPurchaseIds);
            purchaseDetailDao.update(backToNew);
        }

        // 终态单的需求跟着单一起删
        if (!detailsOfFinal.isEmpty()) {
            purchaseDetailDao.deleteByIds(detailsOfFinal.stream().map(PurchaseDetailEntity::getId).toList());
        }

        this.removeByIds(distinctIds);
    }

    /**
     * 领取采购单：单从"已分配"变成"已领取"，单下明细一起进入"正在采购"。
     *
     * <p>三条卡口：必须已经分配采购员（新建的单直接领走等于跳过分配）、
     * 领取人必须是这张单分配的采购员、单下至少要有 1 条明细（否则会卡在"已领取"且永远完不成）。
     * 明细只动还是"已分配"的那些，避免把已经完成或失败的明细又拉回采购中。</p>
     */
    @Override
    @Transactional
    public void receive(Long currentAdminId, List<Long> ids) {
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

        List<PurchaseEntity> receivable = purchases.stream()
                .filter(purchase -> PurchaseStatusEnum.canReceive(purchase.getStatus()))
                .toList();
        if (receivable.isEmpty()) {
            throw new BaseException(BaseCodeEnum.PURCHASE_STATUS_INVALID);
        }

        // 分配给谁就该谁领：别人（包括分配的人自己）不能替他把单领走
        List<Long> notMine = receivable.stream()
                .filter(purchase -> !Objects.equals(purchase.getAssigneeId(), currentAdminId))
                .map(PurchaseEntity::getId)
                .toList();
        if (!notMine.isEmpty()) {
            throw new BaseException(BaseCodeEnum.PURCHASE_NOT_ASSIGNEE,
                    "采购单[" + notMine.stream().map(String::valueOf).collect(Collectors.joining(","))
                            + "]不是分配给当前登录用户的，不能领取");
        }

        List<Long> receivableIds = receivable.stream().map(PurchaseEntity::getId).toList();

        List<PurchaseDetailEntity> details = purchaseDetailDao.selectList(new LambdaQueryWrapper<PurchaseDetailEntity>()
                .in(PurchaseDetailEntity::getPurchaseId, receivableIds));
        Set<Long> purchaseIdsWithDetail = details.stream()
                .map(PurchaseDetailEntity::getPurchaseId)
                .collect(Collectors.toSet());
        if (!purchaseIdsWithDetail.containsAll(receivableIds)) {
            throw new BaseException(BaseCodeEnum.PURCHASE_DETAIL_EMPTY);
        }

        LambdaUpdateWrapper<PurchaseEntity> updateChainWrapper = new LambdaUpdateWrapper<>(PurchaseEntity.class);
        updateChainWrapper.set(PurchaseEntity::getStatus, PurchaseStatusEnum.RECEIVE.getCode())
                .set(PurchaseEntity::getUpdateTime, new Date())
                .in(PurchaseEntity::getId, receivableIds);
        baseMapper.update(updateChainWrapper);

        LambdaUpdateWrapper<PurchaseDetailEntity> updateWrapper = new LambdaUpdateWrapper<>(PurchaseDetailEntity.class);
        updateWrapper.set(PurchaseDetailEntity::getStatus, PurchaseDetailEnum.BUYING.getCode())
                .in(PurchaseDetailEntity::getPurchaseId, receivableIds)
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
            if (!PurchaseDetailEnum.isFinal(item.getStatus())) {
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

    /**
     * 明细变了（合并进来 / 取消分配摘出去）就重算采购单的总金额和仓库。
     *
     * <p>总金额 = 明细的 {@code sku_price} 之和 —— 那个字段是"这条需求的采购金额（总额）"，
     * 不是单价。明细被摘空时把仓库也清掉，免得留一个指向不存在明细的仓库。</p>
     */
    private void refreshPurchaseTotal(Long purchaseId) {
        List<PurchaseDetailEntity> details = purchaseDetailDao.selectList(
                new LambdaQueryWrapper<PurchaseDetailEntity>()
                        .eq(PurchaseDetailEntity::getPurchaseId, purchaseId));

        BigDecimal amount = details.stream()
                .map(PurchaseDetailEntity::getSkuPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LambdaUpdateWrapper<PurchaseEntity> updateWrapper = new LambdaUpdateWrapper<>(PurchaseEntity.class);
        updateWrapper.eq(PurchaseEntity::getId, purchaseId)
                .set(PurchaseEntity::getAmount, amount)
                .set(PurchaseEntity::getUpdateTime, new Date());
        if (details.isEmpty()) {
            updateWrapper.set(PurchaseEntity::getWareId, null);
        } else {
            updateWrapper.set(PurchaseEntity::getWareId, details.get(0).getWareId());
        }
        baseMapper.update(updateWrapper);
    }

}
