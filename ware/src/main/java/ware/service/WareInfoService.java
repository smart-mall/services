package ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import ware.entity.WareInfoEntity;
import ware.vo.FareVo;

import java.util.List;
import java.util.Map;

import common.query.KeyPageQuery;
/**
 * 仓库信息
 */
public interface WareInfoService extends IService<WareInfoEntity> {

    PageVO<WareInfoEntity> queryPage(KeyPageQuery query);

    FareVo getFare(Long addrId);

    void deleteByIds(List<Long> ids);
}

