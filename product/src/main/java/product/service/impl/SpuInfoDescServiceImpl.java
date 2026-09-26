package product.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;
import org.springframework.stereotype.Service;
import product.dao.SpuInfoDescDao;
import product.entity.SpuInfoDescEntity;
import product.service.SpuInfoDescService;

import java.util.Map;


import common.query.PageQuery;
/**
 * spu 介绍服务的默认实现，基于 MyBatis-Plus 的 {@code ServiceImpl} 读写 {@code pms_spu_info_desc}。
 *
 * <p>描述图地址按逗号拼接存在一列里，拼接与拆分见 {@code SpuInfoServiceImpl}。
 */
@Service("spuInfoDescService")
public class SpuInfoDescServiceImpl extends ServiceImpl<SpuInfoDescDao, SpuInfoDescEntity> implements SpuInfoDescService {

    /** {@inheritDoc} */
    @Override
    public PageVO<SpuInfoDescEntity> queryPage(PageQuery query) {
        IPage<SpuInfoDescEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}