package product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;
import org.springframework.stereotype.Service;
import product.dao.SpuInfoDescDao;
import product.entity.SpuInfoDescEntity;
import product.service.SpuInfoDescService;

import java.util.Map;


import common.query.PageQuery;
@Service("spuInfoDescService")
public class SpuInfoDescServiceImpl extends ServiceImpl<SpuInfoDescDao, SpuInfoDescEntity> implements SpuInfoDescService {

    @Override
    public PageVO<SpuInfoDescEntity> queryPage(PageQuery query) {
        IPage<SpuInfoDescEntity> page = this.page(
                query.toPage(),
                new QueryWrapper<SpuInfoDescEntity>()
        );

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}