package product.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 会员提交商品评价的入参。
 *
 * <p>只有用户自己填的三项：商品、会员与各项快照都由服务端从订单服务的核对结果里取，
 * 请求体里若出现这类字段也不作为写入依据。
 */
@Data
public class CommentSubmitVo {

    /** 来源订单项 ID，服务端据此向订单服务核对归属与状态。 */
    @NotNull(message = "订单项不能为空")
    private Long orderItemId;

    /** 星级评分，取值范围 1~5。 */
    @NotNull(message = "请选择星级")
    @Min(value = 1, message = "星级取值为 1~5")
    @Max(value = 5, message = "星级取值为 1~5")
    private Integer star;

    /** 评价内容。 */
    @NotBlank(message = "评价内容不能为空")
    private String content;

}
