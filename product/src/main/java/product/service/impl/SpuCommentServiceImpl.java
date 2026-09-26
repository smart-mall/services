package product.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;
import org.springframework.stereotype.Service;
import product.dao.SpuCommentDao;
import product.entity.SpuCommentEntity;
import product.service.SpuCommentService;



import common.query.PageQuery;
/**
 * 商品评价服务的默认实现，基于 MyBatis-Plus 的 {@code ServiceImpl} 读写 {@code pms_spu_comment}。
 *
 * <p>本模块内没有业务逻辑读写这张表，本类只实现分页查询，增删改由继承的 {@code IService} 提供。
 */
@Service("spuCommentService")
public class SpuCommentServiceImpl extends ServiceImpl<SpuCommentDao, SpuCommentEntity> implements SpuCommentService {

    /** {@inheritDoc} */
    @Override
    public PageVO<SpuCommentEntity> queryPage(PageQuery query) {
        IPage<SpuCommentEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}