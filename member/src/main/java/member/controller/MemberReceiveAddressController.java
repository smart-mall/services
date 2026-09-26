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
import java.util.Map;



import common.exception.BaseCodeEnum;
import common.query.PageQuery;
/**
 * 会员收货地址
 */
@RestController
@Slf4j
@RequestMapping("member/memberreceiveaddress")
public class MemberReceiveAddressController {
    @Autowired
    private MemberReceiveAddressService memberReceiveAddressService;

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
     * 根据会员id查询会员的所有地址
     * @param memberId
     * @return
     */
    @GetMapping(value = "/{memberId}/address")
    public List<MemberReceiveAddressEntity> getAddress(@PathVariable("memberId") Long memberId) {
        log.info("根据会员id查询会员的所有地址：{}", memberId);

        return memberReceiveAddressService.getAddress(memberId);
    }


    /**
     * 列表
     */
    @RequestMapping("/list")
    public R<PageVO<MemberReceiveAddressEntity>> list(PageQuery query){
        PageVO<MemberReceiveAddressEntity> page = memberReceiveAddressService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R<MemberReceiveAddressEntity> info(@PathVariable("id") Long id){
		MemberReceiveAddressEntity memberReceiveAddress = memberReceiveAddressService.getById(id);

        return R.ok(memberReceiveAddress);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody MemberReceiveAddressEntity memberReceiveAddress){
		memberReceiveAddressService.save(memberReceiveAddress);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody MemberReceiveAddressEntity memberReceiveAddress){
		memberReceiveAddressService.updateById(memberReceiveAddress);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		memberReceiveAddressService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
