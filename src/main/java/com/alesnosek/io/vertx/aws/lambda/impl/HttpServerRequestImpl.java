package com.alesnosek.io.vertx.aws.lambda.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.security.cert.X509Certificate;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpFrame;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerFileUpload;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;

public class HttpServerRequestImpl implements HttpServerRequest {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerRequestImpl.class);

    private final JsonObject request;
    private final HttpServerResponse response;

    private String uri;
    private String query;

    private Handler<Buffer> dataHandler;
    private Handler<Void> endHandler;

    private MultiMap params;
    private MultiMap headers;
    private String absoluteURI;

    private MultiMap attributes;

    private boolean ended;

    private String localHost;
    private int localPort = 0;

    private final String remoteHost = "0.0.0.0";
    private final int remotePort = 0;

    private SocketAddress localAddress;
    private SocketAddress remoteAddress;

    public HttpServerRequestImpl(String localHost, int localPort, JsonObject request, HttpServerResponse response) {
        this.localHost = localHost;
        this.localPort = localPort;
        this.request = request;
        this.response = response;
    }

    @Override
    public HttpServerRequest exceptionHandler(Handler<Throwable> handler) {
        return this;
    }

    @Override
    public HttpServerRequest handler(Handler<Buffer> dataHandler) {
        checkEnded();
        this.dataHandler = dataHandler;
        return this;
    }

    @Override
    public HttpServerRequest pause() {
        return this;
    }

    @Override
    public HttpServerRequest resume() {
        return this;
    }

    @Override
    public HttpServerRequest endHandler(Handler<Void> endHandler) {
        checkEnded();
        this.endHandler = endHandler;
        return this;
    }

    @Override
    public HttpVersion version() {
        return HttpVersion.HTTP_1_1;
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.valueOf(request.getString("httpMethod"));
    }

    @Override
    public String rawMethod() {
        return request.getString("httpMethod");
    }

    @Override
    public boolean isSSL() {
        return false;
    }

    @Override
    public String scheme() {
        return "http";
    }

    @Override
    public String uri() {
        if (uri == null) {
            String path = path().equals("/") ? "" : path();
            uri = scheme() + "://" + host() + ":" + localPort + path;
            if (query().length() > 0) {
                uri = uri + "?" + query();
            };
        }
        return uri;
    }

    @Override
    public String path() {
        return request.getString("path");
    }

    @Override
    public String query() {
        if (query == null) {
            StringBuilder queryBuilder = new StringBuilder();
            for (Map.Entry<String, String> param : params()) {
                if (queryBuilder.length() > 0) {
                    queryBuilder.append("&");
                }
                queryBuilder.append(urlEncode(param.getKey()));
                queryBuilder.append("=");
                queryBuilder.append(urlEncode(param.getValue()));
            }
            query = queryBuilder.toString();
        }
        return query;
    }

    @Override
    public String host() {
        return localHost;
    }

    @Override
    public HttpServerResponse response() {
        return response;
    }

    @Override
    public MultiMap headers() {
        if (headers == null) {
            headers = MultiMap.caseInsensitiveMultiMap();
            JsonObject requestHeaders = request.getJsonObject("headers");
            if (requestHeaders != null) {
                for (Map.Entry<String, Object> entry : requestHeaders) {
                    headers.add(entry.getKey(), entry.getValue().toString());
                }
            }
        }
        return headers;
    }

    @Override
    public String getHeader(String headerName) {
        return headers().get(headerName);
    }

    @Override
    public String getHeader(CharSequence headerName) {
        return headers().get(headerName.toString());
    }

    @Override
    public MultiMap params() {
        if (params == null) {
            params = new CaseSensitiveMultiMapImpl();
            JsonObject queryParams = request.getJsonObject("queryStringParameters");
            if (queryParams != null) {
                for (Map.Entry<String, Object> entry : queryParams) {
                    params.add(entry.getKey(), entry.getValue().toString());
                }
            }
        }
        return params;
    }

    @Override
    public String getParam(String paramName) {
        return params().get(paramName);
    }

    @Override
    public SocketAddress remoteAddress() {
        if (remoteAddress == null) {
            remoteAddress = new SocketAddress() {

                @Override
                public String host() {
                    return remoteHost;
                }

                @Override
                public int port() {
                    return remotePort;
                }
            };
        }
        return remoteAddress;
    }

    @Override
    public SocketAddress localAddress() {
        if (localAddress == null) {
            localAddress = new SocketAddress() {

                @Override
                public String host() {
                    return localHost;
                }

                @Override
                public int port() {
                    return localPort;
                }
            };
        }
        return localAddress;
    }

    @Override
    public X509Certificate[] peerCertificateChain() throws SSLPeerUnverifiedException {
        return null;
    }

    @Override
    public String absoluteURI() {
        if (absoluteURI == null) {
            absoluteURI = uri();
        }
        return absoluteURI;
    }

    @Override
    public NetSocket netSocket() {
        return null;
    }

    @Override
    public HttpServerRequest setExpectMultipart(boolean expect) {
        checkEnded();
        if (expect) {
            throw new java.lang.UnsupportedOperationException("Not supported yet.");
        }
        return this;
    }

    @Override
    public boolean isExpectMultipart() {
        return false;
    }

    @Override
    public HttpServerRequest uploadHandler(Handler<HttpServerFileUpload> uploadHandler) {
        checkEnded();
        return this;
    }

    @Override
    public MultiMap formAttributes() {
        if (attributes == null) {
            attributes = MultiMap.caseInsensitiveMultiMap();
        }
        return attributes;
    }

    @Override
    public String getFormAttribute(String attributeName) {
        return formAttributes().get(attributeName);
    }

    @Override
    public ServerWebSocket upgrade() {
        throw new java.lang.UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isEnded() {
        return ended;
    }

    @Override
    public HttpServerRequest customFrameHandler(Handler<HttpFrame> handler) {
        return this;
    }

    @Override
    public HttpConnection connection() {
        return null;
    }

    public void handleData() {
        Buffer data = null;
        if (request.getBoolean("isBase64Encoded")) {
            byte[] body = request.getBinary("body");
            if (body != null) {
                data = Buffer.buffer(body);
            }
        } else {
            String body = request.getString("body");
            if (body != null) {
                data = Buffer.buffer(body);
            }
        }

        if (data != null && dataHandler != null) {
            dataHandler.handle(data);
        }
    }

    public void handleEnd() {
        ended = true;
        if (endHandler != null) {
            endHandler.handle(null);
        }
    }

    private void checkEnded() {
        if (ended) {
            throw new IllegalStateException("Request has already been read");
        }
    }

    private String urlEncode(String param) {
        String res = "";
        try {
            res = URLEncoder.encode(param, "UTF-8").replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("Failed to encode query parameter " + param, e);
        }
        return res;
    }
}