package auth.vo;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * QQ 用户信息接口的返回结果，字段与第三方中转域名返回的 JSON 一一对应。
 *
 * <p>除 {@link #token} 由本服务回填外，其余字段都直接来自接口响应，用于展示与社交账号绑定。</p>
 */
@Data
public class QQUserInfo {

    /** 返回码，0表示成功。 */
    private Integer ret;

    /** 错误信息。 */
    private String msg;

    /** 用户是否丢失。 */
    @JSONField(name = "is_lost")
    private Integer isLost;

    /** 用户昵称。 */
    private String nickname;

    /** 性别（文字描述）。 */
    private String gender;

    /** 性别类型：1男，2女。 */
    @JSONField(name = "gender_type")
    private Integer genderType;

    /** 省份。 */
    private String province;

    /** 城市。 */
    private String city;

    /** 出生年份。 */
    private String year;

    /** 头像URL（40x40）。 */
    private String figureurl;

    /** 头像URL（40x40）。 */
    @JSONField(name = "figureurl_1")
    private String figureurl1;

    /** 头像URL（100x100）。 */
    @JSONField(name = "figureurl_2")
    private String figureurl2;

    /** QQ空间头像URL（100x100）。 */
    @JSONField(name = "figureurl_qq_1")
    private String figureurlQq1;

    /** QQ空间头像URL（40x40）。 */
    @JSONField(name = "figureurl_qq_2")
    private String figureurlQq2;

    /** 是否为黄钻用户。 */
    @JSONField(name = "is_yellow_vip")
    private String isYellowVip;

    /** 是否为VIP。 */
    private String vip;

    /** 黄钻等级。 */
    @JSONField(name = "yellow_vip_level")
    private String yellowVipLevel;

    /** 用户等级。 */
    private String level;

    /** 是否为年费黄钻。 */
    @JSONField(name = "is_yellow_year_vip")
    private String isYellowYearVip;

    /** QQ开放平台唯一标识。 */
    @JSONField(name = "open_id")
    private String openId;

    /** 授权 code，由 {@code QqAuthController} 回填后随用户信息一起转交 member 服务。 */
    private String token;
}