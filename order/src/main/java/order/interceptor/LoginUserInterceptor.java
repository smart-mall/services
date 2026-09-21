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
        log.debug("当前请求的 origin:{}", origin);
        if ("http://admin.gulimall.com".equalsIgnoreCase(origin)
                || "http://localhost:56731".equalsIgnoreCase(origin)) {
            return true;
        }

        // 白名单：这几类请求天然没有登录态，但必须能进来
        String uri = request.getRequestURI();
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        // 1、ware 查订单状态。Feign 是从 MQ 监听线程发起的，没有请求上下文，
        //    带不了 X-Member-Claims，所以这条只能放行（接口本身只返回 orderSn/status）
        boolean internalOrderStatus = antPathMatcher.match("/order/order/status/**", uri);
        // 2、第三方支付回调。⚠️ 微信那条原来漏了 —— 名单里只有 /payed/notify（支付宝），
        //    微信的异步通知一直被拦成 401，而微信收不到 success 就会一直重推
        boolean alipayNotify = antPathMatcher.match("/payed/notify", uri);
        boolean wxpayNotify = antPathMatcher.match("/pay/notify", uri);
        if (internalOrderStatus || alipayNotify || wxpayNotify) {
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
        // 白名单那几条路径都没有业务代码读它（ware 的状态查询和两个支付回调都不碰
        // LoginUserInterceptor.loginUser），但这是隐患，顺手清掉。
        loginUser.remove();
    }
}
