package com.hnqc.ironhand.downloader.service.impl;

import com.hnqc.ironhand.downloader.exceptions.DownloadException;
import com.hnqc.ironhand.downloader.service.IDownloadService;
import com.hnqc.ironhand.spider.utils.StatusCode;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

/**
 * 下载服务实现类
 *
 * @author zido
 * @date 2018/34/12
 */
@Service
public class DownloadServiceImpl implements IDownloadService {
    private HttpClient httpClient;

    public DownloadServiceImpl() {
        httpClient = HttpClients.createDefault();
    }

    @Override
    public String download(String url) {
        HttpGet httpGet = new HttpGet(url);
        try {
            HttpResponse resp = httpClient.execute(httpGet);
            StatusLine line = resp.getStatusLine();
            int code = line.getStatusCode();
            if (StatusCode.isSuccess(code)) {
                HttpEntity entity = resp.getEntity();
                return EntityUtils.toString(entity);
            } else {
                throw new DownloadException(String.format("响应状态错误：[%s]:%d", url, code));
            }
        } catch (IOException e) {
            throw new DownloadException(String.format("建立http连接异常:[%s]", url));
        }
    }

    @Override
    public InputStream getStream(String url) {
        HttpGet httpGet = new HttpGet(url);
        try {
            HttpResponse response = httpClient.execute(httpGet);
            StatusLine statusLine = response.getStatusLine();
            int code = statusLine.getStatusCode();
            if (StatusCode.isSuccess(code)) {
                HttpEntity entity = response.getEntity();
                return entity.getContent();
            } else {
                throw new DownloadException(String.format("响应状态错误：[%s]:%d", url, code));
            }
        } catch (IOException e) {
            throw new DownloadException(String.format("建立http连接异常:[%s]", url));
        }
    }
}