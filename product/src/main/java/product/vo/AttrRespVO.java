package product.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 商品属性详情与列表出参，在 {@link AttrVO} 上补充所属分组与分类信息。
 *
 * <p>由 {@code AttrServiceImpl#queryBaseAttrPage} 与 {@code #getAttrInfo} 组装，是
 * {@code /product/attr/{attrType}/list/{category}} 与 {@code /product/attr/info/{attrId}} 的出参，
 * 供后台属性管理页回显。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AttrRespVO  extends AttrVO {
    /** 所属属性分组名；销售属性不绑定分组，为 {@code null}。 */
    private String groupName;
    /** 所属分类名。 */
    private String catalogName;
    /** 从一级分类到所属分类的 ID 路径，供前端级联选择器回显。 */
    private List<Long> catalogPath;
}
