package common.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 通用 HTTP 工具：按 host、path 与查询参数拼出 URL，发送 GET / POST / PUT / DELETE 请求并返回原始响应。
 *
 * <p>请求头 map 必须非 {@code null}，方法直接遍历它；查询参数可为 {@code null}。
 * 返回的 {@code HttpResponse} 未读取实体、也未关闭底层客户端，调用方需自行读取并释放。</p>
 *
 * <p>host 以 {@code https://} 开头时改用信任所有证书、接受任意主机名的客户端，不校验服务端身份。</p>
 */
public class HttpUtils {

    /**
     * 发送 GET 请求，查询参数拼进 URL。
     *
     * @param host 服务地址，形如 {@code https://host}；以 {@code https://} 开头时不校验服务端证书
     * @param path 请求路径，可为空白
     * @param method HTTP 方法名，本方法未使用
     * @param headers 请求头，不能为 {@code null}
     * @param querys 查询参数，可为 {@code null}
     * @return 原始响应，未读取实体
     * @throws Exception 请求执行失败时抛出
     */
    public static HttpResponse doGet(String host, String path, String method,
                                     Map<String, String> headers,
                                     Map<String, String> querys)
            throws Exception {
        HttpClient httpClient = wrapClient(host);

        HttpGet request = new HttpGet(buildUrl(host, path, querys));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        return httpClient.execute(request);
    }

    /**
     * 以表单形式提交 POST 请求，参数以 UTF-8 编码进请求体。
     *
     * @param host 服务地址，形如 {@code https://host}；以 {@code https://} 开头时不校验服务端证书
     * @param path 请求路径，可为空白
     * @param method HTTP 方法名，本方法未使用
     * @param headers 请求头，不能为 {@code null}
     * @param querys 查询参数，可为 {@code null}
     * @param bodys 表单参数；为 {@code null} 时不设置请求体
     * @return 原始响应，未读取实体
     * @throws Exception 请求执行失败时抛出
     */
    public static HttpResponse doPost(String host, String path, String method,
                                      Map<String, String> headers,
                                      Map<String, String> querys,
                                      Map<String, String> bodys)
            throws Exception {
        HttpClient httpClient = wrapClient(host);

        HttpPost request = new HttpPost(buildUrl(host, path, querys));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        if (bodys != null) {
            List<NameValuePair> nameValuePairList = new ArrayList<NameValuePair>();

            for (String key : bodys.keySet()) {
                nameValuePairList.add(new BasicNameValuePair(key, bodys.get(key)));
            }
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(nameValuePairList, "utf-8");
            formEntity.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
            request.setEntity(formEntity);
        }

        return httpClient.execute(request);
    }

    /**
     * 以字符串体提交 POST 请求，请求体编码为 UTF-8。
     *
     * @param host 服务地址，形如 {@code https://host}；以 {@code https://} 开头时不校验服务端证书
     * @param path 请求路径，可为空白
     * @param method HTTP 方法名，本方法未使用
     * @param headers 请求头，不能为 {@code null}
     * @param querys 查询参数，可为 {@code null}
     * @param body 请求体；为空白时不设置
     * @return 原始响应，未读取实体
     * @throws Exception 请求执行失败时抛出
     */
    public static HttpResponse doPost(String host, String path, String method,
                                      Map<String, String> headers,
                                      Map<String, String> querys,
                                      String body)
            throws Exception {
        HttpClient httpClient = wrapClient(host);

        HttpPost request = new HttpPost(buildUrl(host, path, querys));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        if (StringUtils.isNotBlank(body)) {
            request.setEntity(new StringEntity(body, "utf-8"));
        }

        return httpClient.execute(request);
    }

    /**
     * 以字节数组作为请求体提交 POST 请求。
     *
     * @param host 服务地址，形如 {@code https://host}；以 {@code https://} 开头时不校验服务端证书
     * @param path 请求路径，可为空白
     * @param method HTTP 方法名，本方法未使用
     * @param headers 请求头，不能为 {@code null}
     * @param querys 查询参数，可为 {@code null}
     * @param body 请求体；为 {@code null} 时不设置
     * @return 原始响应，未读取实体
     * @throws Exception 请求执行失败时抛出
     */
    public static HttpResponse doPost(String host, String path, String method,
                                      Map<String, String> headers,
                                      Map<String, String> querys,
                                      byte[] body)
            throws Exception {
        HttpClient httpClient = wrapClient(host);

        HttpPost request = new HttpPost(buildUrl(host, path, querys));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        if (body != null) {
            request.setEntity(new ByteArrayEntity(body));
        }

        return httpClient.execute(request);
    }

    /**
     * 以字符串体提交 PUT 请求，请求体编码为 UTF-8。
     *
     * @param host 服务地址，形如 {@code https://host}；以 {@code https://} 开头时不校验服务端证书
     * @param path 请求路径，可为空白
     * @param method HTTP 方法名，本方法未使用
     * @param headers 请求头，不能为 {@code null}
     * @param querys 查询参数，可为 {@code null}
     * @param body 请求体；为空白时不设置
     * @return 原始响应，未读取实体
     * @throws Exception 请求执行失败时抛出
     */
    public static HttpResponse doPut(String host, String path, String method,
                                     Map<String, String> headers,
                                     Map<String, String> querys,
                                     String body)
            throws Exception {
        HttpClient httpClient = wrapClient(host);

        HttpPut request = new HttpPut(buildUrl(host, path, querys));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        if (StringUtils.isNotBlank(body)) {
            request.setEntity(new StringEntity(body, "utf-8"));
        }

        return httpClient.execute(request);
    }

    /**
     * 以字节数组作为请求体提交 PUT 请求。
     *
     * @param host 服务地址，形如 {@code https://host}；以 {@code https://} 开头时不校验服务端证书
     * @param path 请求路径，可为空白
     * @param method HTTP 方法名，本方法未使用
     * @param headers 请求头，不能为 {@code null}
     * @param querys 查询参数，可为 {@code null}
     * @param body 请求体；为 {@code null} 时不设置
     * @return 原始响应，未读取实体
     * @throws Exception 请求执行失败时抛出
     */
    public static HttpResponse doPut(String host, String path, String method,
                                     Map<String, String> headers,
                                     Map<String, String> querys,
                                     byte[] body)
            throws Exception {
        HttpClient httpClient = wrapClient(host);

        HttpPut request = new HttpPut(buildUrl(host, path, querys));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        if (body != null) {
            request.setEntity(new ByteArrayEntity(body));
        }

        return httpClient.execute(request);
    }

    /**
     * 发送 DELETE 请求，查询参数拼进 URL。
     *
     * @param host 服务地址，形如 {@code https://host}；以 {@code https://} 开头时不校验服务端证书
     * @param path 请求路径，可为空白
     * @param method HTTP 方法名，本方法未使用
     * @param headers 请求头，不能为 {@code null}
     * @param querys 查询参数，可为 {@code null}
     * @return 原始响应，未读取实体
     * @throws Exception 请求执行失败时抛出
     */
    public static HttpResponse doDelete(String host, String path, String method,
                                        Map<String, String> headers,
                                        Map<String, String> querys)
            throws Exception {
        HttpClient httpClient = wrapClient(host);

        HttpDelete request = new HttpDelete(buildUrl(host, path, querys));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        return httpClient.execute(request);
    }

    private static String buildUrl(String host, String path, Map<String, String> querys) throws UnsupportedEncodingException {
        StringBuilder sbUrl = new StringBuilder();
        sbUrl.append(host);
        if (!StringUtils.isBlank(path)) {
            sbUrl.append(path);
        }
        if (null != querys) {
            StringBuilder sbQuery = new StringBuilder();
            for (Map.Entry<String, String> query : querys.entrySet()) {
                if (0 < sbQuery.length()) {
                    sbQuery.append("&");
                }
                if (StringUtils.isBlank(query.getKey()) && !StringUtils.isBlank(query.getValue())) {
                    sbQuery.append(query.getValue());
                }
                if (!StringUtils.isBlank(query.getKey())) {
                    sbQuery.append(query.getKey());
                    if (!StringUtils.isBlank(query.getValue())) {
                        sbQuery.append("=");
                        sbQuery.append(URLEncoder.encode(query.getValue(), "utf-8"));
                    }
                }
            }
            if (0 < sbQuery.length()) {
                sbUrl.append("?").append(sbQuery);
            }
        }

        return sbUrl.toString();
    }

    private static HttpClient wrapClient(String host) {
        HttpClient httpClient = new DefaultHttpClient();
        if (host.startsWith("https://")) {
            sslClient(httpClient);
        }

        return httpClient;
    }

    /**
     * 把客户端换成信任所有证书、接受任意主机名的 HTTPS 实现。
     *
     * @param httpClient 待改造的客户端
     */
    private static void sslClient(HttpClient httpClient) {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            X509TrustManager tm = new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                @Override
                public void checkClientTrusted(X509Certificate[] xcs, String str) {

                }
                @Override
                public void checkServerTrusted(X509Certificate[] xcs, String str) {

                }
            };
            ctx.init(null, new TrustManager[] { tm }, null);
            SSLSocketFactory ssf = new SSLSocketFactory(ctx);
            ssf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            ClientConnectionManager ccm = httpClient.getConnectionManager();
            SchemeRegistry registry = ccm.getSchemeRegistry();
            registry.register(new Scheme("https", 443, ssf));
        } catch (KeyManagementException ex) {
            throw new RuntimeException(ex);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

}
