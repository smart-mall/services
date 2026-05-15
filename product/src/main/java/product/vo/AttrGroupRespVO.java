package product.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import product.entity.AttrGroupEntity;

@EqualsAndHashCode(callSuper = true)
@Data
public class AttrGroupRespVO extends AttrGroupEntity {
    private String catelogName;
}
