package product.vo;

import lombok.Data;

/**
 * 商品属性新增与修改的入参。
 *
 * <p>是 {@code /product/attr/save} 与 {@code /product/attr/update} 的请求体：基本属性需带
 * {@code attrGroupId} 以便写入分组关联，销售属性不写关联。
 */
@Data
public class AttrVO {
    /** 属性 ID；新增时留空由数据库生成，修改时必填。 */
    private Long attrId;
    /** 属性名。 */
    private String attrName;
    /** 是否需要检索：0 不需要，1 需要；为 1 的属性会随商品写入 Elasticsearch 供搜索。 */
    private Integer searchType;
    /** 属性图标在 MinIO 中的对象地址；未设置图标时为 {@code null}。 */
    private String icon;
    /** 可选值列表，多个值用逗号分隔；销售属性与单选型基本属性使用。 */
    private String valueSelect;
    /** 属性类型：0 销售属性，1 基本属性。 */
    private Integer attrType;
    /** 启用状态：0 禁用，1 启用。 */
    private Long enable;
    /** 所属三级分类 ID，指向 {@code pms_category.cat_id}。 */
    private Long catalogId;
    /** 是否在商品介绍中快速展示：0 否，1 是；sku 上仍可单独调整。 */
    private Integer showDesc;
    /** 所属属性分组 ID，仅基本属性有值。 */
    private Long attrGroupId;
    /** 属性值类型：0 唯一，1 单选。 */
    private Integer valueType;
}
