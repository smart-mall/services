package product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.constant.ProductConstant;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.exception.ValidationException;
import common.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import product.dao.ProductAttrValueDao;
import product.dao.SpuInfoDao;
import product.entity.ProductAttrValueEntity;
import product.entity.SpuInfoEntity;
import product.service.ProductAttrValueService;

import java.util.List;
import java.util.Objects;


import common.query.PageQuery;
/**
 * 商品规格参数值服务的默认实现，基于 MyBatis-Plus 的 {@code ServiceImpl} 读写
 * {@code pms_product_attr_value}。
 *
 * <p>改规格参数前要回查 spu 的发布状态：取值在上架时被快照进 Elasticsearch，改完索引不会跟着变。
 */
@Service("productAttrValueService")
public class ProductAttrValueServiceImpl extends ServiceImpl<ProductAttrValueDao, ProductAttrValueEntity> implements ProductAttrValueService {

    private final SpuInfoDao spuInfoDao;

    /**
     * 由容器注入 spu 主表 Mapper 构造。
     *
     * @param spuInfoDao spu 主表 Mapper，改规格参数前用它回查发布状态
     */
    public ProductAttrValueServiceImpl(SpuInfoDao spuInfoDao) {
        this.spuInfoDao = spuInfoDao;
    }

    /** {@inheritDoc} */
    @Override
    public PageVO<ProductAttrValueEntity> queryPage(PageQuery query) {
        IPage<ProductAttrValueEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    /** {@inheritDoc} */
    @Override
    public List<ProductAttrValueEntity> baseAttrListForSpu(Long spuId) {
        LambdaQueryWrapper<ProductAttrValueEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProductAttrValueEntity::getSpuId,spuId);

        return baseMapper.selectList(queryWrapper);
    }

    /**
     * {@inheritDoc}
     *
     * <p>删旧值与插新值在同一个事务里，中途失败不会留下"参数被删光"的中间状态。
     */
    @Override
    @Transactional
    public void updateSpuAttr(Long spuId, List<ProductAttrValueEntity> entities) {
        SpuInfoEntity spu = spuInfoDao.selectById(spuId);
        if (spu == null) {
            throw new ValidationException("spuId", "商品不存在");
        }
        // 规格参数在上架时被快照进 ES，这里改完 ES 不会跟着变，详情页与搜索会分叉
        if (Objects.equals(spu.getPublishStatus(), ProductConstant.ProductStatusEnum.UP.getCode())) {
            throw new BaseException(BaseCodeEnum.PRODUCT_UP_SHELVED_CANNOT_UPDATE);
        }

        LambdaQueryWrapper<ProductAttrValueEntity> eq = new LambdaQueryWrapper<>(ProductAttrValueEntity.class).eq(ProductAttrValueEntity::getSpuId, spuId);
        baseMapper.delete(eq);

        entities.forEach(entity -> {
            entity.setSpuId(spuId);
        });

        baseMapper.insert(entities);
    }

}
