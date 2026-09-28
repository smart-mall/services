package coupon.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 优惠券模板，对应 {@code sms_coupon} 表：定义券的类型、面额、发行总量，以及可领取与可使用的时间区间。
 *
 * <p>会员领取后在使用侧生成 {@link CouponHistoryEntity} 记录，本表只保存模板与各项统计计数。
 * 领取记录只存 {@code coupon_id}、不存券面权益，所以 {@code receiveCount > 0} 之后模板的关键字段
 * 不可再改 —— 改了会静默改掉存量券，判定见 {@code CouponServiceImpl#updateCoupon}。
 *
 * <p>字段上的约束注解只在管理端新增/修改时生效，查询路径不触发校验。
 * {@code statusText}、{@code allowedActions}、{@code spuRelations}、{@code categoryRelations}
 * 不落库，由查询侧组装。
 */
@Data
@TableName("sms_coupon")
public class CouponEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键。 */
	@TableId
	private Long id;
	/**
	 * 优惠券类型[0->全场赠券；1->会员赠券；2->购物赠券；3->注册赠券]。
	 *
	 * <p>它表示券怎么发到会员手里，与"领到之后能买什么"的 {@code useType} 是两个维度。
	 * 当前只实现 0（券中心主动领）与 1（后台定向发）。
	 */
	@NotNull(message = "请选择优惠券类型")
	private Integer couponType;
	/** 优惠券图片的地址，可为空；为空时由前端用统一模板渲染。 */
	@Size(max = 2000, message = "优惠券图片地址过长")
	private String couponImg;
	/** 优惠券名称，展示给会员。 */
	@NotBlank(message = "优惠券名称不能为空")
	@Size(max = 100, message = "优惠券名称不能超过 100 个字符")
	private String couponName;
	/** 券面金额，核销时按此金额抵扣商品金额，不抵扣运费。 */
	@NotNull(message = "券面金额不能为空")
	@DecimalMin(value = "0.01", message = "券面金额必须大于 0")
	private BigDecimal amount;
	/** 每个会员最多可领取的张数。 */
	@NotNull(message = "每人限领张数不能为空")
	@Min(value = 1, message = "每人限领张数至少为 1")
	private Integer perLimit;
	/** 使用门槛金额，商品总额达到该值才可用；0 表示无门槛。 */
	@NotNull(message = "使用门槛不能为空")
	@DecimalMin(value = "0.00", message = "使用门槛不能为负数")
	private BigDecimal minPoint;
	/** 优惠券可使用的开始时间。 */
	@NotNull(message = "请选择有效期开始时间")
	private Date startTime;
	/** 优惠券可使用的结束时间。 */
	@NotNull(message = "请选择有效期结束时间")
	private Date endTime;
	/**
	 * 适用范围类型[0->全场通用；1->指定分类；2->指定商品]。
	 *
	 * <p>取 1 或 2 时关联明细必须非空，否则这张券匹配不到任何商品，谁也领不走。
	 */
	@NotNull(message = "请选择适用范围")
	@Min(value = 0, message = "适用范围取值不正确")
	@Max(value = 2, message = "适用范围取值不正确")
	private Integer useType;
	/** 备注。 */
	@Size(max = 200, message = "备注不能超过 200 个字符")
	private String note;
	/** 发行总量，同时是可领取的上限；已领张数达到它即领完。 */
	@NotNull(message = "发行总量不能为空")
	@Min(value = 1, message = "发行总量至少为 1")
	private Integer publishCount;
	/** 已核销数量，由核销链路维护，管理端提交的值会被忽略。 */
	private Integer useCount;
	/** 已领取数量，由领取链路维护；同时是模板关键字段是否锁定的判据。 */
	private Integer receiveCount;
	/** 可领取的开始时间，与可使用区间是两段不同的时间。 */
	@NotNull(message = "请选择领取开始时间")
	private Date enableStartTime;
	/** 可领取的结束时间。 */
	@NotNull(message = "请选择领取结束时间")
	private Date enableEndTime;
	/** 优惠码，暂无消费方。 */
	@Size(max = 64, message = "优惠码不能超过 64 个字符")
	private String code;
	/**
	 * 可领取的会员等级[0->不限等级，其他-对应等级]。
	 *
	 * <p>会员等级体系尚未接通：等级只在注册时取一次默认值、之后不再变动，也不在登录凭证里，
	 * 服务端取到的会员等级恒为 {@code null}。写入侧一律存 0；非 0 的存量行在领取时按不满足处理。
	 */
	private Integer memberLevel;
	/** 发布状态[0-未发布，1-已发布]；只有已发布且在领取窗口内的券才能被领取。由发布/停发接口维护。 */
	private Integer publish;

	/** 当前状态文案，由发布开关、领取窗口与领取进度现算，不落库。 */
	@TableField(exist = false)
	private String statusText;

	/** 该券当前允许执行的动作，取值见 {@code CouponStatusEnum}；列表页据此决定按钮显隐。 */
	@TableField(exist = false)
	private List<String> allowedActions;

	/** 指定商品的关联明细，仅 {@code useType} 为 2 时有值。 */
	@TableField(exist = false)
	private List<CouponSpuRelationEntity> spuRelations;

	/** 指定分类的关联明细，仅 {@code useType} 为 1 时有值。 */
	@TableField(exist = false)
	private List<CouponSpuCategoryRelationEntity> categoryRelations;

}
