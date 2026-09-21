package member.vo;

import lombok.Data;


@Data
public class MemberUserRegisterVo {

    private String userName;

    private String password;

    /** 手机号注册时带这个；邮箱注册时为 null */
    private String phone;

    /** 邮箱注册时带这个；手机号注册时为 null */
    private String email;

}
