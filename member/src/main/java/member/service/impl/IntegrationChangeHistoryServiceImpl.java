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
import member.service.MemberService;
import member.vo.IntegrationHistoryAdminVo;
import member.vo.IntegrationHistoryPageQuery;
import member.vo.IntegrationHistoryVo;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import common.query.PageQuery;
/**
 * 会员积分变动流水的查询实现。
 *
 * <p>无状态、线程安全，只读不写：流水行的写入方是 {@code IntegrationAccountService}。
 */
@Service("integrationChangeHistoryService")
public class IntegrationChangeHistoryServiceImpl extends ServiceImpl<IntegrationChangeHistoryDao, IntegrationChangeHistoryEntity> implements IntegrationChangeHistoryService {

    /** 会员昵称查询，只用于给台账补展示用的名字。 */
    private final MemberService memberService;

    /**
     * 构造服务，依赖由容器注入，创建后可直接使用。
     *
     * @param memberService 会员业务服务，用于批量取昵称，不能为 {@code null}
     */
    public IntegrationChangeHistoryServiceImpl(MemberService memberService) {
        this.memberService = memberService;
    }

    /** {@inheritDoc} */
    @Override
    public PageVO<IntegrationHistoryAdminVo> queryPage(IntegrationHistoryPageQuery query) {
        // 按 create_time 排序时同秒的行次序不定，再按主键兜一层，翻页才不会漏行或重复
        IPage<IntegrationChangeHistoryEntity> page = this.page(query.toPage(),
                new LambdaQueryWrapper<IntegrationChangeHistoryEntity>()
                        .eq(query.getMemberId() != null, IntegrationChangeHistoryEntity::getMemberId, query.getMemberId())
                        .orderByDesc(IntegrationChangeHistoryEntity::getCreateTime)
                        .orderByDesc(IntegrationChangeHistoryEntity::getId));

        List<IntegrationChangeHistoryEntity> records = page.getRecords();
        if (records.isEmpty()) {
            return new PageVO<>(page.getTotal(), List.of());
        }

        // 昵称一次查完再映射：逐行查会变成"台账有几行就发几次查询"
        Map<Long, String> nicknames = memberService.getMemberNames(records.stream()
                .map(IntegrationChangeHistoryEntity::getMemberId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());

        return new PageVO<>(page.getTotal(), records.stream().map(history -> {
            IntegrationHistoryAdminVo vo = new IntegrationHistoryAdminVo();
            vo.setId(history.getId());
            vo.setMemberId(history.getMemberId());
            vo.setMemberNickname(nicknames.get(history.getMemberId()));
            vo.setCreateTime(history.getCreateTime());
            vo.setChangeCount(history.getChangeCount());
            vo.setChangeType(history.getChangeType());
            vo.setChangeTypeText(textOf(history.getChangeType()));
            vo.setOrderSn(history.getOrderSn());
            vo.setNote(history.getNote());
            return vo;
        }).toList());
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
     * 把变动类型取值翻成文案。
     *
     * @param changeType 变动类型取值，可以为 {@code null}
     * @return 类型文案；取值不属于已知类型时为 {@code null}，由前端按"其他变动"显示
     */
    private String textOf(Integer changeType) {
        IntegrationChangeTypeEnum type = IntegrationChangeTypeEnum.of(changeType);
        return type == null ? null : type.getMsg();
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
