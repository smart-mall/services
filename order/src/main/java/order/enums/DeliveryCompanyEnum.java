package order.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 物流公司（配送方式）枚举，{@code code} 落库到 {@code oms_order.delivery_company}，
 * {@code name} 作为展示文案返回前端。
 *
 * <p><b>为什么是枚举而不是一张表：</b>物流公司是取值有限、几乎不变的基础数据。取值放在这里，
 * 管理端的下拉从 {@code GET /order/order/delivery-companies} 取 —— 清单只有这一份，前端不抄一遍，
 * 也就不会出现"加了一家公司在两个仓库各改一次、漏掉哪边都不报错"的漂移。</p>
 *
 * <p><b>落库的是 code 而不是 name：</b>公司改名（"顺丰速运" → "顺丰速运有限公司"）不该回头改历史订单，
 * 展示文案变化只改本枚举即可。</p>
 */
public enum DeliveryCompanyEnum {

    /** 顺丰速运。 */
    SF("SF", "顺丰速运"),
    /** 圆通速递。 */
    YTO("YTO", "圆通速递"),
    /** 中通快递。 */
    ZTO("ZTO", "中通快递"),
    /** 申通快递。 */
    STO("STO", "申通快递"),
    /** 韵达速递。 */
    YD("YD", "韵达速递"),
    /** 京东物流。 */
    JD("JD", "京东物流"),
    ;

    /** 公司编码，落库到 {@code oms_order.delivery_company}。 */
    private final String code;
    /** 公司名称，展示用。 */
    private final String name;

    DeliveryCompanyEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /**
     * 判断编码是不是本枚举里的合法取值。
     *
     * <p>发货时用它挡掉编外的公司编码：这个值会写进订单并被会员看到，不该由调用方随便给。</p>
     *
     * @param code 待判断的编码，允许为 {@code null}
     * @return 命中任一枚举项时为 {@code true}；{@code null} 为 {@code false}
     */
    public static boolean isValid(String code) {
        for (DeliveryCompanyEnum company : values()) {
            if (company.code.equals(code)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 取全部取值，供管理端下拉使用。
     *
     * @return 全部物流公司，顺序与枚举声明一致
     */
    public static List<DeliveryCompanyEnum> listAll() {
        return Arrays.asList(values());
    }
}
