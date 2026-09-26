package member.controller;

import java.util.Arrays;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import common.exception.ValidationException;
import common.to.LoginLogTo;
import member.entity.MemberLoginLogEntity;
import member.service.MemberLoginLogService;
import common.vo.PageVO;
import common.utils.R;



/**
 * 会员登录记录
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:14:17
 */
@RestController
@RequestMapping("member/memberloginlog")
public class MemberLoginLogController {
    @Autowired
    private MemberLoginLogService memberLoginLogService;

    /**
     * 落一条登录记录，由 auth 在登录成功后调用。
     *
     * <p>服务间接口：调用方没有登录态，所以不看 {@code X-Member-Claims}，
     * memberId 由调用方给。</p>
     */
    @PostMapping("/record")
    public R<Void> record(@RequestBody LoginLogTo to) {
        if (to == null || to.getMemberId() == null) {
            throw new ValidationException("memberId", "不能为空");
        }
        memberLoginLogService.record(to);

        return R.ok();
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R<PageVO<MemberLoginLogEntity>> list(@RequestParam Map<String, Object> params){
        PageVO<MemberLoginLogEntity> page = memberLoginLogService.queryPage(params);

        return R.ok(page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R<MemberLoginLogEntity> info(@PathVariable("id") Long id){
		MemberLoginLogEntity memberLoginLog = memberLoginLogService.getById(id);

        return R.ok(memberLoginLog);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody MemberLoginLogEntity memberLoginLog){
		memberLoginLogService.save(memberLoginLog);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody MemberLoginLogEntity memberLoginLog){
		memberLoginLogService.updateById(memberLoginLog);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		memberLoginLogService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
