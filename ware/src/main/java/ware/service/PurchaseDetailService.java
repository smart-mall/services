package ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import ware.entity.PurchaseDetailEntity;

import java.util.List;
import java.util.Map;

import ware.vo.PurchaseDetailPageQuery;
public interface PurchaseDetailService extends IService<PurchaseDetailEntity> {

    PageVO<PurchaseDetailEntity> queryPage(PurchaseDetailPageQuery query);

    void saveDetail(PurchaseDetailEntity detail);

    void updateDetail(PurchaseDetailEntity detail);

    void removeDetails(List<Long> ids);
}
