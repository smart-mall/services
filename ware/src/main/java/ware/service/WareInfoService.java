package ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import ware.entity.WareInfoEntity;
import ware.vo.FareVo;

import java.util.List;
import java.util.Map;

/**
 * 仓库信息
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:20:17
 */
public interface WareInfoService extends IService<WareInfoEntity> {

    PageVO<WareInfoEntity> queryPage(Map<String, Object> params);

    FareVo getFare(Long addrId);

    void deleteByIds(List<Long> ids);
}

