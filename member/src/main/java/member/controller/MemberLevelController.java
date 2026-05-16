package member.controller;

import common.utils.PageUtils;
import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import member.entity.MemberLevelEntity;
import member.service.MemberLevelService;
import member.vo.MemberSelectVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;



/**
 * 会员等级
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:14:17
 */
@RestController
@RequestMapping("member/memberlevel")
@Slf4j
public class MemberLevelController {
    @Autowired
    private MemberLevelService memberLevelService;

    // 获取spu下拉框选择信息
    @GetMapping(value = "/getMemberSelect")
    public R getSpuSelect() {
        log.info("获取会员等级下拉框选择信息");
        List<MemberSelectVO> spuSelect = memberLevelService.getMemberSelect();

        return R.ok().setData(spuSelect);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = memberLevelService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
		MemberLevelEntity memberLevel = memberLevelService.getById(id);

        return R.ok().put("memberLevel", memberLevel);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody MemberLevelEntity memberLevel){
		memberLevelService.save(memberLevel);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody MemberLevelEntity memberLevel){
		memberLevelService.updateById(memberLevel);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids){
		memberLevelService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
