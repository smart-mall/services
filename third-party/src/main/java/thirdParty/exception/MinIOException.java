package thirdParty.exception;

import common.exception.BaseCodeEnum;
import common.exception.BaseException;

/**
 * 存储层失败。
 *
 * <p>固定用 {@link BaseCodeEnum#MEDIA_STORAGE_ERROR}，对外永远只是一句"可以重试"。</p>
 *
 * <p>注意父类必须是 {@link common.exception.BaseException}：本模块原来自己有一个
 * {@code thirdParty.exception.BaseException}，那是个裸 {@code RuntimeException}，
 * 和 {@code common} 里的同名类毫无关系 —— 继承它的异常不会被
 * {@code GlobalExceptionHandler} 接住，只会变成 Spring 默认的 500 响应体
 * （没有 code / msg 字段，前端直接懵）。那个同名类已经删掉，
 * 免得再有人踩。</p>
 *
 * <p>原因：改造前这里的错误信息是直接 {@code R.error(e.getMessage())} 回给调用方的，
 * 而 MinIO 的异常文本里带着 endpoint、bucket 名和 S3 错误码 —— 等于把内网拓扑
 * 暴露给任何一个能调到这个接口的人。现在原始异常只进日志（见 MinIOUtil）。</p>
 */
public class MinIOException extends BaseException {

    public MinIOException(String message) {
        super(BaseCodeEnum.MEDIA_STORAGE_ERROR, message);
    }
}
