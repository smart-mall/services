package member.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 收货地址的新增 / 修改入参。
 *
 * <p>不带 memberId：归属由登录态决定，不接受前端传。</p>
 */
@Data
public class AddressSaveVo {

    @NotBlank(message = "收货人不能为空")
    @Size(max = 255, message = "收货人姓名过长")
    private String name;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Size(max = 64, message = "邮政编码过长")
    private String postCode;

    @NotBlank(message = "请选择省份")
    @Size(max = 100, message = "省份名称过长")
    private String province;

    @NotBlank(message = "请选择城市")
    @Size(max = 100, message = "城市名称过长")
    private String city;

    @NotBlank(message = "请选择区县")
    @Size(max = 100, message = "区县名称过长")
    private String region;

    @NotBlank(message = "详细地址不能为空")
    @Size(max = 255, message = "详细地址过长")
    private String detailAddress;

    /** 省市区代码，取自 third-party 地址树的节点 code。选填：名称才是展示和算运费要用的 */
    @Size(max = 15, message = "省市区代码过长")
    private String areacode;

    /**
     * 是否设为默认。
     *
     * <p>只表示"把这条设为默认"，传 false 不会取消默认 —— 默认地址必须恰好有一条，
     * 否则结算页取默认地址时会飘。取消默认只能靠把另一条设为默认。</p>
     */
    private Boolean defaultStatus;
}
