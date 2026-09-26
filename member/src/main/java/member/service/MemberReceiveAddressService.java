package member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import member.entity.MemberReceiveAddressEntity;
import member.vo.AddressSaveVo;

import java.util.List;
import java.util.Map;

import common.query.PageQuery;
/**
 * 会员收货地址
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:14:17
 */
public interface MemberReceiveAddressService extends IService<MemberReceiveAddressEntity> {

    PageVO<MemberReceiveAddressEntity> queryPage(PageQuery query);

    /**
     * 按会员查地址。给 order 的结算页和 seckill 的秒杀下单用（它们从登录态拿自己的 memberId）。
     *
     * <p>不做归属过滤：调用方负责传自己的 id。</p>
     */
    List<MemberReceiveAddressEntity> getAddress(Long memberId);

    /** 我的收货地址，默认地址排在最前。前台地址管理页用 */
    List<MemberReceiveAddressEntity> listMine(Long memberId);

    /** 新增。第一条地址强制成为默认 */
    MemberReceiveAddressEntity create(Long memberId, AddressSaveVo vo);

    /** 修改。id 必须属于该会员 */
    MemberReceiveAddressEntity update(Long memberId, Long id, AddressSaveVo vo);

    /** 删除。id 必须属于该会员；删掉的是默认那条时，把剩下最早的一条提为默认 */
    void delete(Long memberId, Long id);

    /** 设为默认。同一事务里把该会员其他地址清 0 */
    void setDefault(Long memberId, Long id);
}
