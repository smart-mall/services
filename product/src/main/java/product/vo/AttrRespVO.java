package product.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class AttrRespVO  extends AttrVO {
    private String groupName;
    private String catalogName;
    private List<Long> catalogPath;
}
