package product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 前台（首页、全局导航）使用的商品三级分类树节点。
 *
 * <p>三个层级复用同一个类：一级分类的 {@code children} 是二级分类，二级分类的 {@code children}
 * 是三级分类，三级分类的 {@code children} 为空列表。子分类必须是数组而不是以父分类 ID 为 key 的映射：
 * 数组元素自带分类名与图标，顺序也能固定；映射的 key 只能携带一级分类 ID，一级分类的 name/icon
 * 无处存放，且顺序不保证，前端菜单会漂移。
 *
 * <p>本类实例作为 {@code CategoryService#getCatalogTree} 的返回值写入 {@code category} 缓存，
 * 缓存的 value 由 {@code CacheConfig} 以 JSON 序列化到 Redis。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 分类 ID，指向 {@code pms_category.cat_id}。 */
    private Long catId;

    /** 分类名。 */
    private String name;

    /**
     * Element UI 图标类名，如 el-icon-goods —— 不是图片地址。
     * 后台分类树三个层级都会渲染它；这个接口只是把值透传给商城前台，前台目前不使用。
     */
    private String icon;

    /** 子分类；三级分类为空列表。 */
    private List<CategoryVo> children;
}
