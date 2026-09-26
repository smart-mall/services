package member.exception;


/**
 * 注册时账号名已被占用抛出。
 *
 * <p>非受检异常，调用方不必显式捕获。
 */
public class UsernameException extends RuntimeException {

    /** 使用固定提示语构造异常。 */
    public UsernameException() {
        super("存在相同的用户名");
    }
}
