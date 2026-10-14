package member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;
import member.dao.GrowthChangeHistoryDao;
import member.entity.GrowthChangeHistoryEntity;
import member.enums.GrowthChangeTypeEnum;
import member.service.GrowthChangeHistoryService;
import member.service.MemberService;
import member.vo.GrowthHistoryAdminVo;
import member.vo.GrowthHistoryVo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import common.query.PageQuery;
/**
 * 会员成长值变动流水的查询实现。
 *
 * <p>无状态、线程安全，只读不写：流水行的写入方是 {@code GrowthAccountService}。
 */
@Service("growthChangeHistoryService")
public class GrowthChangeHistoryServiceImpl extends ServiceImpl<GrowthChangeHistoryDao, GrowthChangeHistoryEntity> implements GrowthChangeHistoryService {

    /** 会员昵称查询，只用于给台账补展示用的名字。 */
    private final MemberService memberService;

    /**
     * 构造服务，依赖由容器注入，创建后可直接使用。
     *
     * @param memberService 会员业务服务，用于批量取昵称，不能为 {@code null}
     */
    public GrowthChangeHistoryServiceImpl(MemberService memberService) {
        this.memberService = memberService;
    }

    /** {@inheritDoc} */
    @Override
    public PageVO<GrowthHistoryAdminVo> queryPage(PageQuery query) {
        // 按 create_time 排序时同秒的行次序不定，再按主键兜一层，翻页才不会漏行或重复
        IPage<GrowthChangeHistoryEntity> page = this.page(query.toPage(),
                new LambdaQueryWrapper<GrowthChangeHistoryEntity>()
                        .orderByDesc(GrowthChangeHistoryEntity::getCreateTime)
                        .orderByDesc(GrowthChangeHistoryEntity::getId));

        List<GrowthChangeHistoryEntity> records = page.getRecords();
        if (records.isEmpty()) {
            return new PageVO<>(page.getTotal(), List.of());
        }

        // 昵称一次查完再映射：逐行查会变成"台账有几行就发几次查询"
        Map<Long, String> nicknames = memberService.getMemberNames(records.stream()
                .map(GrowthChangeHistoryEntity::getMemberId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());

        return new PageVO<>(page.getTotal(), records.stream().map(history -> {
            GrowthHistoryAdminVo vo = new GrowthHistoryAdminVo();
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
    public PageVO<GrowthHistoryVo> queryMine(Long memberId, PageQuery query) {
        // 按 create_time 排序时同秒的行次序不定，再按主键兜一层，翻页才不会漏行或重复
        IPage<GrowthChangeHistoryEntity> page = this.page(query.toPage(),
                new LambdaQueryWrapper<GrowthChangeHistoryEntity>()
                        .eq(GrowthChangeHistoryEntity::getMemberId, memberId)
                        .orderByDesc(GrowthChangeHistoryEntity::getCreateTime)
                        .orderByDesc(GrowthChangeHistoryEntity::getId));

        return new PageVO<>(page.getTotal(), page.getRecords().stream().map(history -> {
            GrowthHistoryVo vo = new GrowthHistoryVo();
            vo.setId(history.getId());
            vo.setCreateTime(history.getCreateTime());
            vo.setChangeCount(history.getChangeCount());
            vo.setChangeType(history.getChangeType());
            vo.setChangeTypeText(textOf(history.getChangeType()));
            vo.setOrderSn(history.getOrderSn());
            vo.setNote(history.getNote());
            return vo;
        }).toList());
    }

    /**
     * 把变动类型取值翻成文案。
     *
     * @param changeType 变动类型取值，可以为 {@code null}
     * @return 类型文案；取值不属于已知类型时为 {@code null}，由前端按"其他变动"显示
     */
    private String textOf(Integer changeType) {
        GrowthChangeTypeEnum type = GrowthChangeTypeEnum.of(changeType);
        return type == null ? null : type.getMsg();
    }

}
