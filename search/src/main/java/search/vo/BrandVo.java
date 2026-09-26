package search.vo;

import lombok.Data;


/**
 * 品牌信息，只包含品牌 ID 与名称。
 */
@Data
public class BrandVo {

    /** 品牌 ID。 */
    private Long brandId;

    /** 品牌名称。 */
    private String name;

}
