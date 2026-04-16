package search.service;

import common.to.es.SkuEsModel;

import java.util.List;

public interface ProductSaveService {
    public boolean productStatusUp(List<SkuEsModel> skuEsModels);
}
