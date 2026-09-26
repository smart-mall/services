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
 * 会员等级的后台管理接口：下拉选项查询，以及分页列表、详情、新增、修改、删除。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权。
 */
@RestController
@RequestMapping("member/memberlevel")
@Slf4j
public class MemberLevelController {
    @Autowired
    private MemberLevelService memberLevelService;

    /**
     * 查询全部会员等级，供下拉框选择使用。
     *
     * @return 等级选项列表，每项只含 id 与 name；没有等级时返回空列表
     */
    @GetMapping(value = "/getMemberSelect")
    public R<List<MemberSelectVO>> getSpuSelect() {
        log.info("获取会员等级下拉框选择信息");
        List<MemberSelectVO> spuSelect = memberLevelService.getMemberSelect();

        return R.ok(spuSelect);
    }

    /**
     * 分页查询会员等级。
     *
     * @param query 分页与关键字参数，{@code key} 按等级名称或等级 id 模糊匹配
     * @return 分页结果，{@code rows} 为会员等级列表
     */
    @RequestMapping("/list")
    public R<PageVO<MemberLevelEntity>> list(KeyPageQuery query){
        log.info("列表: {}", JSON.toJSONString(query, SerializerFeature.PrettyFormat));
        PageVO<MemberLevelEntity> page = memberLevelService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询单个会员等级。
     *
     * @param id 会员等级主键
     * @return 等级详情；id 不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<MemberLevelEntity> info(@PathVariable("id") Long id){
        log.info("信息: {}", id);
		MemberLevelEntity memberLevel = memberLevelService.getById(id);

        return R.ok(memberLevel);
    }

    /**
     * 新增一个会员等级。
     *
     * @param memberLevel 等级内容，主键由数据库生成
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody MemberLevelEntity memberLevel){
        log.info("保存会员等级: {}", JSON.toJSONString(memberLevel, SerializerFeature.PrettyFormat));
		memberLevelService.save(memberLevel);

        return R.ok();
    }

    /**
     * 按主键修改一个会员等级。
     *
     * @param memberLevel 等级内容，主键必填；为 {@code null} 的字段不参与更新
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody MemberLevelEntity memberLevel){
        log.info("修改会员等级: {}", JSON.toJSONString(memberLevel, SerializerFeature.PrettyFormat));
		memberLevelService.updateById(memberLevel);

        return R.ok();
    }

    /**
     * 按主键批量删除会员等级。
     *
     * @param ids 待删除的等级主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
        log.info("删除会员等级: {}", JSON.toJSONString(ids, SerializerFeature.PrettyFormat));
		memberLevelService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
