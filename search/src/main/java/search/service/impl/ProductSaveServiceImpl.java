package search.service.impl;

import es.SkuEsModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.IndexedObjectInformation;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
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
            indexQuery.setId(String.valueOf(sku.getSkuId())); // 设置文档ID
            indexQuery.setObject(sku); // 设置对象
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
}
