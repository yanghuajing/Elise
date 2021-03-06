package site.zido.elise.downloader;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import site.zido.elise.custom.GlobalConfig;
import site.zido.elise.custom.HttpClientConfig;
import site.zido.elise.downloader.httpclient.HttpClientHeaderWrapper;
import site.zido.elise.http.Http;
import site.zido.elise.http.HttpRequestBody;
import site.zido.elise.http.Request;
import site.zido.elise.http.Response;
import site.zido.elise.http.impl.DefaultResponse;
import site.zido.elise.select.Html;
import site.zido.elise.select.Text;
import site.zido.elise.task.Task;
import site.zido.elise.utils.HtmlUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Downloader using http client framework
 *
 * @author zido
 */
public class HttpClientDownloader implements Downloader {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientDownloader.class);
    private CloseableHttpClient client;
    private ConcurrentHashMap<Long, HttpClientContext> contextContainer = new ConcurrentHashMap<>();

    public HttpClientDownloader(CloseableHttpClient client) {
        this.client = client;
    }

    @Override
    public Response download(Task task, Request request) {
        CloseableHttpResponse httpResponse = null;
        HttpClientContext context = getContext(task);
        HttpUriRequest httpUriRequest = buildRequest(task, request);
        DefaultResponse response = DefaultResponse.fail();
        try {
            httpResponse = client.execute(httpUriRequest, context);
            response = handleResponse(request, task, httpResponse);
            LOGGER.debug("downloading response success {}", request.getUrl());
            return response;
        } catch (IOException e) {
            LOGGER.error("download response {} error", request.getUrl(), e);
            return response;
        } finally {
            if (httpResponse != null) {
                EntityUtils.consumeQuietly(httpResponse.getEntity());
                try {
                    httpResponse.close();
                } catch (IOException e) {
                    LOGGER.error("http response close failed", e);
                }
            }
        }
    }

    private DefaultResponse handleResponse(Request request, Task task, HttpResponse httpResponse) throws IOException {
        byte[] bytes = EntityUtils.toByteArray(httpResponse.getEntity());
        String contentType = httpResponse.getEntity().getContentType() == null ? "" : httpResponse.getEntity().getContentType().getValue();

        DefaultResponse response = new DefaultResponse();
        response.setContentType(Http.ContentType.parse(contentType));
        String charset = HtmlUtils.getHtmlCharset(bytes, task.getConfig().get(GlobalConfig.KEY_CHARSET));
        response.setBody(new Html(new String(bytes, charset), request.getUrl()));
        response.setUrl(new Text(request.getUrl()));
        response.setStatusCode(httpResponse.getStatusLine().getStatusCode());
        response.setDownloadSuccess(true);
        return response;
    }

    /**
     * Generate a context for the task
     *
     * @param task the task
     * @return context
     */
    private HttpClientContext getContext(Task task) {
        return contextContainer.computeIfAbsent(task.getId(), key -> {
            final HttpClientContext context = HttpClientContext.create();
            final HttpClientConfig config = new HttpClientConfig(task.getConfig());
            boolean disableCookie = config.getDisableCookie();
            if (disableCookie) {
                context.setCookieSpecRegistry(name -> null);
            }
            context.setCookieStore(new BasicCookieStore());
            return context;
        });
    }

    private HttpUriRequest buildRequest(Task task, Request request) {
        RequestBuilder builder = RequestBuilder.create(request.getMethod());
        final HttpRequestBody body = request.getBody();
        if (body != null) {
            ByteArrayEntity bodyEntity = new ByteArrayEntity(body.getBytes());
            bodyEntity.setContentType(body.getContentType().toString());
            builder.setEntity(bodyEntity);
        }
        final HttpClientConfig config = new HttpClientConfig(task.getConfig());
        final String charset = config.getCharset();
        if (charset != null) {
            builder.setCharset(Charset.forName(charset));
        }
        builder.setUri(request.getUrl());
        if (config.getHeaders() != null) {
            for (site.zido.elise.http.Header header : config.getHeaders()) {
                builder.addHeader(new HttpClientHeaderWrapper(header));
            }
        }
        return builder.build();
    }
}
