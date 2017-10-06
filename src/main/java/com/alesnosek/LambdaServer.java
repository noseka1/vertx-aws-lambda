package com.alesnosek;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.amazonaws.services.lambda.runtime.Context;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.streams.ReadStream;

public class LambdaServer implements HttpServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(LambdaServer.class);

    private final Vertx vertx;
    private final Context context;
    private final InputStream input;
    private final OutputStream output;

    private Handler<HttpServerRequest> requestHandler;
    private Handler<ServerWebSocket> websocketHandler;

    private String localHost = "0.0.0.0";
    private int localPort = 0;

    public LambdaServer(Vertx vertx, Context context, InputStream input, OutputStream output) {
        this.vertx = vertx;
        this.context = context;
        this.input = input;
        this.output = output;
    }

    @Override
    public boolean isMetricsEnabled() {
        return false;
    }

    @Override
    public ReadStream<HttpServerRequest> requestStream() {
        return null;
    }

    @Override
    public HttpServer requestHandler(Handler<HttpServerRequest> handler) {
        this.requestHandler = handler;
        return this;
    }

    @Override
    public Handler<HttpServerRequest> requestHandler() {
        return requestHandler;
    }

    @Override
    public HttpServer connectionHandler(Handler<HttpConnection> handler) {
        return this;
    }

    @Override
    public ReadStream<ServerWebSocket> websocketStream() {
        return null;
    }

    @Override
    public HttpServer websocketHandler(Handler<ServerWebSocket> handler) {
        this.websocketHandler = handler;
        return this;
    }

    @Override
    public Handler<ServerWebSocket> websocketHandler() {
        return websocketHandler;
    }

    @Override
    public HttpServer listen() {
        processRequest();
        return this;
    }

    @Override
    public HttpServer listen(int port, String host) {
        localPort = port;
        localHost = host;
        processRequest();
        return this;
    }

    @Override
    public HttpServer listen(int port, String host, Handler<AsyncResult<HttpServer>> listenHandler) {
        localPort = port;
        localHost = host;
        listenHandler.handle(Future.succeededFuture(this));
        processRequest();
        return this;
    }

    @Override
    public HttpServer listen(int port) {
        localPort = port;
        processRequest();
        return this;
    }

    @Override

    public HttpServer listen(int port, Handler<AsyncResult<HttpServer>> listenHandler) {
        localPort = port;
        listenHandler.handle(Future.succeededFuture(this));
        processRequest();
        return this;
    }

    @Override
    public HttpServer listen(Handler<AsyncResult<HttpServer>> listenHandler) {
        listenHandler.handle(Future.succeededFuture(this));
        processRequest();
        return this;
    }

    @Override
    public void close() {
        // nothing to do here
    }

    @Override
    public void close(Handler<AsyncResult<Void>> completionHandler) {
        completionHandler.handle(Future.succeededFuture());
    }

    @Override
    public int actualPort() {
        return localPort;
    }

    private void processRequest() {
        HttpServerResponse response = new HttpServerResponseImpl(output);
        Buffer inputBuffer;
        try {
            inputBuffer = inputStreamToBuffer(input);
        } catch (IOException e) {
            String msg = "Failed to read the Lambda request";
            LOGGER.error(msg, e);
            errorResponse(msg);
            return;
        }

        JsonObject inputJson;
        try {
            inputJson = new JsonObject(inputBuffer);
        } catch (DecodeException e) {
            String msg = "Failed to decode the Lambda request";
            LOGGER.error(msg, e);
            errorResponse(msg);
            return;
        }
        HttpServerRequestImpl request = new HttpServerRequestImpl(localHost, localPort, inputJson, response);

        if (requestHandler != null) {
            requestHandler.handle(request);
        }
        request.handleData();
        request.handleEnd();
    }

    private Buffer inputStreamToBuffer(InputStream input) throws IOException {
        byte[] data = new byte[1024];

        Buffer buffer = Buffer.buffer();
        while (((input.read(data, 0, data.length))) != -1) {
            buffer.appendBytes(data);
        }
        return buffer;
    }

    private void errorResponse(String msg) {
        JsonObject outputJson = new JsonObject();

        outputJson.put("headers", new JsonObject());
        outputJson.put("statusCode", 500);
        outputJson.put("isBase64Encoded", false);
        outputJson.put("body", msg);

        try {
            output.write(outputJson.toBuffer().getBytes());
        } catch (IOException e) {
            LOGGER.error("Failed to write the Lambda response", e);
        }

        try {
            output.close();
        } catch (IOException e) {
            LOGGER.error("Failed to close the Lambda response stream", e);
        }
    }

}
