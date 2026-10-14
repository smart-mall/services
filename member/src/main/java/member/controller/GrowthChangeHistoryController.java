package member.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import member.entity.GrowthChangeHistoryEntity;
import member.service.GrowthChangeHistoryService;
import member.vo.GrowthHistoryAdminVo;
import common.vo.PageVO;
import common.utils.R;



import common.query.PageQuery;
/**
 * 会员成长值变化记录的后台查询接口：分页列表与详情。
 *
 * <p>只读，没有新增、修改与删除：流水行只由 {@code GrowthAccountService} 在成长值真正变动时写入，
 * 后台改一行或删一行都会让流水与 {@code ums_member.growth} 的累计值对不上。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权。
 */
@RestController
@RequestMapping("member/growthchangehistory")
public class GrowthChangeHistoryController {
    @Autowired
    private GrowthChangeHistoryService growthChangeHistoryService;

    /**
     * 分页查询会员成长值变化记录。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为变化记录列表
     */
    @RequestMapping("/list")
    public R<PageVO<GrowthHistoryAdminVo>> list(PageQuery query){
        PageVO<GrowthHistoryAdminVo> page = growthChangeHistoryService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询单条成长值变化记录。
     *
     * @param id 记录主键
     * @return 记录详情；id 不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<GrowthChangeHistoryEntity> info(@PathVariable("id") Long id){
		GrowthChangeHistoryEntity growthChangeHistory = growthChangeHistoryService.getById(id);

        return R.ok(growthChangeHistory);
    }

}
