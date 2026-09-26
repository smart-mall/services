package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import product.entity.CommentReplayEntity;

import java.util.Map;

import common.query.PageQuery;
/**
 * 商品评价回复关系
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 09:58:33
 */
public interface CommentReplayService extends IService<CommentReplayEntity> {

    PageVO<CommentReplayEntity> queryPage(PageQuery query);
}

