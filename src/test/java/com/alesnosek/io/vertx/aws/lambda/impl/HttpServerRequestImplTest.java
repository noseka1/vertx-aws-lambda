package com.alesnosek.io.vertx.aws.lambda.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.net.ssl.SSLPeerUnverifiedException;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.alesnosek.io.vertx.aws.lambda.impl.HttpServerRequestImpl;
import com.alesnosek.io.vertx.aws.lambda.impl.HttpServerResponseImpl;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class HttpServerRequestImplTest {

    String localHost = "localhost";
    int localPort = 8888;
    HttpServerRequestImpl request;
    ByteArrayOutputStream responseOutput = new ByteArrayOutputStream();
    HttpServerResponseImpl response;

    static JsonObject requestBasic;
    static JsonObject requestPath;
    static JsonObject requestPathMultiple;

    @BeforeClass
    public static void beforeClass() throws IOException {
        requestBasic = loadData("request_basic.json");
        requestPath = loadData("request_path.json");
        requestPathMultiple = loadData("request_path_multiple.json");
    }

    @Before
    public void before(TestContext context) {
        response = new HttpServerResponseImpl(responseOutput);
        request = new HttpServerRequestImpl(localHost, localPort, requestBasic, response);
    }

    @Test
    public void testExceptionHandler(TestContext context) {
        context.assertEquals(request, request.exceptionHandler(ar -> {
        }));
    }

    @Test
    public void testHandler(TestContext context) {
        context.assertEquals(request, request.handler(ar -> {
        }));
    }

    @Test(expected = IllegalStateException.class)
    public void testHandlerEnded(TestContext context) {
        request.handleEnd();
        request.handler(ar -> {
        });
    }

    @Test
    public void testPause(TestContext context) {
        context.assertEquals(request, request.pause());
    }

    @Test
    public void testResume(TestContext context) {
        context.assertEquals(request, request.resume());
    }

    @Test
    public void testEndHandler(TestContext context) {
        context.assertEquals(request, request.endHandler(ar -> {
            request.response().end("data1");
        }));
        request.handleEnd();
        context.assertEquals("data1",
                new String(new JsonObject(Buffer.buffer(responseOutput.toByteArray())).getBinary("body")));
    }

    @Test(expected = IllegalStateException.class)
    public void testEndHandlerEnded(TestContext context) {
        request.handleEnd();
        request.endHandler(ar -> {
        });
    }

    @Test
    public void testVersion(TestContext context) {
        context.assertEquals(HttpVersion.HTTP_1_1, request.version());
    }

    @Test
    public void testMethod(TestContext context) {
        context.assertEquals(HttpMethod.POST, request.method());
    }

    @Test
    public void testRawMethod(TestContext context) {
        context.assertEquals("POST", request.rawMethod());
    }

    @Test
    public void testIsSSL(TestContext context) {
        context.assertFalse(request.isSSL());
    }

    @Test
    public void testScheme(TestContext context) {
        context.assertEquals("http", request.scheme());
    }

    @Test
    public void testUri(TestContext context) {
        context.assertEquals("http://localhost:8888?p1=1&p2=2", request.uri());
    }

    @Test
    public void testPath(TestContext context) {
        context.assertEquals("/", request.path());
    }

    @Test
    public void testQuery(TestContext context) {
        context.assertEquals("p1=1&p2=2", request.query());
    }

    @Test
    public void testHost(TestContext context) {
        context.assertEquals("localhost", request.host());
    }

    @Test
    public void testResponse(TestContext context) {
        context.assertEquals(response, request.response());
    }

    @Test
    public void testHeaders(TestContext context) {
        context.assertEquals("val1", request.headers().get("X-H1"));
    }

    @Test
    public void testGetHeader(TestContext context) {
        context.assertEquals("val1", request.getHeader("X-H1"));
    }

    @Test
    public void testParams(TestContext context) {
        MultiMap params = request.params();
        context.assertEquals(2, params.size());
        context.assertEquals("1", params.get("p1"));
        context.assertEquals("2", params.get("p2"));
    }

    @Test
    public void testGetParam(TestContext context) {
        context.assertEquals("1", request.getParam("p1"));
        context.assertEquals("2", request.getParam("p2"));
    }

    @Test
    public void testGetRemoteAddress(TestContext context) {
        SocketAddress address = request.remoteAddress();
        context.assertEquals("0.0.0.0", address.host());
        context.assertEquals(0, address.port());
    }

    @Test
    public void testLocalAddress(TestContext context) {
        SocketAddress address = request.localAddress();
        context.assertEquals("localhost", address.host());
        context.assertEquals(8888, address.port());
    }

    @Test
    public void testPeerCertificateChain(TestContext context) throws SSLPeerUnverifiedException {
        context.assertNull(request.peerCertificateChain());
    }

    @Test
    public void testAbsoluteURI(TestContext context) {
        context.assertEquals("http://localhost:8888?p1=1&p2=2", request.absoluteURI());
    }

    @Test
    public void testNetSocket(TestContext context) {
        context.assertNull(request.netSocket());
    }

    @Test(expected = java.lang.UnsupportedOperationException.class)
    public void testSetExpectMultipart(TestContext context) {
        request.setExpectMultipart(true);
    }

    @Test
    public void testIsExpectMultipart(TestContext context) {
        context.assertFalse(request.isExpectMultipart());
    }

    @Test
    public void testUploadHandler(TestContext context) {
        context.assertEquals(request, request.uploadHandler(p -> {
        }));
    }

    @Test
    public void testformAttributes(TestContext context) {
        MultiMap attr = request.formAttributes();
        context.assertEquals(0, attr.size());
    }

    @Test
    public void testGetFormAttribute(TestContext context) {
        context.assertNull(request.getFormAttribute("X"));
    }

    @Test(expected = java.lang.UnsupportedOperationException.class)
    public void testUpgrade(TestContext context) {
        request.upgrade();
    }

    @Test
    public void testIsEnded(TestContext context) {
        context.assertFalse(request.isEnded());
        request.handleEnd();
        context.assertTrue(request.isEnded());
    }

    @Test
    public void testCustomFrameHandler(TestContext context) {
        context.assertEquals(request, request.customFrameHandler(ar -> {
        }));
    }

    @Test
    public void testConnection(TestContext context) {
        context.assertNull(request.connection());
    }

    @Test
    public void testHandleDataRequestPath(TestContext context) {
        request = new HttpServerRequestImpl(localHost, localPort, requestPath, response);
        request.bodyHandler(buffer -> {
            context.assertEquals("request line 1\nrequest line 2\nrequest line 3", buffer.toString());
        });
        request.handleData();
        request.handleEnd();
        context.assertEquals("/path1", request.path());
        context.assertEquals("", request.query());
        context.assertTrue(request.headers().isEmpty());
        context.assertEquals("http://localhost:8888/path1", request.absoluteURI());
    }

    @Test
    public void testHandleDataRequestPathMultiple(TestContext context) {
        request = new HttpServerRequestImpl(localHost, localPort, requestPathMultiple, response);
        request.bodyHandler(buffer -> {
            context.assertEquals("", buffer.toString());
        });
        request.handleData();
        request.handleEnd();
        context.assertEquals("/path1/path2/path3", request.path());
        context.assertEquals("http://localhost:8888/path1/path2/path3", request.absoluteURI());
    }

    private static JsonObject loadData(String fileName) throws IOException {
        InputStream inputData = HttpServerRequestImplTest.class.getClassLoader().getResourceAsStream(fileName);

        Buffer inputBuffer;
        inputBuffer = inputStreamToBuffer(inputData);

        JsonObject inputJson;
        inputJson = new JsonObject(inputBuffer);
        return inputJson;
    }

    private static Buffer inputStreamToBuffer(InputStream input) throws IOException {
        byte[] data = new byte[1024];

        Buffer buffer = Buffer.buffer();
        while (((input.read(data, 0, data.length))) != -1) {
            buffer.appendBytes(data);
        }
        return buffer;
    }
}
