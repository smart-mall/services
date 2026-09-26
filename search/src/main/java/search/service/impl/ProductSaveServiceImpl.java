package search.service.impl;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import es.SkuEsModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.IndexedObjectInformation;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.ByQueryResponse;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.stereotype.Service;
import search.constant.EsConstant;
import search.service.ProductSaveService;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ProductSaveServiceImpl implements ProductSaveService {
    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Override
    public boolean productStatusUp(List<SkuEsModel> skuEsModels) {
        // 批量索引操作
        List<IndexQuery> indexQueries = new ArrayList<>();

        for (SkuEsModel sku : skuEsModels) {
            IndexQuery indexQuery = new IndexQuery();
            indexQuery.setId(String.valueOf(sku.getSkuId()));
            indexQuery.setObject(sku);
            indexQueries.add(indexQuery);
        }

        // 执行批量插入
        List<IndexedObjectInformation> res = elasticsearchTemplate.bulkIndex(indexQueries, IndexCoordinates.of(EsConstant.PRODUCT_INDEX));
        // 检查是否成功
        if (!res.isEmpty()) {
            // 检查是否有失败的信息
            boolean hasError = res.stream().anyMatch(info ->
                    info.seqNo() < 0 || info.version() < 0
            );

            if (hasError) {
                log.error("批量插入部分失败");
                // 打印失败详情
                res.forEach(info -> System.out.println("ID: " + info.id() +
                        ", SeqNo: " + info.seqNo() +
                        ", Version: " + info.version()));
                return false;
            } else {
                log.info("批量插入成功，共 {} 条", res.size());
                return true;
            }
        } else {
            log.error("批量插入失败，返回结果为空");
            return false;
        }
    }

    @Override
    public boolean productStatusDown(List<Long> spuIds) {
        if (spuIds == null || spuIds.isEmpty()) {
            return true;
        }

        // spuId 在索引里可能是 keyword 也可能是 long，传字符串值两种 mapping 都能命中
        List<FieldValue> values = spuIds.stream()
                .map(id -> FieldValue.of(String.valueOf(id)))
                .toList();

        NativeQuery query = NativeQuery.builder()
                .withQuery(new TermsQuery.Builder()
                        .field("spuId")
                        .terms(t -> t.value(values))
                        .build()
                        ._toQuery())
                .build();

        // 删除立刻对后续检索可见，否则要等默认的 1 秒刷新
        DeleteQuery deleteQuery = DeleteQuery.builder(query)
                .withRefresh(true)
                .build();

        ByQueryResponse response = elasticsearchTemplate.delete(
                deleteQuery, SkuEsModel.class, IndexCoordinates.of(EsConstant.PRODUCT_INDEX));

        log.info("下架商品已从 ES 清除 {} 条：spuIds={}", response.getDeleted(), spuIds);
        return true;
    }
}
