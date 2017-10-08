package com.alesnosek.io.vertx.aws.lambda.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;

import com.alesnosek.io.vertx.aws.lambda.LambdaServer;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

public class SampleApp implements RequestStreamHandler {

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {

        // Lambda function is allowed write access to /tmp only
        System.setProperty("vertx.cacheDirBase", "/tmp/.vertx");

        Vertx vertx = Vertx.vertx();

        Router router = Router.router(vertx);

        router.route().handler(rc -> {
            LocalDateTime now = LocalDateTime.now();
            rc.response().putHeader("content-type", "text/html").end("Hello from Lambda at " + now);
        });

        LambdaServer server = new LambdaServer(vertx, context, input, output);

        server.requestHandler(router::accept).listen();
    }
}