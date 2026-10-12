package order.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 物流公司选项，供管理端发货弹窗的下拉框使用。
 *
 * <p>清单由服务端下发：前端写死会让同一份清单存在两处，加一家公司要改两个仓库，漏掉哪边都不会报错。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryCompanyVo {

    /** 公司编码，发货时原样回传给服务端。 */
    private String code;

    /** 公司名称，下拉框里显示。 */
    private String name;

}
