package member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.utils.PageUtils;
import common.utils.Query;
import member.dao.MemberReceiveAddressDao;
import member.entity.MemberReceiveAddressEntity;
import member.service.MemberReceiveAddressService;
import member.vo.AddressSaveVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;


@Service("memberReceiveAddressService")
public class MemberReceiveAddressServiceImpl extends ServiceImpl<MemberReceiveAddressDao, MemberReceiveAddressEntity> implements MemberReceiveAddressService {

    /** 每人的地址条数上限，防无脑灌 */
    private static final int MAX_ADDRESS = 20;

    private static final int DEFAULT_YES = 1;
    private static final int DEFAULT_NO = 0;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberReceiveAddressEntity> page = this.page(
                new Query<MemberReceiveAddressEntity>().getPage(params),
                new QueryWrapper<MemberReceiveAddressEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<MemberReceiveAddressEntity> getAddress(Long memberId) {

        List<MemberReceiveAddressEntity> addressList = this.baseMapper.selectList
                (new QueryWrapper<MemberReceiveAddressEntity>().eq("member_id", memberId));

        return addressList;
    }

    @Override
    public List<MemberReceiveAddressEntity> listMine(Long memberId) {
        // 必须带排序：结算页取默认地址用的是 filter(defaultStatus == 1).findFirst()，
        // 不排序的话 MySQL 不保证顺序，多条默认时默认地址会飘
        return this.list(new LambdaQueryWrapper<MemberReceiveAddressEntity>()
                .eq(MemberReceiveAddressEntity::getMemberId, memberId)
                .orderByDesc(MemberReceiveAddressEntity::getDefaultStatus)
                .orderByAsc(MemberReceiveAddressEntity::getId));
    }

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

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void setDefault(Long memberId, Long id) {
        requireOwn(memberId, id);
        clearDefault(memberId);
        setDefaultFlag(id);
    }

    /** 取地址并确认它属于这个会员。不属于自己的按"不存在"报，不报"无权访问" */
    private MemberReceiveAddressEntity requireOwn(Long memberId, Long id) {
        MemberReceiveAddressEntity entity = this.getOne(new LambdaQueryWrapper<MemberReceiveAddressEntity>()
                .eq(MemberReceiveAddressEntity::getId, id)
                .eq(MemberReceiveAddressEntity::getMemberId, memberId));
        if (entity == null) {
            // 17004 是订单域定义的"收货地址不存在"，语义一致就直接复用，不再造一个同义码
            throw new BaseException(BaseCodeEnum.ADDRESS_NOT_FOUND);
        }
        return entity;
    }

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

    /** 把该会员所有地址的默认标记清掉 */
    private void clearDefault(Long memberId) {
        this.update(new LambdaUpdateWrapper<MemberReceiveAddressEntity>()
                .eq(MemberReceiveAddressEntity::getMemberId, memberId)
                .set(MemberReceiveAddressEntity::getDefaultStatus, DEFAULT_NO));
    }

    private void setDefaultFlag(Long id) {
        this.update(new LambdaUpdateWrapper<MemberReceiveAddressEntity>()
                .eq(MemberReceiveAddressEntity::getId, id)
                .set(MemberReceiveAddressEntity::getDefaultStatus, DEFAULT_YES));
    }

}
