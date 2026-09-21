package seckill.interceptor;


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
 * <p>未登录时由"写一段 {@code <script>alert(...)</script>}"改成 HTTP 401 + JSON，
 * 原因见 order 模块里同名拦截器的注释：调用方是 SPA 的 XHR，HTML 会让它 JSON 解析失败。</p>
 *
 * <p>另外这个拦截器不只是"挡请求"，它还是 {@code loginUser} 这个 ThreadLocal 的唯一写入方 ——
 * {@code SeckillServiceImpl.kill} 就是从这里取当前会员的，和 order 模块同一套做法。</p>
 */
@Slf4j
@Component
public class LoginUserInterceptor implements HandlerInterceptor {

    public static ThreadLocal<MemberResponseVo> loginUser = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //        放行所有后台接口
        String orign = request.getHeader("origin");
        if ("http://admin.gulimall.com".equalsIgnoreCase(orign)) {
            return true;
        }

        String uri = request.getRequestURI();
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        // 只有抢购要求登录。浏览秒杀场次（/seckill/front/current）和商品详情里的秒杀信息都是匿名可见的，
        // 首页的秒杀栏要在没登录时就显示出来。
        boolean match = antPathMatcher.match("/seckill/front/kill", uri);

        if (match) {
            // 获取登录的用户信息（网关验签之后注入的请求头）
            MemberResponseVo attribute = LoginUserUtils.currentUser(request);
            if (attribute == null) {
                // 未登录：HTTP 401 + JSON
                LoginUserUtils.writeUnauthorized(response);
                return false;
            }
            // 把登录后用户的信息放在ThreadLocal里面进行保存
            loginUser.set(attribute);
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 线程复用的清理，理由见 order 模块的 LoginUserInterceptor
        loginUser.remove();
    }
}
