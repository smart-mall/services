package search.service.impl;


import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.NumberRangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import es.SkuEsModel;
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
import search.service.MallSearchService;
import search.vo.SearchParam;
import search.vo.SearchResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class MallSearchServiceImpl implements MallSearchService {
    private final ElasticsearchTemplate elasticsearchTemplate;

    public MallSearchServiceImpl(ElasticsearchTemplate elasticsearchTemplate) {
        this.elasticsearchTemplate = elasticsearchTemplate;
    }

    @Override
    public SearchResult search(SearchParam param) {
        //0、补齐缺省值并校验查询条件。不合法直接抛 BaseException，由 common 的
        //   GlobalExceptionHandler 统一转成 code=10001；之后的代码可以直接信任
        //   pageNum / pageSize / sort / skuPrice / attrs 这些字段
        param.normalizeAndValidate();

        //1、准备检索请求
        NativeQuery nativeQuery = buildNativeQuery(param);

        //2、执行检索请求
        SearchHits<SkuEsModel> search = elasticsearchTemplate.search(nativeQuery, SkuEsModel.class, IndexCoordinates.of(EsConstant.PRODUCT_INDEX));

        //3、分析响应数据，封装成我们需要的格式
        return buildSearchResult(search, param);
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

            // 获取属性信息的聚合。
            // attr_agg 的结构是 global → attr_scope(filter) → attr_nested，不把外面两层剥掉
            // 拿到的会是 GlobalAggregate，而不是期望的 NestedAggregate。
            ElasticsearchAggregation attrsAgg = aggregations.get("attr_agg");
            if (attrsAgg != null) {
                List<SearchResult.AttrVo> attrVos = new ArrayList<>();
                Aggregate aggregate = unwrapFacet(attrsAgg.aggregation().getAggregate(), "attr_scope", "attr_nested");

                // 因为 attrs 是 nested 类型，剥掉外面两层之后就是 NestedAggregate
                Aggregate attrIdAggregate = null;

                if (aggregate != null && aggregate.isNested()) {
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

            //3、当前商品涉及到的所有【品牌】信息
            ElasticsearchAggregation brandAgg = aggregations.get("brand_agg");
            if (brandAgg != null) {
                List<SearchResult.BrandVo> brandVos = new ArrayList<>();
                Aggregate aggregate = unwrapFacet(brandAgg.aggregation().getAggregate(), "brand_scope", "brand_terms");

                if (aggregate != null && aggregate.isLterms()) {
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
                Aggregate aggregate = unwrapFacet(catalogAgg.aggregation().getAggregate(), "catalog_scope", "catalog_terms");

                if (aggregate != null && aggregate.isLterms()) {
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
        long total = searchHits != null ? searchHits.getTotalHits() : 0;
        result.setTotal(total);

        //5、2分页信息-总页码-计算。用前端传进来的 pageSize（normalizeAndValidate 已补齐）
        int pageSize = param.getPageSize();
        int totalPages = (int) (total % pageSize == 0 ? total / pageSize : total / pageSize + 1);
        result.setTotalPages(totalPages);
        // 不再返回 pageNavs：原来是把 1..totalPages 全部页码塞进数组，结果一多就是个大数组。
        // 前端拿 pageNum / totalPages 自己算要显示哪几个页码更合适（它知道自己的分页控件有多宽）。

        //6、当前已选中的属性 id
        // 单独算一遍，不再像原来那样挂在下面的 navs 循环里 —— 前端要用它回显属性的勾选状态，
        // 将来就算不再返回 navs，这个字段也得留着。
        if (param.getAttrs() != null) {
            for (String attr : param.getAttrs()) {
                SearchParam.AttrFilter parsed = SearchParam.parseAttr(attr);
                if (parsed != null) {
                    result.getAttrIds().add(parsed.attrId());
                }
            }
        }

        //7、已选筛选条件（属性）。前端当筛选 chips 用：每条告诉它"要移除哪个参数的哪个值"。
        if (param.getAttrs() != null && !param.getAttrs().isEmpty()) {
            List<SearchResult.NavVo> navs = result.getNavs();
            // 一条都没命中时聚合结果整体为空，result.getAttrs() 会是 null
            List<SearchResult.AttrVo> attrs = result.getAttrs() == null ? List.of() : result.getAttrs();

            for (String attr : param.getAttrs()) {
                // 只按第一个下划线切分：原来的 split("_") 会把 "1_8GB_plus" 这种值截断成 "8GB"
                SearchParam.AttrFilter parsed = SearchParam.parseAttr(attr);
                if (parsed == null) {
                    continue;
                }

                SearchResult.NavVo navVo = new SearchResult.NavVo();
                // 同一属性多值时协议里用冒号分隔（8GB:12GB），展示时换成顿号
                navVo.setNavValue(parsed.value().replace(":", "、"));

                // 从聚合结果中获取属性名称
                String attrName = attrs.stream()
                        .filter(a -> a.getAttrId().equals(parsed.attrId()))
                        .map(SearchResult.AttrVo::getAttrName)
                        .findFirst()
                        .orElse(String.valueOf(parsed.attrId()));
                navVo.setNavName(attrName);

                // 前端点 x 时把 attrs 里的这一项删掉重新请求，值形如 "属性id_属性值"（原样回传）
                navVo.setRemoveKey("attrs");
                navVo.setRemoveValue(attr);
                navs.add(navVo);
            }
            result.setNavs(navs);
        }

        //8. 已选筛选条件（品牌）
        if (param.getBrandId() != null && !param.getBrandId().isEmpty()) {
            List<SearchResult.NavVo> navs = result.getNavs();
            // 一条都没命中时聚合结果整体为空，result.getBrands() 会是 null
            List<SearchResult.BrandVo> brands = result.getBrands() == null ? List.of() : result.getBrands();

            // 选了多个品牌就生成多条面包屑，每条各自负责移除自己的 brandId。
            // 原来是把所有品牌名拼成一条、但点击 x 只移除第一个 brandId，剩下的品牌还在过滤却看不到了
            for (Long brandId : param.getBrandId()) {
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                navVo.setNavName("品牌");
                navVo.setNavValue(brands.stream()
                        .filter(b -> b.getBrandId().equals(brandId))
                        .map(SearchResult.BrandVo::getBrandName)
                        .findFirst()
                        .orElse(String.valueOf(brandId)));
                navVo.setRemoveKey("brandId");
                navVo.setRemoveValue(String.valueOf(brandId));
                navs.add(navVo);
            }
        }

        //9. 已选筛选条件（分类）
        if (param.getCatalog3Id() != null) {
            List<SearchResult.NavVo> navs = result.getNavs();
            // 一条都没命中时聚合结果整体为空，result.getCatalogs() 会是 null
            List<SearchResult.CatalogVo> catalogs = result.getCatalogs() == null ? List.of() : result.getCatalogs();

            SearchResult.NavVo navVo = new SearchResult.NavVo();
            navVo.setNavName("分类");

            // 从聚合结果中获取分类名称
            navVo.setNavValue(catalogs.stream()
                    .filter(c -> c.getCatalogId().equals(param.getCatalog3Id()))
                    .map(SearchResult.CatalogVo::getCatalogName)
                    .findFirst()
                    .orElse(String.valueOf(param.getCatalog3Id())));

            navVo.setRemoveKey("catalog3Id");
            navVo.setRemoveValue(String.valueOf(param.getCatalog3Id()));
            navs.add(navVo);
        }

        log.debug("result:{}", result);

        return result;
    }

    private NativeQuery buildNativeQuery(SearchParam param) {

        // 1. 构建 Bool Query。
        //    facet 聚合要各自排除掉"自己这一类"的过滤条件，所以过滤条件的拼装抽到了
        //    buildBoolQuery 里；这里传 null 表示所有条件都生效（主查询用）。
        BoolQuery boolQuery = buildBoolQuery(param, null);

        // 构建 NativeQuery
        NativeQueryBuilder queryBuilder = NativeQuery.builder()
                .withQuery(boolQuery._toQuery());

        // 2. 排序（合法性已由 SearchParam.normalizeAndValidate() 统一校验）
        if (StringUtils.hasText(param.getSort())) {
            String[] sortFields = param.getSort().split("_");
            Sort.Order order = new Sort.Order(
                    "asc".equalsIgnoreCase(sortFields[1]) ?
                            Sort.Direction.ASC : Sort.Direction.DESC,
                    sortFields[0]
            );
            queryBuilder.withSort(Sort.by(order));
        }

        // 3. 分页（pageNum / pageSize 已由 normalizeAndValidate() 补齐并校验过范围）
        queryBuilder.withPageable(PageRequest.of(
                param.getPageNum() - 1,
                param.getPageSize()
        ));

        // 4. 高亮
        if (StringUtils.hasText(param.getKeyword())) {
            HighlightFieldParameters fieldParameters = HighlightFieldParameters.builder()
                    .withPreTags("<em>")
                    .withPostTags("</em>")
                    .build();

            queryBuilder.withHighlightQuery(
                    new HighlightQuery(
                            new Highlight(List.of(new HighlightField("skuTitle", fieldParameters))),
                            null
                    )
            );
        }

        // 5. 聚合分析（三个 facet）
        //
        // 每个 facet 的结构是 global → filter → terms/nested：
        //
        // 为什么要 global（关键）：ES 的聚合是在"父查询命中的文档"上算的，子聚合只能在那个集合里
        // 继续收窄，不能放宽。所以只包一层 filter 聚合是没用的 —— 勾了"华为"之后聚合仍然只在
        // 华为的文档里算，brand_agg 里依然只剩华为。
        // 这一点在真实 ES 8.11 上验证过：主查询 brandId=1 时，
        //   filter 写法          → 品牌桶只有 1 个：1(3)
        //   global + filter 写法 → 品牌桶 3 个：2(9), 6(4), 1(3)
        // global 会跳出查询作用域、在索引全部文档上算，所以里面那个 filter 必须把该生效的条件
        // （关键字、分类、价格、有货……）自己重新加上，这就是 buildBoolQuery 的第二个参数。
        //
        // 排除的只是"自己这一类"：选了三级分类之后品牌候选仍然只在该分类里（这是想要的），
        // 价格区间、是否有货这类硬条件也仍然生效。
// 5.1 按照品牌进行聚合
        queryBuilder.withAggregation("brand_agg", Aggregation.of(agg -> agg
                .global(g -> g)
                .aggregations("brand_scope", Aggregation.of(scope -> scope
                        .filter(buildBoolQuery(param, FacetKind.BRAND)._toQuery())
                        .aggregations("brand_terms", Aggregation.of(termsAgg -> termsAgg
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
                        ))
                ))
        ));

// 5.2 按照分类信息进行聚合
        queryBuilder.withAggregation("catalog_agg", Aggregation.of(agg -> agg
                .global(g -> g)
                .aggregations("catalog_scope", Aggregation.of(scope -> scope
                        .filter(buildBoolQuery(param, FacetKind.CATALOG)._toQuery())
                        .aggregations("catalog_terms", Aggregation.of(termsAgg -> termsAgg
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
                        ))
                ))
        ));

// 5.3 按照属性信息进行聚合（Nested聚合，同样包在 global → filter 里面）
        queryBuilder.withAggregation("attr_agg", Aggregation.of(agg -> agg
                .global(g -> g)
                .aggregations("attr_scope", Aggregation.of(scope -> scope
                        .filter(buildBoolQuery(param, FacetKind.ATTR)._toQuery())
                        .aggregations("attr_nested", Aggregation.of(nestedAgg -> nestedAgg
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
                        ))
                ))
        ));

        return queryBuilder.build();
    }

    /**
     * facet 聚合要排除掉"自己这一类"的过滤条件，用这个枚举区分是哪一类。
     */
    private enum FacetKind {
        BRAND, ATTR, CATALOG
    }

    /**
     * 构建 bool 查询。
     *
     * @param excluding 本次要排除掉哪一类过滤条件：主查询传 null（所有条件都生效），
     *                  三个 facet 聚合各自传自己的类型
     */
    private BoolQuery buildBoolQuery(SearchParam param, FacetKind excluding) {

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
        if (param.getCatalog3Id() != null && excluding != FacetKind.CATALOG) {
            boolQueryBuilder.filter(f -> f
                    .term(term -> term
                            .field("catalogId")
                            .value(param.getCatalog3Id())
                    )
            );
        }

        // 1.2.2 brandId 按照品牌id查询
        if (param.getBrandId() != null && !param.getBrandId().isEmpty() && excluding != FacetKind.BRAND) {
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
        // 每个属性一个 nested filter，属性之间是 AND；同一属性内多个值是 OR（同一个 nested 文档里 terms）
        if (param.getAttrs() != null && !param.getAttrs().isEmpty() && excluding != FacetKind.ATTR) {
            for (String item : param.getAttrs()) {
                // normalizeAndValidate() 已经保证能解析出来，这里只是兜底
                SearchParam.AttrFilter parsed = SearchParam.parseAttr(item);
                if (parsed == null) {
                    continue;
                }

                List<FieldValue> attrValues = Arrays.stream(parsed.value().split(":"))
                        .filter(StringUtils::hasText)
                        .map(FieldValue::of)
                        .toList();
                if (attrValues.isEmpty()) {
                    continue;
                }

                BoolQuery.Builder nestedBoolQuery = new BoolQuery.Builder();
                nestedBoolQuery.must(m -> m
                        .term(term -> term
                                .field("attrs.attrId")
                                .value(parsed.attrId())
                        )
                );
                nestedBoolQuery.must(m -> m
                        .terms(terms -> terms
                                .field("attrs.attrValue")
                                .terms(t -> t.value(attrValues))
                        )
                );

                boolQueryBuilder.filter(f -> f
                        .nested(nested -> nested
                                .path("attrs")
                                .query(nestedBoolQuery.build()._toQuery())
                                .scoreMode(ChildScoreMode.None)
                        )
                );
            }
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
            // split 必须带 -1 才会保留结尾的空串："1000_" 用默认的 split 只得到一段，
            // 会掉进"既不是区间也不是单边"的空档，价格条件被静默丢掉。
            String[] price = param.getSkuPrice().split("_", -1);

            // 构建 NumberRangeQuery
            NumberRangeQuery.Builder rangeQueryBuilder =
                    new NumberRangeQuery.Builder()
                            .field("skuPrice");

            // 合法性已由 SearchParam.isValidSkuPrice() 保证，这里只可能是
            // 1000_2000 / _2000 / 1000_ 三种形状：哪一端非空就加哪一端的边界。
            // 原来的写法把 "_2000" 当成两段区间去 parseDouble("")，直接 NumberFormatException。
            if (!price[0].isBlank()) {
                rangeQueryBuilder.gte(Double.parseDouble(price[0]));
            }
            if (!price[1].isBlank()) {
                rangeQueryBuilder.lte(Double.parseDouble(price[1]));
            }

            // 创建 RangeQuery 并指定为 number 类型
            RangeQuery rangeQuery = RangeQuery.of(r -> r
                    .number(rangeQueryBuilder.build())
            );

            // 添加到 filter
            boolQueryBuilder.filter(f -> f.range(rangeQuery));
        }

        return boolQueryBuilder.build();
    }

    /**
     * 剥掉 facet 聚合外面的 global 和 filter 两层包装，拿到真正做 terms / nested 的那一层。
     *
     * <p>三个 facet 都不是裸的 terms / nested：结构是 global → filter → terms/nested
     * （为什么要 global 见 buildNativeQuery 里的注释）。</p>
     *
     * @param aggregate 顶层聚合结果
     * @param scopeName 中间那层 filter 聚合的名字
     * @param innerName 最里面那个 terms / nested 聚合的名字
     * @return 里层聚合结果；取不到时返回 null
     */
    private Aggregate unwrapFacet(Aggregate aggregate, String scopeName, String innerName) {
        if (aggregate == null) {
            return null;
        }
        Aggregate current = aggregate;
        if (current.isGlobal()) {
            current = current.global().aggregations().get(scopeName);
        }
        if (current != null && current.isFilter()) {
            current = current.filter().aggregations().get(innerName);
        }
        return current;
    }
}
