package product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.constant.ProductConstant;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.exception.ValidationException;
import common.vo.PageVO;
import common.utils.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import product.dao.ProductAttrValueDao;
import product.dao.SpuInfoDao;
import product.entity.ProductAttrValueEntity;
import product.entity.SpuInfoEntity;
import product.service.ProductAttrValueService;

import java.util.List;
import java.util.Map;
import java.util.Objects;


@Service("productAttrValueService")
public class ProductAttrValueServiceImpl extends ServiceImpl<ProductAttrValueDao, ProductAttrValueEntity> implements ProductAttrValueService {

    private final SpuInfoDao spuInfoDao;

    public ProductAttrValueServiceImpl(SpuInfoDao spuInfoDao) {
        this.spuInfoDao = spuInfoDao;
    }

    @Override
    public PageVO<ProductAttrValueEntity> queryPage(Map<String, Object> params) {
        IPage<ProductAttrValueEntity> page = this.page(
                new Query<ProductAttrValueEntity>().getPage(params),
                new QueryWrapper<>()
        );

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    @Override
    public List<ProductAttrValueEntity> baseAttrListForSpu(Long spuId) {
        LambdaQueryWrapper<ProductAttrValueEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProductAttrValueEntity::getSpuId,spuId);

        return baseMapper.selectList(queryWrapper);
    }

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
