package member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import member.entity.MemberReceiveAddressEntity;
import member.vo.AddressSaveVo;

import java.util.List;

import common.query.PageQuery;
/**
 * 会员收货地址，提供地址查询与增删改，并维护每人唯一的那条默认地址。
 *
 * <p>所有按 ID 操作的方法都要求地址属于传入的会员，不属于时一律按"不存在"报错。
 */
public interface MemberReceiveAddressService extends IService<MemberReceiveAddressEntity> {

    /**
     * 分页查询全部收货地址。
     *
     * <p>管理端使用，不带筛选条件；实现方不保证行序。
     *
     * @param query 分页参数，不能为 {@code null}；{@code page} / {@code limit} 非法时取默认值并截断上限
     * @return 分页结果，无数据时 {@code rows} 为空列表、{@code total} 为 0，不返回 {@code null}
     */
    PageVO<MemberReceiveAddressEntity> queryPage(PageQuery query);

    /**
     * 按会员查地址。给 order 的结算页和 seckill 的秒杀下单用（它们从登录态拿自己的 memberId）。
     *
     * <p>不做归属过滤：调用方负责传自己的 id。实现方不保证行序。
     *
     * @param memberId 会员 ID，不能为 {@code null}
     * @return 该会员的全部地址；没有地址时返回空列表，不返回 {@code null}
     */
    List<MemberReceiveAddressEntity> getAddress(Long memberId);

    /**
     * 查询我的收货地址，默认地址排在最前。
     *
     * <p>前台地址管理页使用：默认标记倒序、ID 升序，默认地址固定排在最前。
     *
     * @param memberId 会员 ID，不能为 {@code null}
     * @return 地址列表，默认地址在前、其余按 ID 升序；没有地址时返回空列表，不返回 {@code null}
     */
    List<MemberReceiveAddressEntity> listMine(Long memberId);

    /**
     * 新增收货地址。
     *
     * <p>该会员的第一条地址强制成为默认；传入 {@code defaultStatus = true} 时同样设为默认。
     *
     * @param memberId 会员 ID，不能为 {@code null}
     * @param vo 地址内容，不能为 {@code null}；字段校验由调用方的 {@code @Valid} 完成
     * @return 落库后的地址，含自增 ID
     * @throws common.exception.BaseException {@code ADDRESS_LIMIT_EXCEEDED} 该会员地址数已达上限（20 条）
     */
    MemberReceiveAddressEntity create(Long memberId, AddressSaveVo vo);

    /**
     * 修改收货地址。
     *
     * <p>整体替换：{@code vo} 中为 {@code null} 的字段会被清空，而不是保留原值。
     *
     * @param memberId 会员 ID，不能为 {@code null}
     * @param id 地址 ID，必须属于该会员
     * @param vo 地址内容，不能为 {@code null}
     * @return 修改后的地址
     * @throws common.exception.BaseException {@code ADDRESS_NOT_FOUND} 地址不存在或不属于该会员
     */
    MemberReceiveAddressEntity update(Long memberId, Long id, AddressSaveVo vo);

    /**
     * 删除收货地址。
     *
     * <p>删掉的是默认那条时，把剩下 ID 最小的一条提为默认，保证该会员仍有默认地址。
     *
     * @param memberId 会员 ID，不能为 {@code null}
     * @param id 地址 ID，必须属于该会员
     * @throws common.exception.BaseException {@code ADDRESS_NOT_FOUND} 地址不存在或不属于该会员
     */
    void delete(Long memberId, Long id);

    /**
     * 把指定地址设为默认。
     *
     * <p>同一事务里把该会员其他地址的默认标记清 0，任一时刻只保留一条默认地址。
     *
     * @param memberId 会员 ID，不能为 {@code null}
     * @param id 地址 ID，必须属于该会员
     * @throws common.exception.BaseException {@code ADDRESS_NOT_FOUND} 地址不存在或不属于该会员
     */
    void setDefault(Long memberId, Long id);
}
