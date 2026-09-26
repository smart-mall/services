package member.controller;

import common.vo.PageVO;
import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import member.entity.MemberReceiveAddressEntity;
import member.service.MemberReceiveAddressService;
import member.vo.MemberAddressVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;



import common.exception.BaseCodeEnum;
import common.query.PageQuery;
/**
 * 会员收货地址的后台管理接口：按会员查询地址，以及分页列表、详情、新增、修改、删除。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权；会员管理自己的地址走
 * {@link member.web.MemberFrontController}。
 */
@RestController
@Slf4j
@RequestMapping("member/memberreceiveaddress")
public class MemberReceiveAddressController {
    @Autowired
    private MemberReceiveAddressService memberReceiveAddressService;

    /**
     * 新增一条会员收货地址，按入参里的 {@code memberId} 落库，不做登录态校验。
     *
     * <p>落库时固定把 {@code defaultStatus} 置 1，因此经本接口新增的地址都是默认地址。
     *
     * @param memberAddressVo 地址入参，需带 {@code memberId}
     * @return 成功时原样回显入参，回显里的 {@code defaultStatus} 不反映落库时强制置 1 的结果；
     *         保存失败时返回 10000，{@code data} 为 {@code null}
     */
    @PostMapping("/addLocation")
    public R<MemberAddressVo> addLocation(@RequestBody MemberAddressVo memberAddressVo){
        MemberReceiveAddressEntity addressEntity = new MemberReceiveAddressEntity();
        BeanUtils.copyProperties(memberAddressVo, addressEntity);
        addressEntity.setDefaultStatus(1);
        boolean result = memberReceiveAddressService.save(addressEntity);
        if (result){
            return R.ok(memberAddressVo);
        } else {
            return R.error(BaseCodeEnum.UNKNOWN_EXCEPTION);
        }
    }

    /**
     * 查询指定会员的全部收货地址。
     *
     * <p>本接口直接返回地址数组，不套 {@code R} 外壳；不做归属校验，调用方负责传自己的 memberId。
     *
     * @param memberId 会员 ID
     * @return 该会员的全部收货地址；没有地址时返回空列表
     */
    @GetMapping(value = "/{memberId}/address")
    public List<MemberReceiveAddressEntity> getAddress(@PathVariable("memberId") Long memberId) {
        log.info("根据会员id查询会员的所有地址：{}", memberId);

        return memberReceiveAddressService.getAddress(memberId);
    }


    /**
     * 分页查询会员收货地址。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为地址列表
     */
    @RequestMapping("/list")
    public R<PageVO<MemberReceiveAddressEntity>> list(PageQuery query){
        PageVO<MemberReceiveAddressEntity> page = memberReceiveAddressService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询单条会员收货地址。
     *
     * @param id 地址主键
     * @return 地址详情；id 不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<MemberReceiveAddressEntity> info(@PathVariable("id") Long id){
		MemberReceiveAddressEntity memberReceiveAddress = memberReceiveAddressService.getById(id);

        return R.ok(memberReceiveAddress);
    }

    /**
     * 新增一条会员收货地址。
     *
     * @param memberReceiveAddress 地址内容，主键由数据库生成
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody MemberReceiveAddressEntity memberReceiveAddress){
		memberReceiveAddressService.save(memberReceiveAddress);

        return R.ok();
    }

    /**
     * 按主键修改一条会员收货地址。
     *
     * @param memberReceiveAddress 地址内容，主键必填；为 {@code null} 的字段不参与更新
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody MemberReceiveAddressEntity memberReceiveAddress){
		memberReceiveAddressService.updateById(memberReceiveAddress);

        return R.ok();
    }

    /**
     * 按主键批量删除会员收货地址。
     *
     * @param ids 待删除的地址主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		memberReceiveAddressService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
