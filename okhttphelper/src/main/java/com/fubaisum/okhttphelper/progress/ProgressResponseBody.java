package com.fubaisum.okhttphelper.progress;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.ResponseBody;

import java.io.IOException;

import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

/**
 * 包装的响体，回调相应进度
 * User:lizhangqu(513163535@qq.com)
 * Date:2015-09-02
 * Time: 17:18
 */
class ProgressResponseBody extends ResponseBody {
    //实际的待包装响应体
    private final ResponseBody responseBody;
    //进度回调接口
    private final OkHttpProgressListener progressListener;
    //包装完成的BufferedSource
    private BufferedSource bufferedSource;

    /**
     * 构造函数
     *
     * @param responseBody     待包装的响应体
     * @param progressListener 回调接口
     */
    public ProgressResponseBody(ResponseBody responseBody, OkHttpProgressListener progressListener) {
        this.responseBody = responseBody;
        this.progressListener = progressListener;
    }


    /**
     * 重写调用实际的响应体的contentType
     *
     * @return MediaType
     */
    @Override
    public MediaType contentType() {
        return responseBody.contentType();
    }

    /**
     * 重写调用实际的响应体的contentLength
     *
     * @return contentLength
     * @throws IOException 异常
     */
    @Override
    public long contentLength() throws IOException {
        return responseBody.contentLength();
    }

    /**
     * 重写进行包装source
     *
     * @return BufferedSource
     * @throws IOException 异常
     */
    @Override
    public BufferedSource source() throws IOException {
        if (bufferedSource == null) {
            //包装
            bufferedSource = Okio.buffer(source(responseBody.source()));
        }
        return bufferedSource;
    }

    /**
     * 读取，回调进度接口
     *
     * @param source Source
     * @return Source
     */
    private Source source(Source source) {

        return new ForwardingSource(source) {
            //当前读取字节数
            long totalReadBytesCount = 0L;
            //总字节长度，避免多次调用contentLength()方法
            long totalBytesCount = 0L;

            @Override
            public long read(Buffer sink, long byteCount) throws IOException {
                //下次读取的字节数
                long nextReadBytesCount = super.read(sink, byteCount);
                //增加当前读取的字节数，如果读取完成了nextReadBytesCount会返回-1
                totalReadBytesCount += nextReadBytesCount != -1 ? nextReadBytesCount : 0;

                //可能的总字节数，如果contentLength()不知道长度，会返回-1
                if (totalBytesCount == 0) {
                    totalBytesCount = contentLength();
                }

                progressListener.onProgress(totalReadBytesCount, totalBytesCount);

                return nextReadBytesCount;
            }
        };
    }
}