package search.service.impl;


import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.NumberRangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import common.to.es.SkuEsModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.*;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightFieldParameters;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import search.constant.EsConstant;
import search.fegin.ProductFeignService;
import search.service.MallSearchService;
import search.vo.SearchParam;
import search.vo.SearchResult;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class MallSearchServiceImpl implements MallSearchService {
    private final ElasticsearchTemplate elasticsearchTemplate;
    private final ProductFeignService productFeignService;

    public MallSearchServiceImpl(ElasticsearchTemplate elasticsearchTemplate, ProductFeignService productFeignService) {
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.productFeignService = productFeignService;
    }

    @Override
    public SearchResult search(SearchParam param) {
        //1、动态构建出查询需要的DSL语句
        SearchResult result;

        //1、准备检索请求
        NativeQuery nativeQuery = buildNativeQuery(param);

        //2、执行检索请求
        SearchHits<SkuEsModel> search = elasticsearchTemplate.search(nativeQuery, SkuEsModel.class, IndexCoordinates.of(EsConstant.PRODUCT_INDEX));

        //3、分析响应数据，封装成我们需要的格式
        result = buildSearchResult(search, param);


        return result;
    }

    /**
     * 构建结果数据
     * 模糊匹配，过滤（按照属性、分类、品牌，价格区间，库存），完成排序、分页、高亮,聚合分析功能
     *
     * @param searchHits 检索结果
     * @return 封装了检索结果的数据
     */
    private SearchResult buildSearchResult(SearchHits<SkuEsModel> searchHits, SearchParam param) {
        SearchResult result = new SearchResult();

        //1、返回的所有查询到的商品
        List<SkuEsModel> esModels = new ArrayList<>();

        //遍历所有商品信息
        if (searchHits != null && !searchHits.getSearchHits().isEmpty()) {
            for (SearchHit<SkuEsModel> hit : searchHits.getSearchHits()) {
                SkuEsModel esModel = hit.getContent();
                //判断是否按关键字检索，若是就显示高亮，否则不显示
                if (StringUtils.hasText(param.getKeyword()) && hit.getHighlightFields().containsKey("skuTitle")) {
                    //拿到高亮信息显示标题
                    List<String> fragments = hit.getHighlightFields().get("skuTitle");
                    if (fragments != null && !fragments.isEmpty()) {
                        String skuTitleValue = fragments.getFirst();
                        esModel.setSkuTitle(skuTitleValue);
                    }

                }
                esModels.add(esModel);
            }

        }
        result.setProduct(esModels);

        //2、当前商品涉及到的所有【属性】信息attrs
        if (searchHits != null && searchHits.getAggregations() != null) {
            ElasticsearchAggregations aggregations = (ElasticsearchAggregations) searchHits.getAggregations();

            // 获取属性信息的聚合
            ElasticsearchAggregation attrsAgg = aggregations.get("attr_agg");
            if (attrsAgg != null) {
                List<SearchResult.AttrVo> attrVos = new ArrayList<>();
                Aggregate aggregate = attrsAgg.aggregation().getAggregate();

                // 因为 attrs 是 nested 类型，所以 attr_agg 是 NestedAggregate
                // 但注意：在响应中，顶层聚合的名称已经去掉了 nested# 前缀
                // 需要从 nested 聚合的 aggregations 中获取子聚合
                Aggregate attrIdAggregate = null;

                // 方式1：如果是 NestedAggregate
                if (aggregate.isNested()) {
                    NestedAggregate nestedAgg = aggregate.nested();
                    attrIdAggregate = nestedAgg.aggregations().get("attr_id_agg");
                }

                if (attrIdAggregate != null && attrIdAggregate.isLterms()) {
                    LongTermsAggregate lterms = attrIdAggregate.lterms();
                    List<LongTermsBucket> buckets = lterms.buckets().array();

                    for (LongTermsBucket bucket : buckets) {
                        SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
                        //1、得到属性的id
                        long attrId = bucket.key();
                        attrVo.setAttrId(attrId);

                        //2、得到属性的名字
                        Aggregate attrNameAgg = bucket.aggregations().get("attr_name_agg");
                        if (attrNameAgg != null && attrNameAgg.isSterms()) {
                            StringTermsAggregate stringTerms = attrNameAgg.sterms();
                            List<StringTermsBucket> nameBuckets = stringTerms.buckets().array();
                            if (!nameBuckets.isEmpty()) {
                                String attrName = nameBuckets.getFirst().key().stringValue();
                                attrVo.setAttrName(attrName);
                            }
                        }

                        //3、得到属性的所有值
                        Aggregate attrValueAgg = bucket.aggregations().get("attr_value_agg");
                        if (attrValueAgg != null && attrValueAgg.isSterms()) {
                            List<String> attrValues = new ArrayList<>();
                            StringTermsAggregate stringTerms = attrValueAgg.sterms();
                            List<StringTermsBucket> valueBuckets = stringTerms.buckets().array();
                            for (StringTermsBucket valueBucket : valueBuckets) {
                                attrValues.add(valueBucket.key().stringValue());
                            }
                            attrVo.setAttrValue(attrValues);
                        }

                        attrVos.add(attrVo);
                    }
                }
                result.setAttrs(attrVos);
            }

            //3、当前商品涉及到的所有【品牌】信息（保持不变）
            ElasticsearchAggregation brandAgg = aggregations.get("brand_agg");
            if (brandAgg != null) {
                List<SearchResult.BrandVo> brandVos = new ArrayList<>();
                Aggregate aggregate = brandAgg.aggregation().getAggregate();

                if (aggregate.isLterms()) {
                    LongTermsAggregate lterms = aggregate.lterms();
                    List<LongTermsBucket> buckets = lterms.buckets().array();

                    for (LongTermsBucket bucket : buckets) {
                        SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
                        long brandId = bucket.key();
                        brandVo.setBrandId(brandId);

                        Aggregate brandNameAgg = bucket.aggregations().get("brand_name_agg");
                        if (brandNameAgg != null && brandNameAgg.isSterms()) {
                            StringTermsAggregate stringTerms = brandNameAgg.sterms();
                            List<StringTermsBucket> nameBuckets = stringTerms.buckets().array();
                            if (!nameBuckets.isEmpty()) {
                                String brandName = nameBuckets.getFirst().key().stringValue();
                                brandVo.setBrandName(brandName);
                            }
                        }

                        Aggregate brandImgAgg = bucket.aggregations().get("brand_img_agg");
                        if (brandImgAgg != null && brandImgAgg.isSterms()) {
                            StringTermsAggregate stringTerms = brandImgAgg.sterms();
                            List<StringTermsBucket> imgBuckets = stringTerms.buckets().array();
                            if (!imgBuckets.isEmpty()) {
                                String brandImg = imgBuckets.getFirst().key().stringValue();
                                brandVo.setBrandImg(brandImg);
                            }
                        }

                        brandVos.add(brandVo);
                    }
                }
                result.setBrands(brandVos);
            }

            //4、当前商品涉及到的所有分类信息
            ElasticsearchAggregation catalogAgg = aggregations.get("catalog_agg");
            if (catalogAgg != null) {
                List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
                Aggregate aggregate = catalogAgg.aggregation().getAggregate();

                if (aggregate.isLterms()) {
                    LongTermsAggregate lterms = aggregate.lterms();
                    List<LongTermsBucket> buckets = lterms.buckets().array();

                    for (LongTermsBucket bucket : buckets) {
                        SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
                        long catalogId = bucket.key();
                        catalogVo.setCatalogId(catalogId);

                        Aggregate catalogNameAgg = bucket.aggregations().get("catalog_name_agg");
                        if (catalogNameAgg != null && catalogNameAgg.isSterms()) {
                            StringTermsAggregate stringTerms = catalogNameAgg.sterms();
                            List<StringTermsBucket> nameBuckets = stringTerms.buckets().array();
                            if (!nameBuckets.isEmpty()) {
                                String catalogName = nameBuckets.getFirst().key().stringValue();
                                catalogVo.setCatalogName(catalogName);
                            }
                        }
                        catalogVos.add(catalogVo);
                    }
                }
                result.setCatalogs(catalogVos);
            }
        }

        //5、分页信息-页码
        result.setPageNum(param.getPageNum());
        //5、1分页信息、总记录数
        long total = 0;
        if (searchHits != null) {
            total = searchHits.getTotalHits();
        }
        result.setTotal(total);

        //5、2分页信息-总页码-计算
        int totalPages = (int) total % EsConstant.PRODUCT_PAGESIZE == 0 ?
                (int) total / EsConstant.PRODUCT_PAGESIZE : ((int) total / EsConstant.PRODUCT_PAGESIZE + 1);
        result.setTotalPages(totalPages);

        List<Integer> pageNavs = new ArrayList<>();
        for (int i = 1; i <= totalPages; i++) {
            pageNavs.add(i);
        }
        result.setPageNavs(pageNavs);

        //6、构建面包屑导航（属性）
        if (param.getAttrs() != null && !param.getAttrs().isEmpty()) {
            List<SearchResult.NavVo> navs = result.getNavs();

            for (String attr : param.getAttrs()) {
                String[] s = attr.split("_");
                Long attrId = Long.parseLong(s[0]);
                String attrValue = s[1];

                SearchResult.NavVo navVo = new SearchResult.NavVo();
                navVo.setNavValue(attrValue);
                result.getAttrIds().add(attrId);

                // 从聚合结果中获取属性名称
                String attrName = result.getAttrs().stream()
                        .filter(a -> a.getAttrId().equals(attrId))
                        .map(SearchResult.AttrVo::getAttrName)
                        .findFirst()
                        .orElse(String.valueOf(attrId));
                navVo.setNavName(attrName);

                // 生成移除链接
                String replace = repleceQueryString(param, attr, "attrs", countParamSize(param));
                navVo.setLink("http://search.gulimall.com/list.html?" + replace);
                navs.add(navVo);
            }
            result.setNavs(navs);
        }

        //7. 品牌面包屑导航
        if (param.getBrandId() != null && !param.getBrandId().isEmpty()) {
            List<SearchResult.NavVo> navs = result.getNavs();
            SearchResult.NavVo navVo = new SearchResult.NavVo();
            navVo.setNavName("品牌");

            // 直接从聚合结果中获取品牌名称
            List<SearchResult.BrandVo> brands = result.getBrands();
            StringBuilder buffer = new StringBuilder();
            String replace = "";

            for (Long brandId : param.getBrandId()) {
                // 从聚合结果中匹配品牌名称
                String brandName = brands.stream()
                        .filter(b -> b.getBrandId().equals(brandId))
                        .map(SearchResult.BrandVo::getBrandName)
                        .findFirst()
                        .orElse(String.valueOf(brandId));

                buffer.append(brandName);
                if (replace.isEmpty()) {
                    replace = repleceQueryString(param, String.valueOf(brandId), "brandId", countParamSize(param));
                }
            }
            navVo.setNavValue(buffer.toString());
            navVo.setLink("http://search.gulimall.com/list.html?" + replace);
            navs.add(navVo);
        }

        //8. 分类面包屑导航
        if (param.getCatalog3Id() != null) {
            List<SearchResult.NavVo> navs = result.getNavs();
            SearchResult.NavVo navVo = new SearchResult.NavVo();
            navVo.setNavName("分类");

            // 从聚合结果中获取分类名称
            String catalogName = result.getCatalogs().stream()
                    .filter(c -> c.getCatalogId().equals(param.getCatalog3Id()))
                    .map(SearchResult.CatalogVo::getCatalogName)
                    .findFirst()
                    .orElse(String.valueOf(param.getCatalog3Id()));

            navVo.setNavValue(catalogName);
            String replace = repleceQueryString(param, String.valueOf(param.getCatalog3Id()), "catalog3Id", countParamSize(param));
            navVo.setLink("http://search.gulimall.com/list.html?" + replace);
            navs.add(navVo);
        }

        log.info("result:{}", result);

        return result;
    }

    private int countParamSize(SearchParam param) {
        int count = 0;
        if (param.getKeyword() != null) {
            count++;
        }
        if (param.getCatalog3Id() != null) {
            count++;
        }
        if (param.getSort() != null) {
            count++;
        }
        if (param.getHasStock() != null) {
            count++;
        }
        if (param.getSkuPrice() != null) {
            count++;
        }
        if (param.getBrandId() != null) {
            count += param.getBrandId().size();
        }
        if (param.getAttrs() != null) {
            count += param.getAttrs().size();
        }
        return count;
    }

    private String repleceQueryString(SearchParam param, String value, String key, int size) {
        String encode = URLEncoder.encode(value, StandardCharsets.UTF_8);
        encode = encode.replace("+", "%20");

        String queryString = param.get_queryString();
        String target = key + "=" + encode;

        if (queryString.startsWith(target)) {
            // 参数在第一位，删除后可能需要处理后面的 &
            String result = queryString.replace(target, "");
            if (result.startsWith("&")) {
                result = result.substring(1);  // 去掉开头的 &
            }
            return result;
        } else {
            // 参数不在第一位，前面肯定有 &
            return queryString.replace("&" + target, "");
        }
    }


    private NativeQuery buildNativeQuery(SearchParam param) {

        // 1. 构建 Bool Query
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        // 1.1 bool-must 模糊匹配
        if (StringUtils.hasText(param.getKeyword())) {
            boolQueryBuilder.must(m -> m
                    .match(match -> match
                            .field("skuTitle")
                            .query(param.getKeyword())
                    )
            );
        }

        // 1.2 bool-filter 按照三级分类id查询
        if (param.getCatalog3Id() != null) {
            boolQueryBuilder.filter(f -> f
                    .term(term -> term
                            .field("catalogId")
                            .value(param.getCatalog3Id())
                    )
            );
        }

        // 1.2.2 brandId 按照品牌id查询
        if (param.getBrandId() != null && !param.getBrandId().isEmpty()) {
            boolQueryBuilder.filter(f -> f
                    .terms(terms -> terms
                            .field("brandId")
                            .terms(t -> t.value(param.getBrandId().stream()
                                    .map(FieldValue::of)
                                    .toList())
                            )
                    )
            );
        }

        // 1.2.3 attrs 按照所有指定的属性查询
        if (param.getAttrs() != null && !param.getAttrs().isEmpty()) {
            param.getAttrs().forEach(item -> {
                String[] s = item.split("_");
                String attrId = s[0];
                String[] attrValues = s[1].split(":");

                BoolQuery.Builder nestedBoolQuery = new BoolQuery.Builder();
                nestedBoolQuery.must(m -> m
                        .term(term -> term
                                .field("attrs.attrId")
                                .value(attrId)
                        )
                );
                nestedBoolQuery.must(m -> m
                        .terms(terms -> terms
                                .field("attrs.attrValue")
                                .terms(t -> t.value(Arrays.stream(attrValues)
                                        .map(FieldValue::of)
                                        .toList())
                                )
                        )
                );

                boolQueryBuilder.filter(f -> f
                        .nested(nested -> nested
                                .path("attrs")
                                .query(nestedBoolQuery.build()._toQuery())
                                .scoreMode(ChildScoreMode.None)
                        )
                );
            });
        }

        // 1.2.4 hasStock 按照库存是否有进行查询
        if (param.getHasStock() != null) {
            boolQueryBuilder.filter(f -> f
                    .term(term -> term
                            .field("hasStock")
                            .value(param.getHasStock() == 1)
                    )
            );
        }

        // 1.2.5 skuPrice 按照价格区间进行查询
        if (StringUtils.hasText(param.getSkuPrice())) {
            String[] price = param.getSkuPrice().split("_");

            // 构建 NumberRangeQuery
            NumberRangeQuery.Builder rangeQueryBuilder =
                    new NumberRangeQuery.Builder()
                            .field("skuPrice");

            if (price.length == 2) {
                // 区间查询
                rangeQueryBuilder
                        .gte(Double.parseDouble(price[0]))
                        .lte(Double.parseDouble(price[1]));
            } else if (price.length == 1) {
                // 大于或小于
                if (param.getSkuPrice().startsWith("_")) {
                    rangeQueryBuilder.lte(Double.parseDouble(price[0]));
                }
                if (param.getSkuPrice().endsWith("_")) {
                    rangeQueryBuilder.gte(Double.parseDouble(price[0]));
                }
            }

            // 创建 RangeQuery 并指定为 number 类型
            RangeQuery rangeQuery = RangeQuery.of(r -> r
                    .number(rangeQueryBuilder.build())
            );

            // 添加到 filter
            boolQueryBuilder.filter(f -> f.range(rangeQuery));
        }

        // 构建 NativeQuery
        NativeQueryBuilder queryBuilder = NativeQuery.builder()
                .withQuery(boolQueryBuilder.build()._toQuery());

        // 2. 排序
        if (StringUtils.hasText(param.getSort())) {
            String[] sortFields = param.getSort().split("_");
            Sort.Order order = new Sort.Order(
                    "asc".equalsIgnoreCase(sortFields[1]) ?
                            Sort.Direction.ASC : Sort.Direction.DESC,
                    sortFields[0]
            );
            queryBuilder.withSort(Sort.by(order));
        }

        // 3. 分页
        int pageNum = param.getPageNum() != null ? param.getPageNum() : 1;
        queryBuilder.withPageable(PageRequest.of(
                pageNum - 1,
                EsConstant.PRODUCT_PAGESIZE
        ));

        // 4. 高亮
        if (StringUtils.hasText(param.getKeyword())) {
            HighlightFieldParameters fieldParameters = HighlightFieldParameters.builder()
                    .withPreTags("<b style='color:red'>")
                    .withPostTags("</b>")
                    .build();

            queryBuilder.withHighlightQuery(
                    new HighlightQuery(
                            new Highlight(List.of(new HighlightField("skuTitle", fieldParameters))),
                            null
                    )
            );
        }

        // 5. 聚合分析
// 5.1 按照品牌进行聚合
        queryBuilder.withAggregation("brand_agg", Aggregation.of(agg -> agg
                .terms(terms -> terms
                        .field("brandId")
                        .size(50)
                )
                .aggregations("brand_name_agg", Aggregation.of(subAgg -> subAgg
                        .terms(terms2 -> terms2
                                .field("brandName")
                                .size(1)
                        )
                ))
                .aggregations("brand_img_agg", Aggregation.of(subAgg -> subAgg
                        .terms(terms2 -> terms2
                                .field("brandImg")
                                .size(1)
                        )
                ))
        ));

// 5.2 按照分类信息进行聚合
        queryBuilder.withAggregation("catalog_agg", Aggregation.of(agg -> agg
                .terms(terms -> terms
                        .field("catalogId")
                        .size(20)
                )
                .aggregations("catalog_name_agg", Aggregation.of(subAgg -> subAgg
                        .terms(terms2 -> terms2
                                .field("catalogName")
                                .size(1)
                        )
                ))
        ));

// 5.3 按照属性信息进行聚合（Nested聚合）
        queryBuilder.withAggregation("attr_agg", Aggregation.of(agg -> agg
                .nested(nested -> nested
                        .path("attrs")
                )
                .aggregations("attr_id_agg", Aggregation.of(subAgg -> subAgg
                        .terms(terms -> terms
                                .field("attrs.attrId")
                                .size(50)
                        )
                        .aggregations("attr_name_agg", Aggregation.of(subSubAgg -> subSubAgg
                                .terms(terms2 -> terms2
                                        .field("attrs.attrName")
                                        .size(1)
                                )
                        ))
                        .aggregations("attr_value_agg", Aggregation.of(subSubAgg -> subSubAgg
                                .terms(terms2 -> terms2
                                        .field("attrs.attrValue")
                                        .size(50)
                                )
                        ))
                ))
        ));

        return queryBuilder.build();
    }
}
