package product.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 会员发表一条回复的入参。
 *
 * <p>只有内容一项：被回复的评价在路径上，回复人与昵称由服务端从网关注入的身份里取，
 * 请求体里若出现这类字段也不作为写入依据。
 */
@Data
public class CommentReplySubmitVo {

    /** 回复内容。 */
    @NotBlank(message = "回复内容不能为空")
    private String content;

}
