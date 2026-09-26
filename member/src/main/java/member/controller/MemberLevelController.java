package member.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import common.vo.PageVO;
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



import common.query.KeyPageQuery;
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
    public R<List<MemberSelectVO>> getSpuSelect() {
        log.info("获取会员等级下拉框选择信息");
        List<MemberSelectVO> spuSelect = memberLevelService.getMemberSelect();

        return R.ok(spuSelect);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R<PageVO<MemberLevelEntity>> list(KeyPageQuery query){
        log.info("列表: {}", JSON.toJSONString(query, SerializerFeature.PrettyFormat));
        PageVO<MemberLevelEntity> page = memberLevelService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R<MemberLevelEntity> info(@PathVariable("id") Long id){
        log.info("信息: {}", id);
		MemberLevelEntity memberLevel = memberLevelService.getById(id);

        return R.ok(memberLevel);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody MemberLevelEntity memberLevel){
        log.info("保存会员等级: {}", JSON.toJSONString(memberLevel, SerializerFeature.PrettyFormat));
		memberLevelService.save(memberLevel);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody MemberLevelEntity memberLevel){
        log.info("修改会员等级: {}", JSON.toJSONString(memberLevel, SerializerFeature.PrettyFormat));
		memberLevelService.updateById(memberLevel);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
        log.info("删除会员等级: {}", JSON.toJSONString(ids, SerializerFeature.PrettyFormat));
		memberLevelService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
