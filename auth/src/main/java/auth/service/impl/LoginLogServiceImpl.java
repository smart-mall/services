package auth.service.impl;

import auth.feign.MemberFeignService;
import auth.service.IpLocationService;
import auth.service.LoginLogService;
import common.to.LoginLogTo;
import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 登录成功后写一条登录记录。
 *
 * <p>best-effort：IP 解析和落库都包在 try/catch 里，失败只打日志 ——
 * 一条日志不该让用户登录不了。</p>
 */
@Slf4j
@Service
public class LoginLogServiceImpl implements LoginLogService {

    private final MemberFeignService memberFeignService;
    private final IpLocationService ipLocationService;

    public LoginLogServiceImpl(MemberFeignService memberFeignService, IpLocationService ipLocationService) {
        this.memberFeignService = memberFeignService;
        this.ipLocationService = ipLocationService;
    }

    @Override
    public void recordWebLogin(Long memberId, String clientIp) {
        if (memberId == null) {
            return;
        }
        try {
            LoginLogTo to = new LoginLogTo();
            to.setMemberId(memberId);
            to.setIp(clientIp);
            to.setCity(ipLocationService.resolveCity(clientIp));
            to.setLoginType(LoginLogTo.LOGIN_TYPE_WEB);

            R r = memberFeignService.recordLoginLog(to);
            if (r.getCode() != 0) {
                log.warn("记录登录日志失败: memberId={}, code={}, msg={}", memberId, r.getCode(), r.getMsg());
            }
        } catch (Exception e) {
            log.warn("记录登录日志失败: memberId={}", memberId, e);
        }
    }
}
