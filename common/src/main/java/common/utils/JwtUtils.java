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
 * JWT 工具：auth 与 member 负责签发、gateway 负责验签，三方共用这一份实现，claim 名只在这里定义一次。
 *
 * <p>claim 名即 token 的线格式，改名或增删字段会让已签发的 token 验不过，签发方与验签方要一起更新。
 * 算法钉死 {@link Jwts.SIG#HS256}：{@code signWith(key)} 会按密钥长度自动挑算法，让 {@code alg} 跟着 secret 长度漂。
 *
 * <p>token 为 null / 空串时 jjwt 抛的是 {@link IllegalArgumentException} 而非 JwtException，
 * 这里统一成 {@link MalformedJwtException}，调用方只 catch JwtException 也不会漏成 500。
 */
public class JwtUtils {

    /** 会员 ID 的 claim 名，同时作为 token 的 subject。 */
    public static final String CLAIM_ID = "uid";
    /** 会员用户名的 claim 名。 */
    public static final String CLAIM_USERNAME = "username";
    /** 会员昵称的 claim 名。 */
    public static final String CLAIM_NICKNAME = "nickname";
    /** 会员头像地址的 claim 名。 */
    public static final String CLAIM_HEADER = "header";
    /** 会员积分的 claim 名。 */
    public static final String CLAIM_INTEGRATION = "integration";

    /** HS256 的密钥不能小于 256 位，否则 jjwt 直接抛 WeakKeyException */
    private static final int MIN_SECRET_BYTES = 32;

    /** HS256 签名与验签用的密钥。 */
    private final SecretKey key;

    /** token 有效期，单位毫秒。 */
    private final long expireMillis;

    /**
     * 构造 JwtUtils，校验密钥长度并把有效期换算成毫秒。
     *
     * @param secret      HS256 密钥，至少 32 字节；来自配置 jwt.secret
     *                    （application-common.yaml 顶层，auth、member 与 gateway 共用同一个值）
     * @param expireHours 有效期（小时），来自配置 jwt.expire-hours
     * @throws IllegalStateException secret 为 {@code null} 或不足 32 字节时抛出
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

    /**
     * 返回 token 有效期（秒）。
     *
     * <p>登录响应里的 {@code expiresIn} 取这个值，前端据此判断何时该重新登录。
     *
     * @return 有效期秒数
     */
    public long getExpireSeconds() {
        return expireMillis / 1000L;
    }

    /**
     * 签发会员 token。
     *
     * <p>只放下面这几个字段，不能把整个 {@link MemberResponseVo} 序列化进去：
     * password 与 accessToken 一旦进了 token 就等于泄露。
     *
     * @param user 登录会员信息，读取其 id、username、nickname、header、integration
     * @return 紧凑格式的 JWT 字符串
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
     * @param token JWT 字符串
     * @return 由 claim 还原的会员信息，claim 里没有的字段为 {@code null}
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
     * 读取 claim 中的数值。
     *
     * <p>JSON 里的数字反序列化回来可能是 Integer 也可能是 Long（取决于大小），
     * 用 Number 接住再自己转，不能直接 get(name, Long.class)。
     *
     * @param claims token 载荷
     * @param name claim 名
     * @return 该 claim 的数值；claim 不存在时为 {@code null}
     */
    private Number number(Claims claims, String name) {
        return claims.get(name, Number.class);
    }
}
