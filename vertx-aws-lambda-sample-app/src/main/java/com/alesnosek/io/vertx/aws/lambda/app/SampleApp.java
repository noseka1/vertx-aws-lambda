package com.alesnosek.io.vertx.aws.lambda.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.concurrent.Semaphore;

import com.alesnosek.io.vertx.aws.lambda.LambdaServer;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Router;

public class SampleApp implements RequestStreamHandler {

    private Router router;
    private final Semaphore responseEnd = new Semaphore(0, true);

    /**
     * This is a handler method called by the AWS Lambda runtime
     */
    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {

        // Lambda function is allowed write access to /tmp only
        System.setProperty("vertx.cacheDirBase", "/tmp/.vertx");

        Vertx vertx = Vertx.vertx();

        router = Router.router(vertx);

        router.route().handler(rc -> {
            LocalDateTime now = LocalDateTime.now();
            rc.response().putHeader("content-type", "text/html").end("Hello from Lambda at " + now);
        });

        // create a LambdaServer which will process a single HTTP request
        LambdaServer server = new LambdaServer(vertx, context, input, output);

        // trigger the HTTP request processing
        server.requestHandler(this::handleRequest).listen();

        // block the main thread until the request has been fully processed
        waitForResponseEnd();
    }

    /**
     * This method will block the calling thread until the HTTP request has been
     * processed
     */
    public void waitForResponseEnd() {
        while (true) {
            try {
                responseEnd.acquire();
                return;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleRequest(HttpServerRequest request) {
        request.response().endHandler(this::handleResponseEnd);
        router.accept(request);
    }

    private void handleResponseEnd(Void v) {
        responseEnd.release();
    }
}