package thirdParty.vo;

/**
 * 单文件上传结果，不可变，直接作为接口 {@code data} 返回。
 *
 * @param url  完整可访问地址，调用方直接存库；永久地址，不带签名、不会过期
 * @param name 原始文件名，只用于回显和排查，不参与 key 生成
 * @param size 字节数
 */
public record MediaFileVo(String url, String name, long size) {
}
