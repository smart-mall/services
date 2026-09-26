package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import product.entity.SpuCommentEntity;

import java.util.Map;

import common.query.PageQuery;
/**
 * 商品评价
 */
public interface SpuCommentService extends IService<SpuCommentEntity> {

    PageVO<SpuCommentEntity> queryPage(PageQuery query);
}

