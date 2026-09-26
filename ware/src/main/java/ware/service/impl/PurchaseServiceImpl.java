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
/**
 * 采购单服务的默认实现，基于 MyBatis-Plus 的 {@code ServiceImpl} 读写 {@code wms_purchase}。
 *
 * <p>写操作都在事务内完成，状态守卫复用 {@link PurchaseStatusEnum} 与 {@link PurchaseDetailEnum}；
 * 采购单的总金额与仓库由 {@link #refreshPurchaseTotal(Long)} 按明细重算，不接受前端传值。
 */
@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {

    /** 没传优先级时新建采购单的默认值。 */
    private static final int DEFAULT_PRIORITY = 1;

    private final PurchaseDetailDao purchaseDetailDao;
    private final WareSkuService wareSkuService;
    private final WareInfoService wareInfoService;

    /**
     * 由容器注入明细 Mapper 与两个协作服务构造。
     *
     * @param purchaseDetailDao 采购需求单 Mapper，合并、取消分配与提交完成时直接读写明细
     * @param wareSkuService 库存服务，采购成功后按明细增加库存
     * @param wareInfoService 仓库服务，列表查询时补齐仓库名
     */
    public PurchaseServiceImpl(PurchaseDetailDao purchaseDetailDao, WareSkuService wareSkuService, WareInfoService wareInfoService) {
        this.purchaseDetailDao = purchaseDetailDao;
        this.wareSkuService = wareSkuService;
        this.wareInfoService = wareInfoService;
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
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
     * {@inheritDoc}
     *
     * <p>并入已有采购单时还会校验该单现有明细与本批明细同仓库 —— 否则并进来就破坏了"一张单一个仓库"。
     */
    @Override
    @Transactional
    public void merge(MergeVO mergeVO) {
        // 1. 校验需求单：去重后不能为空、必须都存在、必须还没并入任何单
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

        // 2. 必须同仓库：一张采购单只对应一个仓库，它的 ware_id 就是从这里取的
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

        // 3. 定位目标采购单：没指定就新建一张，指定了就校验状态与仓库、按需改优先级
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

        // 4. 明细挂到采购单上，再重算这张单的总金额与仓库
        LambdaUpdateWrapper<PurchaseDetailEntity> updateChainWrapper = new LambdaUpdateWrapper<>(PurchaseDetailEntity.class);
        updateChainWrapper.set(PurchaseDetailEntity::getStatus, PurchaseDetailEnum.ASSIGNED.getCode())
                .set(PurchaseDetailEntity::getPurchaseId, purchaseId)
                .in(PurchaseDetailEntity::getId, items);
        purchaseDetailDao.update(updateChainWrapper);

        refreshPurchaseTotal(purchaseId);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void unassign(List<Long> itemIds) {
        // 1. 校验需求单：去重后不能为空、必须都存在、必须处于"已分配"
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

        // 2. 所属采购单必须都还没被领取，否则采购员手上的单会和系统里的对不上
        List<Long> purchaseIds = details.stream().map(PurchaseDetailEntity::getPurchaseId).distinct().toList();
        List<PurchaseEntity> purchases = baseMapper.selectByIds(purchaseIds);
        boolean allOpen = purchases.size() == purchaseIds.size()
                && purchases.stream().allMatch(purchase -> PurchaseStatusEnum.isOpen(purchase.getStatus()));
        if (!allOpen) {
            throw new BaseException(BaseCodeEnum.PURCHASE_STATUS_INVALID);
        }

        // 3. 摘出来退回"新建"，再重算受影响采购单的总金额与仓库。
        // purchase_id 要显式置成 null：set(column, null) 走 #{} 占位符，生成的就是 SET purchase_id = NULL
        LambdaUpdateWrapper<PurchaseDetailEntity> updateWrapper = new LambdaUpdateWrapper<>(PurchaseDetailEntity.class);
        updateWrapper.set(PurchaseDetailEntity::getStatus, PurchaseDetailEnum.CREATED.getCode())
                .set(PurchaseDetailEntity::getPurchaseId, null)
                .in(PurchaseDetailEntity::getId, distinctIds);
        purchaseDetailDao.update(updateWrapper);

        purchaseIds.forEach(this::refreshPurchaseTotal);
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void removePurchase(List<Long> ids) {
        // 1. 校验采购单：去重后不能为空、必须都存在
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

        // 2. 已领取的是在途，不能删
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

        // 3. 终态的单：它下面的需求必须也走完了，然后跟着一起删
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

        // 4. 还没开始采购的单：需求退回"新建"，purchase_id 同样显式置成 null
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

        // 5. 终态单的需求跟着单一起删
        if (!detailsOfFinal.isEmpty()) {
            purchaseDetailDao.deleteByIds(detailsOfFinal.stream().map(PurchaseDetailEntity::getId).toList());
        }

        this.removeByIds(distinctIds);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void receive(Long currentAdminId, List<Long> ids) {
        // 1. 校验采购单：去重后不能为空、必须都存在、必须处于"已分配"
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

        // 2. 分配给谁就该谁领：别人（包括分配的人自己）不能替他把单领走
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

        // 3. 单下必须有明细，否则会卡在"已领取"且永远完不成
        List<PurchaseDetailEntity> details = purchaseDetailDao.selectList(new LambdaQueryWrapper<PurchaseDetailEntity>()
                .in(PurchaseDetailEntity::getPurchaseId, receivableIds));
        Set<Long> purchaseIdsWithDetail = details.stream()
                .map(PurchaseDetailEntity::getPurchaseId)
                .collect(Collectors.toSet());
        if (!purchaseIdsWithDetail.containsAll(receivableIds)) {
            throw new BaseException(BaseCodeEnum.PURCHASE_DETAIL_EMPTY);
        }

        // 4. 单推进到"已领取"；明细只动还是"已分配"的那些，避免把已完成或失败的明细拉回采购中
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

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void done(PurchaseDoneVO purchaseDoneVO) {
        // 1. 校验采购单与明细：单必须是"已领取"，明细不能为空、不能重复、必须属于这张单
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

        // 2. 逐条记结果：采购成功的进待入库列表，任一"采购失败"就把单落到"有异常"
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

        // 3. 明细结果与单状态落库
        purchaseDetailDao.updateById(new ArrayList<>(detailMap.values()));

        PurchaseEntity update = new PurchaseEntity();
        update.setId(purchaseId);
        update.setStatus(allFinished ? PurchaseStatusEnum.FINISH.getCode() : PurchaseStatusEnum.HASERROR.getCode());
        update.setUpdateTime(new Date());
        baseMapper.updateById(update);

        // 4. 只有采购成功的明细才入库，采购失败的明细不入库
        finishedDetails.forEach(detail ->
                wareSkuService.addStock(detail.getSkuId(), detail.getWareId(), detail.getSkuNum()));
    }

    /**
     * 按明细重算采购单的总金额与仓库。
     *
     * <p>总金额 = 明细 {@code sku_price} 之和 —— 该字段是"这条需求的采购金额（总额）"，不是单价。
     * 明细被摘空时把仓库也清掉，免得留一个指向不存在明细的仓库。
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
