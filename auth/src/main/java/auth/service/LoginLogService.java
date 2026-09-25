package auth.service;

/**
 * 登录记录。
 */
public interface LoginLogService {

    /** 记一条 web 端登录记录。best-effort，失败不影响调用方 */
    void recordWebLogin(Long memberId, String clientIp);
}
