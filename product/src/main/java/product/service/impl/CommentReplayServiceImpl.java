package product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;
import org.springframework.stereotype.Service;
import product.dao.CommentReplayDao;
import product.entity.CommentReplayEntity;
import product.service.CommentReplayService;

import java.util.Map;


import common.query.PageQuery;
@Service("commentReplayService")
public class CommentReplayServiceImpl extends ServiceImpl<CommentReplayDao, CommentReplayEntity> implements CommentReplayService {

    @Override
    public PageVO<CommentReplayEntity> queryPage(PageQuery query) {
        IPage<CommentReplayEntity> page = this.page(
                query.toPage(),
                new QueryWrapper<CommentReplayEntity>()
        );

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}