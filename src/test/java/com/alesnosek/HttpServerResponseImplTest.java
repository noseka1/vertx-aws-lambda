package com.alesnosek;

import java.io.ByteArrayOutputStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class HttpServerResponseImplTest {

    ByteArrayOutputStream responseOutput;
    HttpServerResponseImpl response;

    @Before
    public void before(TestContext context) {
        responseOutput = new ByteArrayOutputStream();
        response = new HttpServerResponseImpl(responseOutput);
    }

    @Test
    public void testWriteQueueFull(TestContext context) {
        context.assertFalse(response.writeQueueFull());
    }

    @Test(expected = java.lang.IllegalStateException.class)
    public void testWriteQueueFullException(TestContext context) {
        response.end();
        response.writeQueueFull();
    }

    @Test
    public void testExceptionHandler(TestContext context) {
        context.assertEquals(response, response.exceptionHandler(t -> {
            context.assertEquals("my exception", t.getMessage());
        }));
        response.handleException(new Exception("my exception"));
    }

    @Test(expected = java.lang.IllegalStateException.class)
    public void testExceptionHandlerException(TestContext context) {
        response.end();
        response.exceptionHandler(t -> {
        });
    }

    @Test
    public void testSetWriteQueueMaxSize(TestContext context) {
        context.assertEquals(response, response.setWriteQueueMaxSize(100));
    }

    @Test(expected = java.lang.IllegalStateException.class)
    public void testSetWriteQueueMaxSizeException(TestContext context) {
        response.end();
        response.setWriteQueueMaxSize(200);
    }

    @Test
    public void testDrainHandler(TestContext context) {
        context.assertEquals(response, response.drainHandler(t -> {
        }));
    }

    @Test(expected = java.lang.IllegalStateException.class)
    public void testDrainHandlerException(TestContext context) {
        response.end();
        response.drainHandler(t -> {
        });
    }

    @Test
    public void testStatusCode(TestContext context) {
        context.assertEquals(200, response.getStatusCode());
        context.assertEquals(response, response.setStatusCode(300));
        context.assertEquals(300, response.getStatusCode());
    }

    @Test
    public void testGetStatusMessage(TestContext context) {
        context.assertEquals("OK", response.getStatusMessage());
        context.assertEquals(response, response.setStatusMessage("My message"));
        context.assertEquals("My message", response.getStatusMessage());
    }

    @Test
    public void testSetChunked(TestContext context) {
        context.assertEquals(response, response.setChunked(true));
        context.assertTrue(response.isChunked());
    }

    @Test(expected = java.lang.IllegalStateException.class)
    public void testSetChunkedException(TestContext context) {
        response.end();
        response.setChunked(true);
    }

    @Test
    public void testHeaders(TestContext context) {
        MultiMap headers = response.headers();
        context.assertEquals(response, response.putHeader("X-1", "value one"));
        context.assertEquals(response, response.putHeader("X-2", "value two"));
        context.assertEquals("value one", headers.get("X-1"));
        context.assertEquals("value two", headers.get("X-2"));
    }

    @Test
    public void testTrailers(TestContext context) {
        MultiMap trailers = response.trailers();
        context.assertEquals(response, response.putTrailer("X-3", "value three"));
        context.assertEquals(response, response.putTrailer("X-4", "value four"));
        context.assertEquals("value three", trailers.get("X-3"));
        context.assertEquals("value four", trailers.get("X-4"));
    }

    @Test
    public void testCloseHandler(TestContext context) {
        context.assertEquals(response, response.closeHandler(v -> {
        }));
    }

    @Test
    public void testEndHandler(TestContext context) {
        StringBuilder res = new StringBuilder();
        context.assertEquals(response, response.endHandler(v -> {
            res.append("DONE");
        }));
        response.end();
        context.assertEquals("DONE", res.toString());
    }

    @Test(expected = java.lang.IllegalStateException.class)
    public void testEndHandlerException(TestContext context) {
        response.end();
        response.endHandler(v -> {
        });
    }

    @Test(expected = java.lang.IllegalStateException.class)
    public void testWriteException(TestContext context) {
        response.write("Some data");
    }

    @Test
    public void testWrite(TestContext context) {
        String data = "Some data to write";
        int dataLength = data.length();
        response.putHeader("Content-Length", Integer.toString(dataLength));
        context.assertEquals(response, response.write("Some data"));
        response.end();
        JsonObject output = readOuput();
        context.assertTrue(output.getBoolean("isBase64Encoded"));
        context.assertEquals(200, output.getInteger("statusCode"));
        context.assertEquals(Integer.toString(dataLength), output.getJsonObject("headers").getString("Content-Length"));
        context.assertEquals("Some data", new String(output.getBinary("body")));
    }

    @Test
    public void testWriteMultiple(TestContext context) {
        String data1 = "Data1";
        int dataLength1 = data1.length();
        String data2 = "Data2";
        int dataLength2 = data2.length();
        String data3 = "Data3";
        int dataLength3 = data3.length();

        int dataLengthTotal = dataLength1 + dataLength2 + dataLength3;

        response.putHeader("Content-Length", Integer.toString(dataLengthTotal));
        response.write(data1);
        response.write(data2);
        response.end(data3);

        JsonObject output = readOuput();
        context.assertEquals(Integer.toString(dataLengthTotal),
                output.getJsonObject("headers").getString("Content-Length"));
        context.assertEquals("Data1Data2Data3", new String(output.getBinary("body")));
    }

    @Test
    public void testWriteContinue(TestContext context) {
        context.assertEquals(response, response.writeContinue());
    }

    @Test
    public void testEnd(TestContext context) {
        String data = "End the response";
        int dataLength = data.length();
        response.end(data);

        JsonObject output = readOuput();
        context.assertEquals(Integer.toString(dataLength), output.getJsonObject("headers").getString("Content-Length"));
        context.assertEquals(data, new String(output.getBinary("body")));
    }

    @Test(expected = java.lang.UnsupportedOperationException.class)
    public void testSendFileException1(TestContext context) {
        response.sendFile("X");
    }

    @Test(expected = java.lang.UnsupportedOperationException.class)
    public void testSendFileException2(TestContext context) {
        response.sendFile("Y", 0, 10, ar -> {
        });
    }

    @Test
    public void testClose(TestContext context) {
        context.assertFalse(response.closed());
        response.close();
        context.assertTrue(response.closed());
    }

    @Test
    public void testEnded(TestContext context) {
        context.assertFalse(response.ended());
        response.end();
        context.assertTrue(response.ended());
    }

    @Test
    public void testHeadWritten(TestContext context) {
        context.assertFalse(response.headWritten());
        response.putHeader("Content-Length", "0");
        response.write("");
        context.assertTrue(response.headWritten());
    }

    @Test
    public void testHeadersEndHandler(TestContext context) {
        StringBuilder res = new StringBuilder();
        context.assertEquals(response, response.headersEndHandler(v -> {
            res.append("DONE");
        }));
        response.putHeader("Content-Length", "0");
        context.assertEquals("", res.toString());
        response.write("");
        context.assertEquals("DONE", res.toString());
    }

    @Test
    public void testBodyEndHandler(TestContext context) {
        StringBuilder res = new StringBuilder();
        context.assertEquals(response, response.bodyEndHandler(v -> {
            res.append("DONE");
        }));
        context.assertEquals("", res.toString());
        response.end();
        context.assertEquals("DONE", res.toString());
    }

    @Test
    public void testBytesWritten(TestContext context) {
        String data = "Some response data";
        int dataLength = data.length();
        response.end(data);
        JsonObject output = readOuput();
        context.assertEquals((long) dataLength, response.bytesWritten());
        context.assertEquals(Integer.toString(dataLength), output.getJsonObject("headers").getString("Content-Length"));
    }

    @Test
    public void testStreamId(TestContext context) {
        context.assertEquals(-1, response.streamId());
    }

    @Test
    public void testReset(TestContext context) {
        response.reset();
    }

    @Test
    public void testPush(TestContext context) {
        StringBuilder res = new StringBuilder();
        response.push(HttpMethod.GET, "/", ar -> {
            res.append("DONE");
            context.assertTrue(ar.failed());
        });
        context.assertEquals("DONE", res.toString());
    }

    @Test
    public void testWriteCustomFrame(TestContext context) {
        context.assertEquals(response, response.writeCustomFrame(0, 0, Buffer.buffer()));
    }

    private JsonObject readOuput() {
        return new JsonObject(Buffer.buffer(responseOutput.toByteArray()));
    }
}
