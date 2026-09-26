package auth.service;

/**
 * 登录记录写入，供各登录链路在认证成功后调用。
 *
 * <p>实现方必须 best-effort：写失败只记日志，不能把异常抛给调用方而让用户登录失败。
 */
public interface LoginLogService {

    /**
     * 记一条 web 端登录记录。
     *
     * @param memberId 会员 ID，为 {@code null} 时直接跳过，不落库
     * @param clientIp 客户端 IP，可以为 {@code null}
     */
    void recordWebLogin(Long memberId, String clientIp);
}
