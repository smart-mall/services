package thirdParty.exception;

import common.exception.BaseCodeEnum;
import common.exception.BaseException;

/**
 * 存储层异常：MinIO 读写失败时抛出。
 *
 * <p>错误码固定为 {@link BaseCodeEnum#MEDIA_STORAGE_ERROR}，对外只暴露"可以重试"；
 * MinIO 异常原文含 endpoint、bucket 名与 S3 错误码，只进日志，不回传调用方。</p>
 *
 * <p>父类必须是 {@link BaseException}：{@code GlobalExceptionHandler} 只按
 * {@code common.exception.BaseException} 组装统一响应格式，继承其他异常会退化成 Spring 默认的
 * 500 响应体，没有 code / msg 字段。</p>
 */
public class MinIOException extends BaseException {

    /**
     * 构造存储层异常，错误码固定为 {@link BaseCodeEnum#MEDIA_STORAGE_ERROR}。
     *
     * @param message 面向调用方的提示，不得包含 MinIO 原始异常文本
     */
    public MinIOException(String message) {
        super(BaseCodeEnum.MEDIA_STORAGE_ERROR, message);
    }
}
