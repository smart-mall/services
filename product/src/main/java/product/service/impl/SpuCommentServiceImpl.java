package product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;
import org.springframework.stereotype.Service;
import product.dao.SpuCommentDao;
import product.entity.SpuCommentEntity;
import product.service.SpuCommentService;

import java.util.Map;


import common.query.PageQuery;
@Service("spuCommentService")
public class SpuCommentServiceImpl extends ServiceImpl<SpuCommentDao, SpuCommentEntity> implements SpuCommentService {

    @Override
    public PageVO<SpuCommentEntity> queryPage(PageQuery query) {
        IPage<SpuCommentEntity> page = this.page(
                query.toPage(),
                new QueryWrapper<SpuCommentEntity>()
        );

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}