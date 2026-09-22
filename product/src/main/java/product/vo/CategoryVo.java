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
 * 三个层级复用同一个类：
 * 一级分类的 children 是二级分类，二级分类的 children 是三级分类，三级分类的 children 为空列表。
 *
 * 之所以不再用 Map<一级id, List<二级>> 那种结构：
 * 1、Map 的 key 只能携带一级分类的 id，一级分类的 name/icon 会丢失，前端必须额外再请求一次才能拿到；
 * 2、Map（HashMap）不保证顺序，前端 v-for 渲染时菜单顺序会漂移。
 * 改成有序数组 + 嵌套 children 之后，一次请求就拿到完整且有序的菜单。
 *
 * 必须实现 Serializable：spring.cache.type=redis 且没有自定义序列化器，@Cacheable 的缓存值走 JDK 序列化。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 分类id */
    private Long catId;

    /** 分类名称 */
    private String name;

    /**
     * Element UI 图标类名，如 el-icon-goods —— 不是图片地址。
     * 后台分类树三个层级都会渲染它；这个接口只是把值透传给商城前台，前台目前不使用。
     */
    private String icon;

    /** 子分类，三级分类为空列表 */
    private List<CategoryVo> children;
}
