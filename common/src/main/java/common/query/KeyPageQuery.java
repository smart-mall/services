package common.query;

import lombok.Getter;
import lombok.Setter;

/**
 * 带一个 {@code key} 模糊搜索的分页请求，管理端大部分列表接口都是这个形状。
 */
@Getter
@Setter
public class KeyPageQuery extends PageQuery {

    /** 关键字，按各接口自己的含义匹配（品牌名、分类名、采购单号…） */
    private String key;
}
