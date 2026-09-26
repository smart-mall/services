package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import product.entity.BrandEntity;

import java.util.List;

import common.query.KeyPageQuery;
/**
 * 品牌服务：维护品牌主表，并同步品牌与分类关联表里冗余的品牌名。
 *
 * <p>品牌 logo 存在 MinIO，删除品牌时同步删除 logo 对象。
 */
public interface BrandService extends IService<BrandEntity> {

    /**
     * 按关键字分页查询品牌。
     *
     * @param query 分页与关键字条件，{@code key} 同时模糊匹配品牌名与品牌 ID，不能为 {@code null}
     * @return 分页结果，{@code rows} 为品牌列表；没有数据时 {@code rows} 为空列表
     */
    PageVO<BrandEntity> queryPage(KeyPageQuery query);

    /**
     * 修改品牌，并同步刷新品牌分类关联表里冗余的品牌名。
     *
     * <p>logo 被替换时先让 third-party 删除被替换的 logo 对象，删除失败则抛业务异常并回滚修改。
     *
     * @param brand 品牌内容，{@code brandId} 必填，不能为 {@code null}
     * @throws common.exception.BaseException 被替换的 logo 对象删除失败时抛出，修改整批回滚
     */
    void updateDetail(BrandEntity brand);

    /**
     * 按主键批量删除品牌。
     *
     * <p>品牌下还有商品时整批拒绝；品牌与分类的关联行、品牌 logo 对象跟着一起删。删除是幂等的，
     * 入参为 {@code null} 或查不到的 ID 会被静默跳过。
     *
     * @param list 待删除的品牌主键列表，允许为 {@code null}
     * @throws common.exception.BaseException 品牌下仍有商品，或 logo 对象删除失败时抛出，删除整批不生效
     */
    void deleteByIds(List<Long> list);
}

