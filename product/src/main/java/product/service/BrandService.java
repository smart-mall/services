package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import product.entity.BrandEntity;

import java.util.List;
import java.util.Map;

import common.query.KeyPageQuery;
/**
 * 品牌
 */
public interface BrandService extends IService<BrandEntity> {

    PageVO<BrandEntity> queryPage(KeyPageQuery query);

    void updateDetail(BrandEntity brand);

    void deleteByIds(List<Long> list);
}

