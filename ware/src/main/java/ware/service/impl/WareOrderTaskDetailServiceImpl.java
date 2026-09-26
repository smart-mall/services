package ware.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;

import ware.dao.WareOrderTaskDetailDao;
import ware.entity.WareOrderTaskDetailEntity;
import ware.service.WareOrderTaskDetailService;


import common.query.PageQuery;
@Service("wareOrderTaskDetailService")
public class WareOrderTaskDetailServiceImpl extends ServiceImpl<WareOrderTaskDetailDao, WareOrderTaskDetailEntity> implements WareOrderTaskDetailService {

    @Override
    public PageVO<WareOrderTaskDetailEntity> queryPage(PageQuery query) {
        IPage<WareOrderTaskDetailEntity> page = this.page(
                query.toPage(),
                new QueryWrapper<WareOrderTaskDetailEntity>()
        );

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}