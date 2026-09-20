package order.interceptor;

import common.utils.LoginUserUtils;
import common.vo.MemberResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * 登录拦截器。
 *
 * <p>登录态是 JWT：token 的解析统一在网关做（gateway 的 JwtAuthFilter），网关验签通过之后
 * 把用户信息放在 {@link common.constant.AuthServerConstant#MEMBER_CLAIMS_HEADER} 请求头里，
 * 这里只读这个头 —— 不解析 token，也不读 HttpSession。</p>
 *
 * <p>未登录时的返回变了：原来往响应里写一段 {@code <script>alert('请先进行登录...')</script>}
 * 再 302 到 auth.gulimall.com/login.html，那是给浏览器整页跳转用的。现在调用方是 SPA 的 XHR，
 * 收到一段 HTML 会在 JSON 解析时直接崩，所以改成 HTTP 401 + JSON。</p>
 */
@Slf4j
@Component
public class LoginUserInterceptor implements HandlerInterceptor {

    public static ThreadLocal<MemberResponseVo> loginUser = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行所有后台接口
        String origin = request.getHeader("origin");
        log.debug("当前请求的uri:{}", origin);
        if ("http://admin.gulimall.com".equalsIgnoreCase(origin)
                || "http://localhost:56731".equalsIgnoreCase(origin)) {
            return true;
        }

        String uri = request.getRequestURI();
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        boolean match = antPathMatcher.match("/order/order/status/**", uri);
        boolean match1 = antPathMatcher.match("/payed/notify", uri);
        if (match || match1) {
            return true;
        }

        // 获取登录的用户信息（网关验签之后注入的请求头）
        MemberResponseVo attribute = LoginUserUtils.currentUser(request);
        if (attribute == null) {
            // 未登录：HTTP 401 + JSON
            LoginUserUtils.writeUnauthorized(response);
            return false;
        }

        // 把登录后用户的信息放在ThreadLocal里面进行保存
        loginUser.set(attribute);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // Tomcat 的线程是复用的。上面有几个分支是"提前 return true"、根本没 set 的，
        // 如果同一个线程刚处理完 A 的请求、接着处理这种请求，loginUser.get() 会拿到 A。
        // 现在没有可达的路径会读到它（queryPageWithItem 没有 controller 暴露，
        // /order/order/status/** 也没有对应的 handler），但这是隐患，顺手清掉。
        loginUser.remove();
    }
}
