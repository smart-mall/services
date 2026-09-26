package search.vo;

import es.SkuEsModel;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;


/**
 * 前台商品检索的出参：命中的商品列表、分页信息与聚合出的筛选项。
 */
@Data
public class SearchResult {

    /**
     * 命中的商品列表
     */
    private List<SkuEsModel> product;


    /**
     * 当前页码
     */
    private Integer pageNum;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 总页码
     */
    private Integer totalPages;

    /**
     * 当前查询到的结果，所有涉及到的品牌
     */
    private List<BrandVo> brands;

    /**
     * 当前查询到的结果，所有涉及到的所有属性
     */
    private List<AttrVo> attrs;

    /**
     * 当前查询到的结果，所有涉及到的所有分类
     */
    private List<CatalogVo> catalogs;




    /** 已选筛选条件，前端当筛选 chips 用。 */
    private List<NavVo> navs = new ArrayList<>();

    /** 已选中的属性 ID，前端用它回显属性勾选状态。 */
    private List<Long> attrIds = new ArrayList<>();

    /**
     * 已选筛选条件里的一条：告诉前端要移除哪个查询参数的哪个值。
     *
     * <p>后端只给 {@code removeKey} + {@code removeValue}，不拼装 URL，前端从自己的查询条件里删掉
     * 后重新请求即可。关键词这类没有复选框可回显的条件，只能靠它显示和清除。
     */
    @Data
    public static class NavVo {

        /** 筛选条件的类别名，例如"品牌"、"分类"、"内存" */
        private String navName;

        /** 展示用的具体值，例如"华为"、"手机"、"8GB"；属性多值时用顿号连接 */
        private String navValue;

        /** 点击 x 时要移除的查询参数名，例如 brandId / catalog3Id / attrs */
        private String removeKey;

        /** 要移除的参数值，原样未做 URL 编码，例如 "1" / "5" / "1_华为" */
        private String removeValue;
    }


    /**
     * 检索结果中出现过的品牌，供前端渲染品牌筛选项。
     */
    @Data
    public static class BrandVo {

        /** 品牌 ID。 */
        private Long brandId;

        /** 品牌名称。 */
        private String brandName;

        /** 品牌图片地址。 */
        private String brandImg;
    }


    /**
     * 检索结果中出现过的属性，供前端渲染属性筛选项。
     */
    @Data
    public static class AttrVo {

        /** 属性 ID。 */
        private Long attrId;

        /** 属性名称。 */
        private String attrName;

        /** 该属性在结果中出现过的所有值。 */
        private List<String> attrValue;
    }


    /**
     * 检索结果中出现过的分类，供前端渲染分类筛选项。
     */
    @Data
    public static class CatalogVo {

        /** 分类 ID。 */
        private Long catalogId;

        /** 分类名称。 */
        private String catalogName;
    }
}
