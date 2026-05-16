package thirdParty.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import thirdParty.entity.AddressEntity;

@Mapper
public interface AddressMapper  extends BaseMapper<AddressEntity> {
}
