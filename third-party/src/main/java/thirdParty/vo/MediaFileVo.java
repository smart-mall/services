package thirdParty.vo;

/**
 * 上传结果。
 *
 * @param url  完整可访问地址，调用方直接存库。这是永久地址，不带签名、不会过期
 * @param name 原始文件名，只用于回显和排查，<b>不参与 key 生成</b>
 * @param size 字节数
 */
public record MediaFileVo(String url, String name, long size) {
}
