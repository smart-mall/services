package member.controller;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import member.entity.MemberStatisticsInfoEntity;
import member.service.MemberStatisticsInfoService;
import common.vo.PageVO;
import common.utils.R;



import common.query.PageQuery;
/**
 * 会员统计信息的后台管理接口：分页列表、详情、新增、修改、删除。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权。
 */
@RestController
@RequestMapping("member/memberstatisticsinfo")
public class MemberStatisticsInfoController {
    @Autowired
    private MemberStatisticsInfoService memberStatisticsInfoService;

    /**
     * 分页查询会员统计信息。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为统计记录列表
     */
    @RequestMapping("/list")
    public R<PageVO<MemberStatisticsInfoEntity>> list(PageQuery query){
        PageVO<MemberStatisticsInfoEntity> page = memberStatisticsInfoService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询单条会员统计信息。
     *
     * @param id 统计记录主键
     * @return 统计详情；id 不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<MemberStatisticsInfoEntity> info(@PathVariable("id") Long id){
		MemberStatisticsInfoEntity memberStatisticsInfo = memberStatisticsInfoService.getById(id);

        return R.ok(memberStatisticsInfo);
    }

    /**
     * 新增一条会员统计信息。
     *
     * @param memberStatisticsInfo 统计内容，主键由数据库生成
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody MemberStatisticsInfoEntity memberStatisticsInfo){
		memberStatisticsInfoService.save(memberStatisticsInfo);

        return R.ok();
    }

    /**
     * 按主键修改一条会员统计信息。
     *
     * @param memberStatisticsInfo 统计内容，主键必填；为 {@code null} 的字段不参与更新
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody MemberStatisticsInfoEntity memberStatisticsInfo){
		memberStatisticsInfoService.updateById(memberStatisticsInfo);

        return R.ok();
    }

    /**
     * 按主键批量删除会员统计信息。
     *
     * @param ids 待删除的统计记录主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		memberStatisticsInfoService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
