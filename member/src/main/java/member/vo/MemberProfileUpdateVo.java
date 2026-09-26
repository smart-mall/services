package member.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Date;

/**
 * 会员资料的修改入参。
 *
 * <p>只有这 7 个字段。等级、积分、成长值、启用状态、用户名、来源由系统决定，
 * 用户改它们等于提权或伪造身份，所以不在白名单里。</p>
 *
 * <p>整体替换语义：表单会把 7 个字段全带上，没填的字段会落成 null（不是"保持不变"）。</p>
 */
@Data
public class MemberProfileUpdateVo {

    /** 昵称。头部显示要用它，空的话回退到 username，但那是兜底不是常态 */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 64, message = "昵称不能超过 64 个字符")
    private String nickname;

    /** 头像地址。来自 third-party 的上传接口，是永久地址 */
    @Size(max = 500, message = "头像地址过长")
    private String header;

    /** 0 未知 / 1 男 / 2 女。 */
    @Min(value = 0, message = "性别取值不正确")
    @Max(value = 2, message = "性别取值不正确")
    private Integer gender;

    /** 生日。前端 el-date-picker 要用 value-format="YYYY-MM-DD" */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date birth;

    /** 所在城市。 */
    @Size(max = 500, message = "所在城市过长")
    private String city;

    /** 职业。 */
    @Size(max = 255, message = "职业过长")
    private String job;

    /** 个性签名。 */
    @Size(max = 255, message = "个性签名过长")
    private String sign;
}
