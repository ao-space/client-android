package xyz.eulix.space.transfer.multipart.lan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;
import okio.Source;
import xyz.eulix.space.transfer.TransferProgressListener;
import xyz.eulix.space.util.Logger;

/**
 * Author:      Zhu Fuyu
 * Description: 局域网上传文件片段请求body
 * History:     2023/2/14
 */
public class LanUploadFileProgressRequestBody extends RequestBody {

    private final File file;
    private final long start;
    private final long length;
    private final MediaType mediaType;
    private BufferedSink bufferedSink;
    private long bytesWritten = 0L;
    private int oldPercent = 0; //上次进度
    private final TransferProgressListener progressListener;

    public LanUploadFileProgressRequestBody(File file, long start, long length, TransferProgressListener progressListener) {
        this.file = file;
        this.start = start;
        this.length = length;
        this.mediaType = MediaType.parse("application/octet-stream");
        this.progressListener = progressListener;
    }

    @Nullable
    @Override
    public MediaType contentType() {
        return mediaType;
    }

    @Override
    public long contentLength() throws IOException {
        return length;
    }

    @Override
    public void writeTo(@NonNull BufferedSink sink) throws IOException {
        if (bufferedSink == null) {
            bufferedSink = Okio.buffer(new ProgressBufferSink(sink));
        }
        Source source = null;
        try (InputStream input = new FileInputStream(file)) {
            if (start > 0) {
                input.skip(start);
            }
            source = Okio.source(input);
            bufferedSink.write(source, length);
            bufferedSink.flush();
        } finally {
            if (source != null) {
                source.close();
            }
        }
    }

    class ProgressBufferSink extends ForwardingSink {

        ProgressBufferSink(Sink delegate) {
            super(delegate);
        }

        @Override
        public void write(Buffer source, long byteCount) throws IOException {
            super.write(source, byteCount);
            bytesWritten += byteCount;
            if (progressListener != null) {
                boolean isPercentChange;
                int currentPercent = (int) (bytesWritten * 100 / length);
                if (currentPercent > oldPercent) {
                    isPercentChange = true;
                    oldPercent = currentPercent;
                } else {
                    isPercentChange = false;
                }
//                Logger.d("zfy", "LanUploadFileProgressRequestBody progress:" + currentPercent);
                progressListener.onProgress(bytesWritten, length, byteCount, isPercentChange, false);
            }
        }

    }
}
