package common.utils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * 通用 HTTP 工具：基于 Apache HttpClient 提供 GET 与 POST（表单 / 字符串体）请求，返回响应体字符串。
 *
 * <p>非 HTTPS 请求复用静态连接池，池内总连接数与单路由上限均为 128，可多线程共用；
 * HTTPS 请求每次新建一个跳过证书与主机名校验的 client，用完即关，因此不校验服务端身份。</p>
 */
public class HttpClientUtils {

	/** 默认建立连接超时，单位毫秒；{@code postParameters} 与 {@code get(url, charset)} 取该值。 */
	public static final int connTimeout=10000;
	/** 默认读取响应超时，单位毫秒；{@code postParameters} 与 {@code get(url, charset)} 取该值。 */
	public static final int readTimeout=10000;
	/** 默认字符编码，既用于请求体编码，也用于响应体解码。 */
	public static final String charset="UTF-8";
	/** 非 HTTPS 请求共用的连接池客户端，在静态块中初始化；HTTPS 请求另建客户端，不走该字段。 */
	private static HttpClient client = null;

	static {
		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
		cm.setMaxTotal(128);
		cm.setDefaultMaxPerRoute(128);
		client = HttpClients.custom().setConnectionManager(cm).build();
	}

	/**
	 * 以表单形式提交 POST 请求，超时取默认的 10 秒。
	 *
	 * @param url 请求地址
	 * @param parameterStr 表单请求体，形如 {@code a=1&b=2}
	 * @return 响应体，按 UTF-8 解码
	 * @throws ConnectTimeoutException 建立连接超时
	 * @throws SocketTimeoutException 读取响应超时
	 * @throws Exception 请求执行或响应读取失败
	 */
	public static String postParameters(String url, String parameterStr) throws ConnectTimeoutException, SocketTimeoutException, Exception{
		return post(url,parameterStr,"application/x-www-form-urlencoded",charset,connTimeout,readTimeout);
	}

	/**
	 * 以表单形式提交 POST 请求，可指定编码与超时。
	 *
	 * @param url 请求地址
	 * @param parameterStr 表单请求体，形如 {@code a=1&b=2}
	 * @param charset 请求体编码，同时用于解码响应体
	 * @param connTimeout 建立连接超时，单位毫秒；为 {@code null} 时不设置
	 * @param readTimeout 读取响应超时，单位毫秒；为 {@code null} 时不设置
	 * @return 响应体，按 {@code charset} 解码
	 * @throws ConnectTimeoutException 建立连接超时
	 * @throws SocketTimeoutException 读取响应超时
	 * @throws Exception 请求执行或响应读取失败
	 */
	public static String postParameters(String url, String parameterStr,String charset, Integer connTimeout, Integer readTimeout) throws ConnectTimeoutException, SocketTimeoutException, Exception{
		return post(url,parameterStr,"application/x-www-form-urlencoded",charset,connTimeout,readTimeout);
	}

	/**
	 * 以表单形式提交 POST 请求，参数以键值对传入，超时取默认的 10 秒。
	 *
	 * @param url 请求地址
	 * @param params 表单参数，编码为 UTF-8
	 * @return 响应体，按 UTF-8 解码
	 * @throws ConnectTimeoutException 建立连接超时
	 * @throws SocketTimeoutException 读取响应超时
	 * @throws Exception 请求执行或响应读取失败
	 */
	public static String postParameters(String url, Map<String, String> params) throws ConnectTimeoutException,
			SocketTimeoutException, Exception {
		return postForm(url, params, null, connTimeout, readTimeout);
	}

	/**
	 * 以表单形式提交 POST 请求，参数以键值对传入，可指定超时。
	 *
	 * @param url 请求地址
	 * @param params 表单参数，编码为 UTF-8
	 * @param connTimeout 建立连接超时，单位毫秒；为 {@code null} 时不设置
	 * @param readTimeout 读取响应超时，单位毫秒；为 {@code null} 时不设置
	 * @return 响应体，按 UTF-8 解码
	 * @throws ConnectTimeoutException 建立连接超时
	 * @throws SocketTimeoutException 读取响应超时
	 * @throws Exception 请求执行或响应读取失败
	 */
	public static String postParameters(String url, Map<String, String> params, Integer connTimeout,Integer readTimeout) throws ConnectTimeoutException,
			SocketTimeoutException, Exception {
		return postForm(url, params, null, connTimeout, readTimeout);
	}

	/**
	 * 发送 GET 请求。
	 *
	 * <p>两个超时都不设置，走 Apache 的默认值（不限时）；需要超时请改用 {@link #get(String, String)}，
	 * 它取 {@link #connTimeout} 与 {@link #readTimeout} 的默认值。</p>
	 *
	 * @param url 请求地址
	 * @return 响应体，按 UTF-8 解码
	 * @throws ConnectTimeoutException 建立连接超时
	 * @throws SocketTimeoutException 读取响应超时
	 * @throws Exception 请求执行或响应读取失败
	 */
	public static String get(String url) throws Exception {
		return get(url, charset, null, null);
	}

	/**
	 * 发送 GET 请求，指定响应体编码，超时取默认的 10 秒。
	 *
	 * @param url 请求地址
	 * @param charset 响应体编码
	 * @return 响应体，按 {@code charset} 解码
	 * @throws ConnectTimeoutException 建立连接超时
	 * @throws SocketTimeoutException 读取响应超时
	 * @throws Exception 请求执行或响应读取失败
	 */
	public static String get(String url, String charset) throws Exception {
		return get(url, charset, connTimeout, readTimeout);
	}

	/**
	 * 发送一个 Post 请求, 使用指定的字符集编码.
	 *
	 * @param url 请求地址
	 * @param body 请求体，形如 {@code a=1&b=2}；为空白时不设置实体
	 * @param mimeType 请求体的 Content-Type，例如 {@code application/xml}、{@code application/x-www-form-urlencoded}
	 * @param charset 请求体编码，同时用于解码响应体
	 * @param connTimeout 建立链接超时时间,毫秒；为 {@code null} 时不设置
	 * @param readTimeout 响应超时时间,毫秒；为 {@code null} 时不设置
	 * @return ResponseBody, 使用指定的字符集编码.
	 * @throws ConnectTimeoutException 建立链接超时异常
	 * @throws SocketTimeoutException  响应超时
	 * @throws Exception 请求执行或响应读取失败
	 */
	public static String post(String url, String body, String mimeType,String charset, Integer connTimeout, Integer readTimeout)
			throws ConnectTimeoutException, SocketTimeoutException, Exception {
		HttpClient client = null;
		HttpPost post = new HttpPost(url);
		String result = "";
		try {
			if (StringUtils.isNotBlank(body)) {
				HttpEntity entity = new StringEntity(body, ContentType.create(mimeType, charset));
				post.setEntity(entity);
			}
			Builder customReqConf = RequestConfig.custom();
			if (connTimeout != null) {
				customReqConf.setConnectTimeout(connTimeout);
			}
			if (readTimeout != null) {
				customReqConf.setSocketTimeout(readTimeout);
			}
			post.setConfig(customReqConf.build());

			HttpResponse res;
			if (url.startsWith("https")) {
				client = createSSLInsecureClient();
				res = client.execute(post);
			} else {
				client = HttpClientUtils.client;
				res = client.execute(post);
			}
			result = IOUtils.toString(res.getEntity().getContent(), charset);
		} finally {
			post.releaseConnection();
			if (url.startsWith("https") && client != null&& client instanceof CloseableHttpClient) {
				((CloseableHttpClient) client).close();
			}
		}
		return result;
	}


	/**
	 * 提交form表单
	 *
	 * @param url 请求地址
	 * @param params 表单参数，编码为 UTF-8；为空时不设置实体
	 * @param headers 附加请求头；为空时不追加
	 * @param connTimeout 建立连接超时，单位毫秒；为 {@code null} 时不设置
	 * @param readTimeout 读取响应超时，单位毫秒；为 {@code null} 时不设置
	 * @return 响应体，按 UTF-8 解码
	 * @throws ConnectTimeoutException 建立连接超时
	 * @throws SocketTimeoutException 读取响应超时
	 * @throws Exception 请求执行或响应读取失败
	 */
	public static String postForm(String url, Map<String, String> params, Map<String, String> headers, Integer connTimeout,Integer readTimeout) throws ConnectTimeoutException,
			SocketTimeoutException, Exception {

		HttpClient client = null;
		HttpPost post = new HttpPost(url);
		try {
			if (params != null && !params.isEmpty()) {
				List<NameValuePair> formParams = new ArrayList<NameValuePair>();
				Set<Entry<String, String>> entrySet = params.entrySet();
				for (Entry<String, String> entry : entrySet) {
					formParams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
				}
				UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParams, Consts.UTF_8);
				post.setEntity(entity);
			}

			if (headers != null && !headers.isEmpty()) {
				for (Entry<String, String> entry : headers.entrySet()) {
					post.addHeader(entry.getKey(), entry.getValue());
				}
			}
			Builder customReqConf = RequestConfig.custom();
			if (connTimeout != null) {
				customReqConf.setConnectTimeout(connTimeout);
			}
			if (readTimeout != null) {
				customReqConf.setSocketTimeout(readTimeout);
			}
			post.setConfig(customReqConf.build());
			HttpResponse res = null;
			if (url.startsWith("https")) {
				client = createSSLInsecureClient();
				res = client.execute(post);
			} else {
				client = HttpClientUtils.client;
				res = client.execute(post);
			}
			return IOUtils.toString(res.getEntity().getContent(), "UTF-8");
		} finally {
			post.releaseConnection();
			if (url.startsWith("https") && client != null
					&& client instanceof CloseableHttpClient) {
				((CloseableHttpClient) client).close();
			}
		}
	}




	/**
	 * 发送一个 GET 请求
	 *
	 * @param url 请求地址
	 * @param charset 响应体编码
	 * @param connTimeout  建立链接超时时间,毫秒；为 {@code null} 时不设置
	 * @param readTimeout  响应超时时间,毫秒；为 {@code null} 时不设置
	 * @return 响应体，按 {@code charset} 解码
	 * @throws ConnectTimeoutException   建立链接超时
	 * @throws SocketTimeoutException   响应超时
	 * @throws Exception 请求执行或响应读取失败
	 */
	public static String get(String url, String charset, Integer connTimeout,Integer readTimeout)
			throws ConnectTimeoutException,SocketTimeoutException, Exception {

		HttpClient client = null;
		HttpGet get = new HttpGet(url);
		String result = "";
		try {
			Builder customReqConf = RequestConfig.custom();
			if (connTimeout != null) {
				customReqConf.setConnectTimeout(connTimeout);
			}
			if (readTimeout != null) {
				customReqConf.setSocketTimeout(readTimeout);
			}
			get.setConfig(customReqConf.build());

			HttpResponse res = null;

			if (url.startsWith("https")) {
				client = createSSLInsecureClient();
				res = client.execute(get);
			} else {
				client = HttpClientUtils.client;
				res = client.execute(get);
			}

			result = IOUtils.toString(res.getEntity().getContent(), charset);
		} finally {
			get.releaseConnection();
			if (url.startsWith("https") && client != null && client instanceof CloseableHttpClient) {
				((CloseableHttpClient) client).close();
			}
		}
		return result;
	}


	/**
	 * 从响应头 Content-Type 里取出 charset 值。
	 *
	 * @param ressponse HTTP 响应
	 * @return Content-Type 中 charset 的值；响应无实体、无 Content-Type 或未声明 charset 时返回 {@code null}
	 */
	@SuppressWarnings("unused")
	private static String getCharsetFromResponse(HttpResponse ressponse) {
		// Content-Type:text/html; charset=GBK
		if (ressponse.getEntity() != null  && ressponse.getEntity().getContentType() != null && ressponse.getEntity().getContentType().getValue() != null) {
			String contentType = ressponse.getEntity().getContentType().getValue();
			if (contentType.contains("charset=")) {
				return contentType.substring(contentType.indexOf("charset=") + 8);
			}
		}
		return null;
	}



	/**
	 * 创建跳过证书与主机名校验的 HTTPS 客户端。
	 *
	 * @return 信任任意证书、接受任意主机名的客户端
	 * @throws GeneralSecurityException 构建 SSLContext 失败时抛出
	 */
	private static CloseableHttpClient createSSLInsecureClient() throws GeneralSecurityException {
		try {
			SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
				@Override
				public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
					return true;
				}
			}).build();

			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, new X509HostnameVerifier() {

				/** {@inheritDoc} */
				@Override
				public boolean verify(String arg0, SSLSession arg1) {
					return true;
				}

				/** {@inheritDoc} */
				@Override
				public void verify(String host, SSLSocket ssl)
						throws IOException {
				}

				/** {@inheritDoc} */
				@Override
				public void verify(String host, X509Certificate cert)
						throws SSLException {
				}

				/** {@inheritDoc} */
				@Override
				public void verify(String host, String[] cns,
								   String[] subjectAlts) throws SSLException {
				}

			});

			return HttpClients.custom().setSSLSocketFactory(sslsf).build();

		} catch (GeneralSecurityException e) {
			throw e;
		}
	}

	public static void main(String[] args) {
		try {
			String str= post("https://localhost:443/ssl/test.shtml","name=12&page=34","application/x-www-form-urlencoded", "UTF-8", 10000, 10000);
			System.out.println(str);
		} catch (ConnectTimeoutException e) {
			e.printStackTrace();
		} catch (SocketTimeoutException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}