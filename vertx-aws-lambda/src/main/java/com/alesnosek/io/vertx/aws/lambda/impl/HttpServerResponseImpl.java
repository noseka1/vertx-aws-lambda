package com.alesnosek.io.vertx.aws.lambda.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;

public class HttpServerResponseImpl implements HttpServerResponse {

    private final OutputStream output;

    private boolean headWritten;
    private boolean written;
    private Handler<Throwable> exceptionHandler;
    private Handler<Void> endHandler;
    private Handler<Void> headersEndHandler;
    private Handler<Void> bodyEndHandler;
    private boolean chunked;
    private boolean closed;
    private MultiMap headers;
    private MultiMap trailers;
    int statusCode = 200;
    String statusMessage = "OK";

    Buffer body;

    public HttpServerResponseImpl(OutputStream output) {
        this.output = output;
    }

    @Override
    public boolean writeQueueFull() {
        checkWritten();
        return false;
    }

    @Override
    public HttpServerResponse exceptionHandler(Handler<Throwable> handler) {
        checkWritten();
        this.exceptionHandler = handler;
        return this;
    }

    @Override
    public HttpServerResponse setWriteQueueMaxSize(int maxSize) {
        checkWritten();
        return this;
    }

    @Override
    public HttpServerResponse drainHandler(Handler<Void> handler) {
        checkWritten();
        return this;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public HttpServerResponse setStatusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    @Override
    public String getStatusMessage() {
        return statusMessage;
    }

    @Override
    public HttpServerResponse setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
        return this;
    }

    @Override
    public HttpServerResponse setChunked(boolean chunked) {
        checkWritten();
        this.chunked = chunked;
        return this;
    }

    @Override
    public boolean isChunked() {
        return chunked;
    }

    @Override
    public MultiMap headers() {
        if (headers == null) {
            headers = MultiMap.caseInsensitiveMultiMap();
        }
        return headers;
    }

    @Override
    public HttpServerResponse putHeader(String name, String value) {
        checkWritten();
        headers().set(name, value);
        return this;
    }

    @Override
    public HttpServerResponse putHeader(CharSequence name, CharSequence value) {
        checkWritten();
        headers().set(name, value);
        return this;
    }

    @Override
    public HttpServerResponse putHeader(String name, Iterable<String> values) {
        checkWritten();
        headers().set(name, values);
        return this;
    }

    @Override
    public HttpServerResponse putHeader(CharSequence name, Iterable<CharSequence> values) {
        checkWritten();
        headers().set(name, values);
        return this;
    }

    @Override
    public MultiMap trailers() {
        if (trailers == null) {
            trailers = MultiMap.caseInsensitiveMultiMap();
        }
        return trailers;
    }

    @Override
    public HttpServerResponse putTrailer(String name, String value) {
        checkWritten();
        trailers().set(name, value);
        return this;
    }

    @Override
    public HttpServerResponse putTrailer(CharSequence name, CharSequence value) {
        checkWritten();
        trailers().set(name, value);
        return this;
    }

    @Override
    public HttpServerResponse putTrailer(String name, Iterable<String> values) {
        checkWritten();
        trailers().set(name, values);
        return this;
    }

    @Override
    public HttpServerResponse putTrailer(CharSequence name, Iterable<CharSequence> value) {
        checkWritten();
        trailers().set(name, value);
        return this;
    }

    @Override
    public HttpServerResponse closeHandler(Handler<Void> handler) {
        return this;
    }

    @Override
    public HttpServerResponse endHandler(Handler<Void> handler) {
        checkWritten();
        this.endHandler = handler;
        return this;
    }

    @Override
    public HttpServerResponse write(String chunk, String enc) {
        return write(Buffer.buffer(chunk, enc));
    }

    @Override
    public HttpServerResponse write(String chunk) {
        return write(Buffer.buffer(chunk));
    }

    @Override
    public HttpServerResponse write(Buffer data) {
        checkWritten();
        if (!headWritten && !chunked && !contentLengthSet()) {
            throw new IllegalStateException(
                    "You must set the Content-Length header to be the total size of the message "
                            + "body BEFORE sending any data if you are not using HTTP chunked encoding.");
        }

        if (!headWritten) {
            prepareHeaders();
        }
        body().appendBuffer(data);
        return this;
    }

    @Override
    public HttpServerResponse writeContinue() {
        return this;
    }

    @Override
    public void end(String chunk) {
        end(Buffer.buffer(chunk));
    }

    @Override
    public void end(String chunk, String enc) {
        end(Buffer.buffer(chunk, enc));
    }

    @Override
    public void end(Buffer chunk) {
        checkWritten();
        if (!chunked && !contentLengthSet()) {
            headers().set(HttpHeaders.CONTENT_LENGTH, String.valueOf(chunk.length()));
        }
        body().appendBuffer(chunk);
        if (!headWritten) {
            prepareHeaders();
        }

        outputResponse();

        closed = true;
        written = true;

        if (bodyEndHandler != null) {
            bodyEndHandler.handle(null);
        }
        if (endHandler != null) {
            endHandler.handle(null);
        }
    }

    @Override
    public void end() {
        end(Buffer.buffer());
    }

    @Override
    public HttpServerResponse sendFile(String filename, long offset, long length) {
        throw new java.lang.UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public HttpServerResponse sendFile(String filename, long offset, long length,
            Handler<AsyncResult<Void>> resultHandler) {
        throw new java.lang.UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public boolean ended() {
        return written;
    }

    @Override
    public boolean closed() {
        return closed;
    }

    @Override
    public boolean headWritten() {
        return headWritten;
    }

    @Override
    public HttpServerResponse headersEndHandler(Handler<Void> handler) {
        this.headersEndHandler = handler;
        return this;
    }

    @Override
    public HttpServerResponse bodyEndHandler(Handler<Void> handler) {
        this.bodyEndHandler = handler;
        return this;
    }

    @Override
    public long bytesWritten() {
        return body().length();
    }

    @Override
    public int streamId() {
        return -1;
    }

    @Override
    public void reset(long code) {
        // nothing to do here
    }

    @Override
    public HttpServerResponse push(HttpMethod method, String path, MultiMap headers,
            Handler<AsyncResult<HttpServerResponse>> handler) {
        return push(method, null, path, headers, handler);
    }

    @Override
    public HttpServerResponse push(io.vertx.core.http.HttpMethod method, String host, String path,
            Handler<AsyncResult<HttpServerResponse>> handler) {
        return push(method, path, handler);
    }

    @Override
    public HttpServerResponse push(HttpMethod method, String path, Handler<AsyncResult<HttpServerResponse>> handler) {
        return push(method, path, null, null, handler);
    }

    @Override
    public HttpServerResponse push(HttpMethod method, String host, String path, MultiMap headers,
            Handler<AsyncResult<HttpServerResponse>> handler) {
        handler.handle(Future.failedFuture("Push promise is only supported with HTTP2"));
        return this;
    }

    @Override
    public HttpServerResponse writeCustomFrame(int type, int flags, Buffer payload) {
        return this;
    }

    void handleException(Throwable t) {
        if (exceptionHandler != null) {
            exceptionHandler.handle(t);
        }
    }

    private void checkWritten() {
        if (written) {
            throw new IllegalStateException("Response has already been written");
        }
    }

    private boolean contentLengthSet() {
        if (headers == null) {
            return false;
        }
        return headers().contains(HttpHeaders.CONTENT_LENGTH);
    }

    private void prepareHeaders() {
        if (headersEndHandler != null) {
            headersEndHandler.handle(null);
        }
        headWritten = true;
    }

    private Buffer body() {
        if (body == null) {
            body = Buffer.buffer();
        }
        return body;
    }

    private void outputResponse() {
        JsonObject outputJson = new JsonObject();

        JsonObject outputHeaders = new JsonObject();
        if (headers != null) {
            for (Map.Entry<String, String> header : headers) {
                outputHeaders.put(header.getKey(), header.getValue());
            }
        }
        if (chunked) {
            if (trailers != null) {
                for (Map.Entry<String, String> trailer : trailers) {
                    outputHeaders.put(trailer.getKey(), trailer.getValue());
                }
            }
            outputHeaders.put(HttpHeaders.CONTENT_LENGTH.toString(), String.valueOf(body.length()));
        }

        outputJson.put("headers", outputHeaders);
        outputJson.put("statusCode", statusCode);
        outputJson.put("isBase64Encoded", true);
        outputJson.put("body", body().getBytes());

        try {
            output.write(outputJson.toBuffer().getBytes());
        } catch (IOException e) {
            handleException(e);
        }

        try {
            output.close();
        } catch (IOException e) {
            handleException(e);
        }
    }
}