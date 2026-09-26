package member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.vo.PageVO;
import member.dao.MemberReceiveAddressDao;
import member.entity.MemberReceiveAddressEntity;
import member.service.MemberReceiveAddressService;
import member.vo.AddressSaveVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


import common.query.PageQuery;
/**
 * 会员收货地址服务的实现：地址增删改查与默认地址维护。
 *
 * <p>无状态、线程安全；同一会员的默认地址唯一性由"先清后设"在同一事务内保证。
 */
@Service("memberReceiveAddressService")
public class MemberReceiveAddressServiceImpl extends ServiceImpl<MemberReceiveAddressDao, MemberReceiveAddressEntity> implements MemberReceiveAddressService {

    /** 每人的地址条数上限，防止单账号灌入过多地址。 */
    private static final int MAX_ADDRESS = 20;

    private static final int DEFAULT_YES = 1;
    private static final int DEFAULT_NO = 0;

    /** {@inheritDoc} */
    @Override
    public PageVO<MemberReceiveAddressEntity> queryPage(PageQuery query) {
        IPage<MemberReceiveAddressEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    /** {@inheritDoc} */
    @Override
    public List<MemberReceiveAddressEntity> getAddress(Long memberId) {

        List<MemberReceiveAddressEntity> addressList = this.baseMapper.selectList
                (new LambdaQueryWrapper<MemberReceiveAddressEntity>().eq(MemberReceiveAddressEntity::getMemberId, memberId));

        return addressList;
    }

    /** {@inheritDoc} */
    @Override
    public List<MemberReceiveAddressEntity> listMine(Long memberId) {
        // 必须带排序：默认地址排最前是地址页的展示契约；
        // 不排序的话 MySQL 不保证顺序，多条默认时默认地址会飘
        return this.list(new LambdaQueryWrapper<MemberReceiveAddressEntity>()
                .eq(MemberReceiveAddressEntity::getMemberId, memberId)
                .orderByDesc(MemberReceiveAddressEntity::getDefaultStatus)
                .orderByAsc(MemberReceiveAddressEntity::getId));
    }

    /** {@inheritDoc} */
    // 清旧默认与插入必须同一事务，否则中途失败会留下「默认标记已清、新地址没写进」
    @Transactional(rollbackFor = Exception.class)
    @Override
    public MemberReceiveAddressEntity create(Long memberId, AddressSaveVo vo) {

        long count = this.count(new LambdaQueryWrapper<MemberReceiveAddressEntity>()
                .eq(MemberReceiveAddressEntity::getMemberId, memberId));
        if (count >= MAX_ADDRESS) {
            throw new BaseException(BaseCodeEnum.ADDRESS_LIMIT_EXCEEDED);
        }

        MemberReceiveAddressEntity entity = new MemberReceiveAddressEntity();
        entity.setMemberId(memberId);
        applyFields(entity, vo);

        // 第一条地址必须是默认，否则结算页一条默认都取不到
        boolean makeDefault = count == 0 || Boolean.TRUE.equals(vo.getDefaultStatus());
        entity.setDefaultStatus(makeDefault ? DEFAULT_YES : DEFAULT_NO);

        if (makeDefault) {
            clearDefault(memberId);
        }
        this.save(entity);
        return entity;
    }

    /** {@inheritDoc} */
    // 清旧默认与更新本行必须同一事务，否则中途失败会留下没有默认地址的状态
    @Transactional(rollbackFor = Exception.class)
    @Override
    public MemberReceiveAddressEntity update(Long memberId, Long id, AddressSaveVo vo) {

        MemberReceiveAddressEntity entity = requireOwn(memberId, id);
        applyFields(entity, vo);

        if (Boolean.TRUE.equals(vo.getDefaultStatus())) {
            clearDefault(memberId);
            entity.setDefaultStatus(DEFAULT_YES);
        }

        // 用 set 而不是 updateById：updateById 默认忽略 null 字段，
        // 用户清空邮编/详细地址时会静默保留旧值
        this.update(new LambdaUpdateWrapper<MemberReceiveAddressEntity>()
                .eq(MemberReceiveAddressEntity::getId, entity.getId())
                .set(MemberReceiveAddressEntity::getName, entity.getName())
                .set(MemberReceiveAddressEntity::getPhone, entity.getPhone())
                .set(MemberReceiveAddressEntity::getPostCode, entity.getPostCode())
                .set(MemberReceiveAddressEntity::getProvince, entity.getProvince())
                .set(MemberReceiveAddressEntity::getCity, entity.getCity())
                .set(MemberReceiveAddressEntity::getRegion, entity.getRegion())
                .set(MemberReceiveAddressEntity::getDetailAddress, entity.getDetailAddress())
                .set(MemberReceiveAddressEntity::getAreacode, entity.getAreacode())
                .set(MemberReceiveAddressEntity::getDefaultStatus, entity.getDefaultStatus()));

        return entity;
    }

    /** {@inheritDoc} */
    // 删除与顺位提升必须同一事务，否则中途失败会让该会员一条默认地址都不剩
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Long memberId, Long id) {

        MemberReceiveAddressEntity entity = requireOwn(memberId, id);
        boolean wasDefault = Integer.valueOf(DEFAULT_YES).equals(entity.getDefaultStatus());

        this.removeById(id);

        if (wasDefault) {
            // 默认那条被删了要顺位提升，否则这个会员一条默认都不剩
            List<MemberReceiveAddressEntity> rest = listMine(memberId);
            if (!rest.isEmpty()) {
                setDefaultFlag(rest.get(0).getId());
            }
        }
    }

    /** {@inheritDoc} */
    // 清旧默认与置新默认必须同一事务，否则中途失败会一条默认都不剩
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void setDefault(Long memberId, Long id) {
        requireOwn(memberId, id);
        clearDefault(memberId);
        setDefaultFlag(id);
    }

    /**
     * 取地址并确认它属于这个会员。
     *
     * <p>不属于自己的按"不存在"报，不报"无权访问"。
     *
     * @param memberId 会员 ID，不能为 {@code null}
     * @param id 地址 ID，不能为 {@code null}
     * @return 归属校验通过的地址
     * @throws BaseException {@code ADDRESS_NOT_FOUND} 地址不存在或不属于该会员
     */
    private MemberReceiveAddressEntity requireOwn(Long memberId, Long id) {
        MemberReceiveAddressEntity entity = this.getOne(new LambdaQueryWrapper<MemberReceiveAddressEntity>()
                .eq(MemberReceiveAddressEntity::getId, id)
                .eq(MemberReceiveAddressEntity::getMemberId, memberId));
        if (entity == null) {
            // 复用订单域定义的 17004：语义一致，没必要再造一个同义码
            throw new BaseException(BaseCodeEnum.ADDRESS_NOT_FOUND);
        }
        return entity;
    }

    /**
     * 把入参字段搬到实体上，默认标记不在这里处理。
     *
     * @param entity 目标实体，不能为 {@code null}
     * @param vo 地址入参，不能为 {@code null}
     */
    private void applyFields(MemberReceiveAddressEntity entity, AddressSaveVo vo) {
        entity.setName(vo.getName());
        entity.setPhone(vo.getPhone());
        entity.setPostCode(vo.getPostCode());
        entity.setProvince(vo.getProvince());
        entity.setCity(vo.getCity());
        entity.setRegion(vo.getRegion());
        entity.setDetailAddress(vo.getDetailAddress());
        entity.setAreacode(vo.getAreacode());
    }

    /**
     * 清除该会员所有地址的默认标记。
     *
     * @param memberId 会员 ID，不能为 {@code null}
     */
    private void clearDefault(Long memberId) {
        this.update(new LambdaUpdateWrapper<MemberReceiveAddressEntity>()
                .eq(MemberReceiveAddressEntity::getMemberId, memberId)
                .set(MemberReceiveAddressEntity::getDefaultStatus, DEFAULT_NO));
    }

    /**
     * 把指定地址标记为默认。
     *
     * @param id 地址 ID，不能为 {@code null}
     */
    private void setDefaultFlag(Long id) {
        this.update(new LambdaUpdateWrapper<MemberReceiveAddressEntity>()
                .eq(MemberReceiveAddressEntity::getId, id)
                .set(MemberReceiveAddressEntity::getDefaultStatus, DEFAULT_YES));
    }

}
