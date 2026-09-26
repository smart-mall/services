package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import product.entity.SkuImagesEntity;

import java.util.List;

import common.query.PageQuery;
/**
 * sku 图片服务：维护 sku 图集。
 */
public interface SkuImagesService extends IService<SkuImagesEntity> {

    /**
     * 分页查询全部 sku 图片，不带筛选条件。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数，不能为 {@code null}
     * @return 分页结果，{@code rows} 为图片列表
     */
    PageVO<SkuImagesEntity> queryPage(PageQuery query);

    /**
     * 查询某个 sku 的图集。
     *
     * <p>{@code default_img} 为 1 的那一条是主图，与图集第一张通常是同一个地址；前端做缩略图列表
     * 时要展示全部图片，不要只展示默认图。
     *
     * @param skuId sku ID，不能为 {@code null}
     * @return 该 sku 的图片列表；没有图片时返回空列表
     */
    List<SkuImagesEntity> getImagesBySkuId(Long skuId);
}

