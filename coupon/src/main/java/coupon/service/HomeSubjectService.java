package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import coupon.entity.HomeSubjectEntity;

import java.util.Map;

import common.query.KeyPageQuery;
/**
 * 首页专题，每个专题跳转到独立页面展示该专题下的商品。
 *
 * <p>继承 {@link IService}，通用增删改查由 MyBatis-Plus 提供；本接口只补管理端的分页查询。
 */
public interface HomeSubjectService extends IService<HomeSubjectEntity> {

    /**
     * 分页查询首页专题，{@code key} 同时模糊匹配专题名与专题 ID。
     *
     * <p>{@code key} 为空时不加筛选条件，返回全部专题。
     *
     * @param query 分页参数与关键字，不能为 {@code null}；{@code key} 可以为 {@code null} 或空白
     * @return 分页结果，无数据时 {@code rows} 为空列表、{@code total} 为 0，不返回 {@code null}
     */
    PageVO<HomeSubjectEntity> queryPage(KeyPageQuery query);
}

