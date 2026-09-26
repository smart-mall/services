package product.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;
import org.springframework.stereotype.Service;
import product.dao.SpuImagesDao;
import product.entity.SpuImagesEntity;
import product.service.SpuImagesService;



import common.query.PageQuery;
/**
 * spu 图片服务的默认实现，基于 MyBatis-Plus 的 {@code ServiceImpl} 读写 {@code pms_spu_images}。
 *
 * <p>新增商品时会连带写入图集；本类只实现分页查询，增删改由继承的 {@code IService} 提供。
 */
@Service("spuImagesService")
public class SpuImagesServiceImpl extends ServiceImpl<SpuImagesDao, SpuImagesEntity> implements SpuImagesService {

    /** {@inheritDoc} */
    @Override
    public PageVO<SpuImagesEntity> queryPage(PageQuery query) {
        IPage<SpuImagesEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}