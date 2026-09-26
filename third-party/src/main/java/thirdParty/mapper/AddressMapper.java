package thirdParty.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import thirdParty.entity.AddressEntity;

/**
 * 行政区划表 {@code address} 的 MyBatis-Plus Mapper，提供该表的通用 CRUD。
 *
 * <p>没有自定义 SQL，地址树的组装在 {@code AddressServiceImpl} 中完成。
 */
@Mapper
public interface AddressMapper  extends BaseMapper<AddressEntity> {
}
