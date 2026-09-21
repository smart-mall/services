package member.exception;


/**
 * 邮箱已被注册。
 *
 * <p>和 {@link PhoneException} / {@link UsernameException} 同一个套路：
 * service 里查重发现冲突就抛，由 controller 捕获并转成对应的业务 code（15006）。</p>
 */
public class EmailException extends RuntimeException {

    public EmailException() {
        super("存在相同的邮箱");
    }
}
