package member.controller;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import member.entity.MemberCollectSubjectEntity;
import member.service.MemberCollectSubjectService;
import common.vo.PageVO;
import common.utils.R;



import common.query.PageQuery;
/**
 * 会员收藏专题活动的后台管理接口：分页列表、详情、新增、修改、删除。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权。
 */
@RestController
@RequestMapping("member/membercollectsubject")
public class MemberCollectSubjectController {
    @Autowired
    private MemberCollectSubjectService memberCollectSubjectService;

    /**
     * 分页查询会员收藏的专题活动。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为收藏记录列表
     */
    @RequestMapping("/list")
    public R<PageVO<MemberCollectSubjectEntity>> list(PageQuery query){
        PageVO<MemberCollectSubjectEntity> page = memberCollectSubjectService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询单条专题活动收藏记录。
     *
     * @param id 收藏记录主键
     * @return 收藏详情；id 不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<MemberCollectSubjectEntity> info(@PathVariable("id") Long id){
		MemberCollectSubjectEntity memberCollectSubject = memberCollectSubjectService.getById(id);

        return R.ok(memberCollectSubject);
    }

    /**
     * 新增一条专题活动收藏记录。
     *
     * @param memberCollectSubject 收藏内容，主键由数据库生成
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody MemberCollectSubjectEntity memberCollectSubject){
		memberCollectSubjectService.save(memberCollectSubject);

        return R.ok();
    }

    /**
     * 按主键修改一条专题活动收藏记录。
     *
     * @param memberCollectSubject 收藏内容，主键必填；为 {@code null} 的字段不参与更新
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody MemberCollectSubjectEntity memberCollectSubject){
		memberCollectSubjectService.updateById(memberCollectSubject);

        return R.ok();
    }

    /**
     * 按主键批量删除专题活动收藏记录。
     *
     * @param ids 待删除的收藏记录主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		memberCollectSubjectService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
