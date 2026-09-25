package member.controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import common.to.LoginLogTo;
import common.utils.LoginUserUtils;
import common.vo.MemberResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import member.entity.MemberLoginLogEntity;
import member.service.MemberLoginLogService;
import common.utils.PageUtils;
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
    public R record(@RequestBody LoginLogTo to) {
        if (to == null || to.getMemberId() == null) {
            return R.error("缺少 memberId");
        }
        memberLoginLogService.record(to);

        return R.ok();
    }

    /**
     * 当前登录会员自己的登录记录，按时间倒序。参数 {@code pageNum / pageSize}。
     *
     * <p>会员 id 取自网关注入的 {@code X-Member-Claims}，不接受前端传 ——
     * 传参的话就成了"读任意会员的登录 IP"。</p>
     */
    @GetMapping("/mine")
    public R mine(@RequestParam Map<String, Object> params,
                  HttpServletRequest request,
                  HttpServletResponse response) throws IOException {
        MemberResponseVo user = LoginUserUtils.currentUser(request);
        if (user == null || user.getId() == null) {
            LoginUserUtils.writeUnauthorized(response);
            return null;
        }

        return R.ok().setData(memberLoginLogService.queryMine(user.getId(), params));
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = memberLoginLogService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
		MemberLoginLogEntity memberLoginLog = memberLoginLogService.getById(id);

        return R.ok().put("memberLoginLog", memberLoginLog);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody MemberLoginLogEntity memberLoginLog){
		memberLoginLogService.save(memberLoginLog);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody MemberLoginLogEntity memberLoginLog){
		memberLoginLogService.updateById(memberLoginLog);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids){
		memberLoginLogService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
