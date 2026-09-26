package member.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 会员收货地址，对应 {@code ums_member_receive_address} 表。
 *
 * <p>一个会员可以有多条地址，其中默认地址唯一，由 {@code defaultStatus} 标记。
 */
@Data
@TableName("ums_member_receive_address")
public class MemberReceiveAddressEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键。 */
	@TableId
	private Long id;
	/** 所属会员 ID。 */
	private Long memberId;
	/** 收货人姓名。 */
	private String name;
	/** 收货人电话。 */
	private String phone;
	/** 邮政编码。 */
	private String postCode;
	/** 省 / 直辖市。 */
	private String province;
	/** 城市。 */
	private String city;
	/** 区 / 县。 */
	private String region;
	/** 详细地址（街道门牌）。 */
	private String detailAddress;
	/** 省市区行政编码。 */
	private String areacode;
	/** 是否为默认收货地址 [0-否，1-是]。 */
	private Integer defaultStatus;

}
