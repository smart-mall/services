package cart.interceptor;

import common.utils.LoginUserUtils;
import common.vo.MemberResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 购物车的登录拦截器。
 *
 * <p>这里原来叫 {@code CartInterceptor}，干两件事：认登录用户 + 给没登录的访客发一个
 * {@code user-key} cookie 做临时购物车。临时购物车已经砍掉（原因见
 * {@link common.constant.CartConstant}），于是这个类只剩一件事：没有登录身份就 401。</p>
 *
 * <p>登录态是 JWT，但解析 JWT 是网关的事。网关验签之后把用户信息放在
 * {@link common.constant.AuthServerConstant#MEMBER_CLAIMS_HEADER} 请求头里，
 * 这里只读这个头 —— 和 order / seckill 的 {@code LoginUserInterceptor} 完全同构，
 * 也是它们共用同一个类名的原因。</p>
 */
@Slf4j
@Component
public class LoginUserInterceptor implements HandlerInterceptor {

    /**
     * 当前登录用户。Service 层从这里拿 userId —— 之所以用 ThreadLocal 而不是把
     * {@code HttpServletRequest} 一路传到 Service，是为了让 Service 不依赖 Servlet API。
     */
    public static ThreadLocal<MemberResponseVo> loginUser = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        MemberResponseVo user = LoginUserUtils.currentUser(request);
        if (user == null) {
            // 未登录：HTTP 401 + JSON。前端 request.ts 判断的是 HTTP 状态码 401 去清
            // localStorage 里的 token，把 401 塞进 body 的 code 字段它认不出来。
            LoginUserUtils.writeUnauthorized(response);
            return false;
        }
        loginUser.set(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // Tomcat 的线程是复用的，不清掉的话下一个请求会读到上一个人的身份
        loginUser.remove();
    }
}
