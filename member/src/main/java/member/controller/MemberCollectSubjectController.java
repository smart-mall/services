package member.controller;

import java.util.Arrays;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import member.entity.MemberCollectSubjectEntity;
import member.service.MemberCollectSubjectService;
import common.vo.PageVO;
import common.utils.R;



import common.query.PageQuery;
/**
 * 会员收藏的专题活动
 */
@RestController
@RequestMapping("member/membercollectsubject")
public class MemberCollectSubjectController {
    @Autowired
    private MemberCollectSubjectService memberCollectSubjectService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R<PageVO<MemberCollectSubjectEntity>> list(PageQuery query){
        PageVO<MemberCollectSubjectEntity> page = memberCollectSubjectService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R<MemberCollectSubjectEntity> info(@PathVariable("id") Long id){
		MemberCollectSubjectEntity memberCollectSubject = memberCollectSubjectService.getById(id);

        return R.ok(memberCollectSubject);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody MemberCollectSubjectEntity memberCollectSubject){
		memberCollectSubjectService.save(memberCollectSubject);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody MemberCollectSubjectEntity memberCollectSubject){
		memberCollectSubjectService.updateById(memberCollectSubject);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		memberCollectSubjectService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
