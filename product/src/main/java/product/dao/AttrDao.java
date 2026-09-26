package product.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import product.entity.AttrEntity;

import java.util.List;

/**
 * {@code pms_attr} 商品属性表的 MyBatis-Plus Mapper，自定义 SQL 写在 {@code AttrDao.xml}。
 */
@Mapper
public interface AttrDao extends BaseMapper<AttrEntity> {

    /**
     * 从给定属性 ID 中筛出可检索的属性。
     *
     * <p>SQL：{@code select attr_id from pms_attr where attr_id in (...) and search_type = 1}，
     * 用于上架时决定哪些规格进 Elasticsearch 索引。
     *
     * @param attrIds 待筛选的属性 ID 列表，不能为 {@code null} 或空集合，否则拼出的 {@code in ()} 是非法 SQL
     * @return 其中 {@code search_type = 1} 的属性 ID，是入参的子集；一个都不满足时返回空列表
     */
    List<Long> selectSearchAttrs(@Param("attrIds") List<Long> attrIds);
}
