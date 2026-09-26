package member.controller;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import common.exception.ValidationException;
import common.to.LoginLogTo;
import member.entity.MemberLoginLogEntity;
import member.service.MemberLoginLogService;
import common.vo.PageVO;
import common.utils.R;



import common.query.PageQuery;
/**
 * 会员登录记录的后台管理接口：登录记录写入，以及分页列表、详情、新增、修改、删除。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权；会员查看自己的记录走
 * {@link member.web.MemberFrontController}。
 */
@RestController
@RequestMapping("member/memberloginlog")
public class MemberLoginLogController {
    @Autowired
    private MemberLoginLogService memberLoginLogService;

    /**
     * 落一条登录记录，由 auth 在登录成功后调用。
     *
     * <p>服务间接口：调用方没有登录态，所以不读 {@code X-Member-Claims}，memberId 由调用方给；
     * 创建时间由服务层填充。
     *
     * @param to 登录记录，{@code memberId} 不能为空
     * @return 统一成功响应，不含业务数据
     * @throws ValidationException 当 {@code to} 为 {@code null} 或 {@code memberId} 为空时抛出
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
     * 分页查询会员登录记录。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为登录记录列表
     */
    @RequestMapping("/list")
    public R<PageVO<MemberLoginLogEntity>> list(PageQuery query){
        PageVO<MemberLoginLogEntity> page = memberLoginLogService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询单条会员登录记录。
     *
     * @param id 登录记录主键
     * @return 记录详情；id 不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<MemberLoginLogEntity> info(@PathVariable("id") Long id){
		MemberLoginLogEntity memberLoginLog = memberLoginLogService.getById(id);

        return R.ok(memberLoginLog);
    }

    /**
     * 新增一条会员登录记录。
     *
     * @param memberLoginLog 记录内容，主键由数据库生成
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody MemberLoginLogEntity memberLoginLog){
		memberLoginLogService.save(memberLoginLog);

        return R.ok();
    }

    /**
     * 按主键修改一条会员登录记录。
     *
     * @param memberLoginLog 记录内容，主键必填；为 {@code null} 的字段不参与更新
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody MemberLoginLogEntity memberLoginLog){
		memberLoginLogService.updateById(memberLoginLog);

        return R.ok();
    }

    /**
     * 按主键批量删除会员登录记录。
     *
     * @param ids 待删除的记录主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		memberLoginLogService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
