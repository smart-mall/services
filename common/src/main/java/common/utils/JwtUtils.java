package common.utils;

import common.vo.MemberResponseVo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具：auth 负责签发、gateway 负责验签，两边共用这一份实现。
 *
 * <p>为什么放在 common：gateway 已经依赖 common（见 gateway/pom.xml），
 * 放这里不用在 auth 和 gateway 里各写一遍 Claims 的字段名。</p>
 *
 * <p>两个踩过的坑，改这个类之前先看：</p>
 * <ol>
 *   <li><b>算法必须显式指定</b>。{@code signWith(key)} 会按密钥长度自动挑算法
 *       （实测 64 字节的 secret 会被挑成 HS512，header 变成 {@code {"alg":"HS512"}}），
 *       那样算法就跟着 secret 的长度漂：以后有人把 secret 换短一点，算法会静默变成 HS256。
 *       所以这里钉死 {@link Jwts.SIG#HS256}。</li>
 *   <li><b>token 为 null / 空串时 jjwt 抛的是 {@link IllegalArgumentException}，不是 JwtException</b>
 *       （实测：{@code CharSequence cannot be null or empty}）。调用方通常只 catch JwtException，
 *       于是这种请求会变成 500。这里把它统一成 {@link MalformedJwtException}，
 *       调用方 catch 一个 JwtException 就够了。</li>
 * </ol>
 */
public class JwtUtils {

    /** 放进 token 的私有 claim 名。改这里等于改 token 格式，auth 和 gateway 要同时发版 */
    public static final String CLAIM_ID = "uid";
    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_NICKNAME = "nickname";
    public static final String CLAIM_HEADER = "header";
    public static final String CLAIM_INTEGRATION = "integration";

    /** HS256 的密钥不能小于 256 位，否则 jjwt 直接抛 WeakKeyException */
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;

    private final long expireMillis;

    /**
     * @param secret      HS256 密钥，至少 32 字节；来自配置 jwt.secret
     *                    （application-common.yaml 顶层，auth 和 gateway 共用同一个值）
     * @param expireHours 有效期（小时），来自配置 jwt.expire-hours
     */
    public JwtUtils(String secret, long expireHours) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("jwt.secret 至少要 " + MIN_SECRET_BYTES
                    + " 字节（HS256 要求密钥不小于 256 位），当前长度："
                    + (secret == null ? "null" : secret.getBytes(StandardCharsets.UTF_8).length));
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMillis = expireHours * 60L * 60L * 1000L;
    }

    /** token 有效期（秒），用来告诉前端什么时候该重新登录 */
    public long getExpireSeconds() {
        return expireMillis / 1000L;
    }

    /**
     * 签发。只放下面这几个字段，<b>不要</b>把整个 MemberResponseVo 塞进去：
     * 带 password / accessToken 就等着泄露吧。
     */
    public String create(MemberResponseVo user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim(CLAIM_ID, user.getId())
                .claim(CLAIM_USERNAME, user.getUsername())
                .claim(CLAIM_NICKNAME, user.getNickname())
                .claim(CLAIM_HEADER, user.getHeader())
                .claim(CLAIM_INTEGRATION, user.getIntegration())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expireMillis))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 验签并取出用户信息。
     *
     * @throws io.jsonwebtoken.JwtException 签名不对、已过期、格式错、算法被篡改、token 为空，全部是这个类型的子类
     */
    public MemberResponseVo parse(String token) {
        if (token == null || token.isBlank()) {
            throw new MalformedJwtException("token 为空");
        }

        // parseSignedClaims 会顺带校验 exp，过期直接抛 ExpiredJwtException
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

        MemberResponseVo user = new MemberResponseVo();
        user.setId(number(claims, CLAIM_ID) == null ? null : number(claims, CLAIM_ID).longValue());
        user.setUsername(claims.get(CLAIM_USERNAME, String.class));
        user.setNickname(claims.get(CLAIM_NICKNAME, String.class));
        user.setHeader(claims.get(CLAIM_HEADER, String.class));
        user.setIntegration(number(claims, CLAIM_INTEGRATION) == null ? null : number(claims, CLAIM_INTEGRATION).intValue());
        return user;
    }

    /**
     * JSON 里的数字反序列化回来可能是 Integer 也可能是 Long（取决于大小），
     * 用 Number 接住再自己转，别直接 get(name, Long.class)。
     */
    private Number number(Claims claims, String name) {
        return claims.get(name, Number.class);
    }
}
