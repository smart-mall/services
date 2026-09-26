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

/**
 * 前台商品检索服务的 ES 实现：把 {@link SearchParam} 翻译成 ES 查询，再把命中结果与聚合结果封装成
 * {@link SearchResult}。
 *
 * <p>无状态、线程安全，依赖的 {@link ElasticsearchTemplate} 由容器注入。
 */
@Slf4j
@Service
public class MallSearchServiceImpl implements MallSearchService {
    private final ElasticsearchTemplate elasticsearchTemplate;

    public MallSearchServiceImpl(ElasticsearchTemplate elasticsearchTemplate) {
        this.elasticsearchTemplate = elasticsearchTemplate;
    }

    /** {@inheritDoc} */
    @Override
    public SearchResult search(SearchParam param) {
        // 1. 补齐缺省值并校验查询条件：不合法抛 ValidationException，由 common 的
        //    GlobalExceptionHandler 统一转成 code=10001；之后可以直接信任
        //    pageNum / pageSize / sort / skuPrice / attrs 这些字段
        param.normalizeAndValidate();

        // 2. 准备检索请求
        NativeQuery nativeQuery = buildNativeQuery(param);

        // 3. 执行检索请求
        SearchHits<SkuEsModel> search = elasticsearchTemplate.search(nativeQuery, SkuEsModel.class, IndexCoordinates.of(EsConstant.PRODUCT_INDEX));

        // 4. 分析响应数据，封装成调用方需要的格式
        return buildSearchResult(search, param);
    }

    /**
     * 把 ES 的命中结果与聚合结果封装成出参。
     *
     * <p>商品标题优先取高亮片段；聚合结果按属性、品牌、分类三组转成筛选项；最后补上分页信息与
     * 已选筛选条件。
     *
     * @param searchHits ES 检索结果
     * @param param 当前检索条件，用于回填页码与已选筛选条件
     * @return 封装好的检索结果
     */
    private SearchResult buildSearchResult(SearchHits<SkuEsModel> searchHits, SearchParam param) {
        SearchResult result = new SearchResult();

        // 1. 返回的所有查询到的商品
        List<SkuEsModel> esModels = new ArrayList<>();

        if (searchHits != null && !searchHits.getSearchHits().isEmpty()) {
            for (SearchHit<SkuEsModel> hit : searchHits.getSearchHits()) {
                SkuEsModel esModel = hit.getContent();
                // 高亮只在关键字命中时返回，取不到片段就保留 ES 里的原文标题
                if (StringUtils.hasText(param.getKeyword()) && hit.getHighlightFields().containsKey("skuTitle")) {
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

        // 2. 当前商品涉及到的所有属性信息（attrs）
        if (searchHits != null && searchHits.getAggregations() != null) {
            ElasticsearchAggregations aggregations = (ElasticsearchAggregations) searchHits.getAggregations();

            // attr_agg 的结构是 global → attr_scope(filter) → attr_nested，不剥掉外面两层
            // 拿到的是 GlobalAggregate，不是 attrs 对应的 NestedAggregate
            ElasticsearchAggregation attrsAgg = aggregations.get("attr_agg");
            if (attrsAgg != null) {
                List<SearchResult.AttrVo> attrVos = new ArrayList<>();
                Aggregate aggregate = unwrapFacet(attrsAgg.aggregation().getAggregate(), "attr_scope", "attr_nested");

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
                        long attrId = bucket.key();
                        attrVo.setAttrId(attrId);

                        Aggregate attrNameAgg = bucket.aggregations().get("attr_name_agg");
                        if (attrNameAgg != null && attrNameAgg.isSterms()) {
                            StringTermsAggregate stringTerms = attrNameAgg.sterms();
                            List<StringTermsBucket> nameBuckets = stringTerms.buckets().array();
                            if (!nameBuckets.isEmpty()) {
                                String attrName = nameBuckets.getFirst().key().stringValue();
                                attrVo.setAttrName(attrName);
                            }
                        }

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

            // 3. 当前商品涉及到的所有品牌信息
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

            // 4. 当前商品涉及到的所有分类信息
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

        // 5. 分页信息：页码、总记录数、总页码（pageSize 已由 normalizeAndValidate 补齐）
        result.setPageNum(param.getPageNum());
        long total = searchHits != null ? searchHits.getTotalHits() : 0;
        result.setTotal(total);

        int pageSize = param.getPageSize();
        int totalPages = (int) (total % pageSize == 0 ? total / pageSize : total / pageSize + 1);
        result.setTotalPages(totalPages);
        // 页码导航由前端用 pageNum / totalPages 自己算，服务端不返回页码数组

        // 6. 当前已选中的属性 id：前端用它回显属性勾选状态，与 navs 是否返回无关
        if (param.getAttrs() != null) {
            for (String attr : param.getAttrs()) {
                SearchParam.AttrFilter parsed = SearchParam.parseAttr(attr);
                if (parsed != null) {
                    result.getAttrIds().add(parsed.attrId());
                }
            }
        }

        // 7. 已选筛选条件（属性）：每条告诉前端要移除哪个参数的哪个值
        if (param.getAttrs() != null && !param.getAttrs().isEmpty()) {
            List<SearchResult.NavVo> navs = result.getNavs();
            // 一条都没命中时聚合结果整体为空，result.getAttrs() 会是 null
            List<SearchResult.AttrVo> attrs = result.getAttrs() == null ? List.of() : result.getAttrs();

            for (String attr : param.getAttrs()) {
                // 属性值里可能自带下划线，只能按第一个下划线切分，否则值会被截断
                SearchParam.AttrFilter parsed = SearchParam.parseAttr(attr);
                if (parsed == null) {
                    continue;
                }

                SearchResult.NavVo navVo = new SearchResult.NavVo();
                // 同一属性多值时协议里用冒号分隔（8GB:12GB），展示时换成顿号
                navVo.setNavValue(parsed.value().replace(":", "、"));

                // 聚合里查不到名称时退回属性 id，保证 chips 不会显示空白
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

        // 8. 已选筛选条件（品牌）
        if (param.getBrandId() != null && !param.getBrandId().isEmpty()) {
            List<SearchResult.NavVo> navs = result.getNavs();
            // 一条都没命中时聚合结果整体为空，result.getBrands() 会是 null
            List<SearchResult.BrandVo> brands = result.getBrands() == null ? List.of() : result.getBrands();

            // 每个品牌单独一条：合并成一条时前端移除只作用于第一个 brandId，其余品牌仍在生效
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

        // 9. 已选筛选条件（分类）
        if (param.getCatalog3Id() != null) {
            List<SearchResult.NavVo> navs = result.getNavs();
            // 一条都没命中时聚合结果整体为空，result.getCatalogs() 会是 null
            List<SearchResult.CatalogVo> catalogs = result.getCatalogs() == null ? List.of() : result.getCatalogs();

            SearchResult.NavVo navVo = new SearchResult.NavVo();
            navVo.setNavName("分类");

            // 聚合里查不到名称时退回分类 id，保证 chips 不会显示空白
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

    /**
     * 把检索条件翻译成 ES 的 NativeQuery：bool 查询、排序、分页、高亮与三个 facet 聚合。
     *
     * @param param 检索条件，缺省值已补齐
     * @return 可直接交给 {@link ElasticsearchTemplate} 执行的查询
     */
    private NativeQuery buildNativeQuery(SearchParam param) {

        // 1. 构建 bool 查询：facet 聚合要各自排除掉"自己这一类"的过滤条件，所以过滤条件的拼装
        //    抽到了 buildBoolQuery 里；这里传 null 表示所有条件都生效（主查询用）
        BoolQuery boolQuery = buildBoolQuery(param, null);

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

        // 5. 聚合分析（品牌、分类、属性三个 facet），结构都是 global → filter → terms/nested
        //
        // 聚合默认只在父查询命中的文档上算，外面只包一层 filter 收窄不了候选：勾了"华为"之后品牌桶
        // 里依然只剩华为。所以每个 facet 用 global 跳出查询作用域，再在 filter 里把该生效的条件
        // 重新加上（buildBoolQuery 的第二个参数），只排除"自己这一类"。
        // 5.1 按品牌聚合
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

        // 5.2 按分类聚合
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

        // 5.3 按属性聚合（Nested 聚合，同样包在 global → filter 里）
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
     * facet 聚合需要排除的过滤条件类别，每个取值对应一个聚合维度。
     */
    private enum FacetKind {

        /** 品牌聚合：算品牌分布时排除 brandId 过滤条件。 */
        BRAND,

        /** 属性聚合：算属性分布时排除 attrs 过滤条件。 */
        ATTR,

        /** 分类聚合：算分类分布时排除 catalog3Id 过滤条件。 */
        CATALOG
    }

    /**
     * 构建 bool 查询：把关键字、分类、品牌、属性、库存、价格这些条件拼成 must / filter。
     *
     * @param param 检索条件
     * @param excluding 要排除掉哪一类过滤条件：主查询传 {@code null}（所有条件都生效），
     *                  三个 facet 聚合各自传自己的类型
     * @return 组装好的 bool 查询
     */
    private BoolQuery buildBoolQuery(SearchParam param, FacetKind excluding) {

        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        // 1.1 must：关键字模糊匹配
        if (StringUtils.hasText(param.getKeyword())) {
            boolQueryBuilder.must(m -> m
                    .match(match -> match
                            .field("skuTitle")
                            .query(param.getKeyword())
                    )
            );
        }

        // 1.2 filter：三级分类
        if (param.getCatalog3Id() != null && excluding != FacetKind.CATALOG) {
            boolQueryBuilder.filter(f -> f
                    .term(term -> term
                            .field("catalogId")
                            .value(param.getCatalog3Id())
                    )
            );
        }

        // 1.3 filter：品牌，多选之间是 OR
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

        // 1.4 filter：属性。每个属性一个 nested filter，属性之间是 AND，
        //     同一属性内多个值是 OR（同一个 nested 文档里 terms）
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

        // 1.5 filter：是否有货
        if (param.getHasStock() != null) {
            boolQueryBuilder.filter(f -> f
                    .term(term -> term
                            .field("hasStock")
                            .value(param.getHasStock() == 1)
                    )
            );
        }

        // 1.6 filter：价格区间
        if (StringUtils.hasText(param.getSkuPrice())) {
            // split 必须带 -1 才会保留结尾的空串："1000_" 用默认的 split 只得到一段，
            // 价格条件会被静默丢掉
            String[] price = param.getSkuPrice().split("_", -1);

            NumberRangeQuery.Builder rangeQueryBuilder =
                    new NumberRangeQuery.Builder()
                            .field("skuPrice");

            // 合法性已由 SearchParam.isValidSkuPrice() 保证，这里只可能是
            // 1000_2000 / _2000 / 1000_ 三种形状：哪一端非空就加哪一端的边界
            if (!price[0].isBlank()) {
                rangeQueryBuilder.gte(Double.parseDouble(price[0]));
            }
            if (!price[1].isBlank()) {
                rangeQueryBuilder.lte(Double.parseDouble(price[1]));
            }

            RangeQuery rangeQuery = RangeQuery.of(r -> r
                    .number(rangeQueryBuilder.build())
            );

            boolQueryBuilder.filter(f -> f.range(rangeQuery));
        }

        return boolQueryBuilder.build();
    }

    /**
     * 剥掉 facet 聚合外面的 global 和 filter 两层包装，拿到真正做 terms / nested 的那一层。
     *
     * <p>三个 facet 都不是裸的 terms / nested：结构是 global → filter → terms/nested
     * （global 的作用见 buildNativeQuery 里的注释）。</p>
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
