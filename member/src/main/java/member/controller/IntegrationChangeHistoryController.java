package member.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import member.entity.IntegrationChangeHistoryEntity;
import member.service.IntegrationChangeHistoryService;
import member.vo.IntegrationHistoryAdminVo;
import member.vo.IntegrationHistoryPageQuery;
import common.vo.PageVO;
import common.utils.R;

/**
 * 会员积分变化记录的后台查询接口：分页列表与详情。
 *
 * <p>只读，没有新增、修改与删除：流水行只由 {@code IntegrationAccountService} 在积分真正变动时写入，
 * 后台改一行或删一行都会让流水与 {@code ums_member.integration} 的余额对不上。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权。
 */
@RestController
@RequestMapping("member/integrationchangehistory")
public class IntegrationChangeHistoryController {
    @Autowired
    private IntegrationChangeHistoryService integrationChangeHistoryService;

    /**
     * 分页查询会员积分变化记录，每行带上会员昵称。
     *
     * @param query 分页与筛选条件，{@code memberId} 不传则返回全部会员的流水、传入则只看该会员
     * @return 分页结果，{@code rows} 为变化记录列表
     */
    @RequestMapping("/list")
    public R<PageVO<IntegrationHistoryAdminVo>> list(IntegrationHistoryPageQuery query){
        PageVO<IntegrationHistoryAdminVo> page = integrationChangeHistoryService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询单条积分变化记录。
     *
     * @param id 记录主键
     * @return 记录详情；id 不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<IntegrationChangeHistoryEntity> info(@PathVariable("id") Long id){
		IntegrationChangeHistoryEntity integrationChangeHistory = integrationChangeHistoryService.getById(id);

        return R.ok(integrationChangeHistory);
    }

}
