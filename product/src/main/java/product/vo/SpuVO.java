package product.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品新增（发布）入参，对应 {@code /product/spuinfo/save} 的请求体。
 *
 * <p>一次携带 spu 主表字段、描述图、图集、积分策略、规格参数与 sku 列表：服务端在一个事务里
 * 拆分写入商品自己的多张表，积分与 sku 优惠再经 Feign 交给 coupon 落库。
 */
@Data
public class SpuVO {

    /** 商品名。 */
    private String spuName;
    /** 商品描述。 */
    private String spuDescription;
    /** 所属三级分类 ID，指向 {@code pms_category.cat_id}。 */
    private Long catalogId;
    /** 品牌 ID，指向 {@code pms_brand.brand_id}。 */
    private Long brandId;
    /** 商品重量，单位：千克。 */
    private BigDecimal weight;
    /** 上架状态：0 新建，1 已上架，2 已下架；新增时一般传 0。 */
    private Integer publishStatus;

    /**
     * 描述图片地址列表，落库时用逗号拼成 {@code pms_spu_info_desc.description}。
     *
     * <p>字段名沿用 {@code decript}：它是前端传的 JSON 键，改名要同步改管理端。
     */
    private List<String> decript;
    /** 实物图片地址列表。 */
    private List<String> images;
    /** 积分策略，经 coupon 服务写入 {@code sms_spu_bounds}。 */
    private Bounds bounds;
    /** 规格参数（基本属性）的取值列表。 */
    private List<BaseAttrs> baseAttrs;
    /** 该 spu 下的全部 sku。 */
    private List<Sku> skus;

    /**
     * 商品积分策略：购买该 spu 可获得的购物积分与成长积分。
     */
    @Data
    public static class Bounds {

        /** 购物积分。 */
        private BigDecimal buyBounds;
        /** 成长积分。 */
        private BigDecimal growBounds;
    }

    /**
     * 规格参数（基本属性）的取值，一项对应 spu 在一个属性上的取值。
     */
    @Data
    public static class BaseAttrs {

        /** 属性 ID，指向 {@code pms_attr.attr_id}。 */
        private Long attrId;
        /** 该 spu 在此属性上的取值。 */
        private String attrValues;
        /** 是否在商品介绍中快速展示：0 否，1 是；落库时写入 {@code pms_product_attr_value.attr_sort}。 */
        private Integer showDesc;
    }
    /**
     * 销售属性取值，一个 sku 的若干销售属性取值组合决定它是哪一个 sku。
     */
    @Data
    public static class Attr {

        /** 属性 ID，指向 {@code pms_attr.attr_id}。 */
        private Long attrId;
        /** 属性名，如颜色、内存。 */
        private String attrName;
        /** 该 sku 在此属性上的取值。 */
        private String attrValue;
    }

    /**
     * sku 图集中的一张图。
     */
    @Data
    public static class Images {

        /** 图片地址。 */
        private String imgUrl;
        /** 是否为默认展示图：0 否，1 是；为 1 的图会成为 sku 的默认图。 */
        private Integer defaultImg;
    }

    /**
     * 该 sku 在一个会员等级下的价格。
     */
    @Data
     public static class MemberPrice {

        /** 会员等级 ID。 */
        private Long id;
        /** 会员等级名称。 */
        private String name;
        /** 会员价，单位：元。 */
        private BigDecimal price;
    }

    /**
     * 商品下的一个 sku，由销售属性取值组合唯一确定。
     */
    @Data
    public static class Sku {

        /** sku 名称。 */
        private String skuName;
        /** sku 价格，单位：元。 */
        private BigDecimal price;
        /** sku 标题。 */
        private String skuTitle;
        /** sku 副标题。 */
        private String skuSubtitle;
        /** sku 图集。 */
        private List<Images> images;
        /** 销售属性取值组合的文字描述，如「蓝色 8G+256G」；服务端不落库，仅随请求传入。 */
        private List<String> descar;
        /** 该 sku 的销售属性取值。 */
        private List<Attr> attr;

        /** 阶梯价：满几件开始打折；为 0 时不写阶梯价。 */
        private Integer fullCount;
        /** 阶梯价折扣，打几折。 */
        private BigDecimal discount;
        /** 阶梯价能否与其他优惠叠加：0 不可叠加，1 可叠加。 */
        private Integer countStatus;
        /** 满减：满多少钱可以减价；为 0 时不写满减。 */
        private BigDecimal fullPrice;
        /** 满减金额：减多少钱。 */
        private BigDecimal reducePrice;
        /** 满减能否与其他优惠叠加：0 不可叠加，1 可叠加。 */
        private Integer priceStatus;
        /** 该 sku 在不同会员等级下的价格。 */
        private List<MemberPrice> memberPrice;
    }
}
