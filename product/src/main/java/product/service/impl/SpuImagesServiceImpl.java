package product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;
import org.springframework.stereotype.Service;
import product.dao.SpuImagesDao;
import product.entity.SpuImagesEntity;
import product.service.SpuImagesService;

import java.util.Map;


import common.query.PageQuery;
@Service("spuImagesService")
public class SpuImagesServiceImpl extends ServiceImpl<SpuImagesDao, SpuImagesEntity> implements SpuImagesService {

    @Override
    public PageVO<SpuImagesEntity> queryPage(PageQuery query) {
        IPage<SpuImagesEntity> page = this.page(
                query.toPage(),
                new QueryWrapper<SpuImagesEntity>()
        );

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}