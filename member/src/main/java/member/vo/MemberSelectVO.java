package member.vo;

import lombok.Data;

/** 会员等级下拉选项：只带 ID 与名称，供选择框回显使用。 */
@Data
public class MemberSelectVO {

    /** 会员等级 ID。 */
    private Long id;

    /** 会员等级名称。 */
    private String name;
}
