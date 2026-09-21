package search.vo;

import es.SkuEsModel;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description:
 * @Created: with IntelliJ IDEA.
 * @author: 夏沫止水
 * @createTime: 2020-06-13 14:41
 **/

@Data
public class SearchResult {

    /**
     * 查询到的所有商品信息
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


//    private Long[] attrIds;
    //===========================以上是返回给页面的所有信息============================//


    /* 已选筛选条件（前端当筛选 chips 用） */
    private List<NavVo> navs = new ArrayList<>();
    private List<Long> attrIds = new ArrayList<>();

    /**
     * 已选筛选条件里的一条。原来这里是一个后端拼好的 link（写死了
     * http://search.gulimall.com/list.html?...），SPA 里不必让后端拼 URL：
     * 后端只负责告诉前端"要移除哪个查询参数的哪个值"，前端从自己的查询条件里删掉后重新请求即可。
     *
     * <p>它不承担"面包屑"的导航职责，就是当前筛选状态的可视化 ——
     * 关键词这种没有复选框可以回显的条件，只能靠它显示和清除。</p>
     */
    @Data
    public static class NavVo {

        /** 显示的分类名，例如"品牌"、"分类"、"内存" */
        private String navName;

        /**
         * 显示的具体值，例如"华为"、"手机"、"8GB"。
         * 属性多选时用顿号连接（内部先把协议里的冒号换成顿号），只用于展示。
         */
        private String navValue;

        /** 点击 x 时要从前端查询条件里移除的参数名，例如 brandId / catalog3Id / attrs */
        private String removeKey;

        /** 要移除的参数值，原样未做 URL 编码，例如 "1" / "5" / "1_华为" */
        private String removeValue;
    }


    @Data
    public static class BrandVo {

        private Long brandId;

        private String brandName;

        private String brandImg;
    }


    @Data
    public static class AttrVo {

        private Long attrId;

        private String attrName;

        private List<String> attrValue;
    }


    @Data
    public static class CatalogVo {

        private Long catalogId;

        private String catalogName;
    }
}
