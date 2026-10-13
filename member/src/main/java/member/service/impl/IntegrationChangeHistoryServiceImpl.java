package member.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;

import member.dao.IntegrationChangeHistoryDao;
import member.entity.IntegrationChangeHistoryEntity;
import member.enums.IntegrationChangeTypeEnum;
import member.service.IntegrationChangeHistoryService;
import member.vo.IntegrationHistoryVo;


import common.query.PageQuery;
/**
 * 会员积分变动流水的查询实现。
 *
 * <p>无状态、线程安全，只读不写：流水行的写入方是 {@code IntegrationAccountService}。
 */
@Service("integrationChangeHistoryService")
public class IntegrationChangeHistoryServiceImpl extends ServiceImpl<IntegrationChangeHistoryDao, IntegrationChangeHistoryEntity> implements IntegrationChangeHistoryService {

    /** {@inheritDoc} */
    @Override
    public PageVO<IntegrationChangeHistoryEntity> queryPage(PageQuery query) {
        IPage<IntegrationChangeHistoryEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    /** {@inheritDoc} */
    @Override
    public PageVO<IntegrationHistoryVo> queryMine(Long memberId, PageQuery query) {
        // 按 create_time 排序时同秒的行次序不定，再按主键兜一层，翻页才不会漏行或重复
        IPage<IntegrationChangeHistoryEntity> page = this.page(query.toPage(),
                new LambdaQueryWrapper<IntegrationChangeHistoryEntity>()
                        .eq(IntegrationChangeHistoryEntity::getMemberId, memberId)
                        .orderByDesc(IntegrationChangeHistoryEntity::getCreateTime)
                        .orderByDesc(IntegrationChangeHistoryEntity::getId));

        return new PageVO<>(page.getTotal(), page.getRecords().stream().map(this::toHistoryVo).toList());
    }

    /**
     * 把流水行映射成会员端视图对象。
     *
     * @param history 流水行，不能为 {@code null}
     * @return 会员端流水行；变动类型不是已知取值时只留时间、数值与备注，不猜文案
     */
    private IntegrationHistoryVo toHistoryVo(IntegrationChangeHistoryEntity history) {
        IntegrationChangeTypeEnum type = IntegrationChangeTypeEnum.of(history.getChangeType());

        IntegrationHistoryVo vo = new IntegrationHistoryVo();
        vo.setId(history.getId());
        vo.setCreateTime(history.getCreateTime());
        vo.setChangeCount(history.getChangeCount());
        vo.setChangeType(history.getChangeType());
        vo.setChangeTypeText(type == null ? null : type.getMsg());
        vo.setOrderSn(history.getOrderSn());
        vo.setNote(history.getNote());
        return vo;
    }

}
