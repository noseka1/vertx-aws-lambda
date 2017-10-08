package com.alesnosek;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class LambdaServerTest {

    Vertx vertx;
    LambdaServer server;
    ByteArrayOutputStream outputData;

    @Before
    public void before(TestContext context) {
        vertx = Vertx.vertx();
        InputStream inputData = this.getClass().getClassLoader().getResourceAsStream("request_basic.json");
        outputData = new ByteArrayOutputStream();
        server = new LambdaServer(vertx, null, inputData, outputData);
    }

    @After
    public void after(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testIsMetricsEnabled(TestContext context) {
        context.assertFalse(server.isMetricsEnabled());
    }

    @Test
    public void testRequestStream(TestContext context) {
        context.assertNull(server.requestStream());
    }

    @Test
    public void testRequestHandler(TestContext context) {
        Handler<HttpServerRequest> handler = new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest event) {
            }
        };
        context.assertEquals(server, server.requestHandler(handler));
        context.assertEquals(handler, server.requestHandler());
    }

    @Test
    public void testConnectionHandler(TestContext context) {
        context.assertEquals(server, server.connectionHandler(null));
    }

    @Test
    public void testWebsocketStream(TestContext context) {
        context.assertNull(server.websocketStream());
    }

    @Test
    public void testWebsocketHandler(TestContext context) {
        Handler<ServerWebSocket> handler = new Handler<ServerWebSocket>() {
            @Override
            public void handle(ServerWebSocket event) {
            }
        };
        context.assertEquals(server, server.websocketHandler(handler));
        context.assertEquals(handler, server.websocketHandler());
    }

    @Test
    public void testListen(TestContext context) {
        server.requestHandler(req -> {
            context.assertEquals("0.0.0.0", req.host());
            req.response().end("data");
        });
        context.assertEquals(server, server.listen());
        context.assertEquals(0, server.actualPort());

        JsonObject response = new JsonObject(outputData.toString());
        context.assertEquals("4", response.getJsonObject("headers").getString("Content-Length"));
        context.assertEquals(200, response.getInteger("statusCode"));
        context.assertTrue(response.getBoolean("isBase64Encoded"));
        context.assertEquals("data", new String(response.getBinary("body")));
    }

    @Test
    public void testListen2(TestContext context) {
        server.requestHandler(req -> {
            context.assertEquals("myhost", req.host());
            req.response().end("data2");
        });
        context.assertEquals(server, server.listen(8888, "myhost"));
        context.assertEquals(8888, server.actualPort());

        JsonObject response = new JsonObject(outputData.toString());
        context.assertEquals("data2", new String(response.getBinary("body")));
    }

    @Test
    public void testListen3(TestContext context) {
        Future<HttpServer> future = Future.future();
        server.requestHandler(req -> {
            context.assertEquals("myhost", req.host());
            req.response().end("data3");
        });
        context.assertEquals(server, server.listen(8888, "myhost", future));
        context.assertEquals(8888, server.actualPort());
        context.assertTrue(future.succeeded());
        context.assertEquals(server, future.result());

        JsonObject response = new JsonObject(outputData.toString());
        context.assertEquals("data3", new String(response.getBinary("body")));
    }

    @Test
    public void testListen4(TestContext context) {
        server.requestHandler(req -> {
            context.assertEquals("0.0.0.0", req.host());
            req.response().end("data4");
        });
        context.assertEquals(server, server.listen(8888));
        context.assertEquals(8888, server.actualPort());

        JsonObject response = new JsonObject(outputData.toString());
        context.assertEquals("data4", new String(response.getBinary("body")));
    }

    @Test
    public void testListen5(TestContext context) {
        Future<HttpServer> future = Future.future();
        server.requestHandler(req -> {
            context.assertEquals("0.0.0.0", req.host());
            req.response().end("data5");
        });
        context.assertEquals(server, server.listen(8888, future));
        context.assertEquals(8888, server.actualPort());
        context.assertTrue(future.succeeded());
        context.assertEquals(server, future.result());

        JsonObject response = new JsonObject(outputData.toString());
        context.assertEquals("data5", new String(response.getBinary("body")));
    }

    @Test
    public void testListen6(TestContext context) {
        Future<HttpServer> future = Future.future();
        server.requestHandler(req -> {
            context.assertEquals("0.0.0.0", req.host());
            req.response().end("data6");
        });
        context.assertEquals(server, server.listen(future));
        context.assertEquals(0, server.actualPort());
        context.assertTrue(future.succeeded());
        context.assertEquals(server, future.result());

        JsonObject response = new JsonObject(outputData.toString());
        context.assertEquals("data6", new String(response.getBinary("body")));
    }

    @Test
    public void testClose(TestContext context) {
        // test that no exception is thrown
        server.close();
    }

    @Test
    public void testClose2(TestContext context) {
        Future<Void> future = Future.future();
        server.close(future);
        context.assertTrue(future.succeeded());
    }
}
