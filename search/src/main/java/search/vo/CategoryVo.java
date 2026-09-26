package search.vo;

import lombok.Data;

/** 分类信息，只包含分类 ID 与名称。 */
@Data
public class CategoryVo {

    /** 分类 ID。 */
    private Long catId;

    /** 分类名称。 */
    private String name;
}
