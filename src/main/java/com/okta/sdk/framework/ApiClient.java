package com.okta.sdk.framework;

import com.fasterxml.jackson.core.type.TypeReference;
import com.okta.sdk.exceptions.ApiException;
import com.okta.sdk.exceptions.SdkException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

public abstract class ApiClient {

    public static final String AFTER_CURSOR = "after";
    public static final String LIMIT = "limit";
    public static final String FILTER = "filter";
    public static final String SEARCH_QUERY = "q";

    protected HttpClient httpClient;
    protected String baseUrl;
    protected int apiVersion;
    protected String token;
    protected ApiClientConfiguration configuration;
    private final Logger LOGGER = LoggerFactory.getLogger(ApiClient.class);

    ////////////////////////////////////////////
    // CONSTRUCTORS
    ////////////////////////////////////////////

    public ApiClient(ApiClientConfiguration config) {
        this(HttpClientBuilder.create()
                        .setUserAgent("OktaSDKJava_v" + Utils.getSdkVersion())
                        .disableCookieManagement().build(),
                config);
    }

    protected ApiClient(HttpClient httpClient, ApiClientConfiguration config) {
        this.httpClient = httpClient;
        this.baseUrl = config.getBaseUrl();
        this.apiVersion = config.getApiVersion();
        this.configuration = config;
        this.token = config.getApiToken();

        initMarshaller();
    }

    ////////////////////////////////////////////
    // ABSTRACT METHODS
    ////////////////////////////////////////////

    protected abstract void initMarshaller();

    protected abstract <T> T unmarshall(HttpResponse response, TypeReference<T> clazz) throws IOException;

    protected abstract HttpEntity buildRequestEntity(Object body) throws IOException;

    protected abstract String getFullPath(String relativePath);

    protected abstract void setAcceptHeader(HttpUriRequest httpUriRequest) throws IOException;

    protected abstract void setContentTypeHeader(HttpUriRequest httpUriRequest) throws IOException;

    ////////////////////////////////////////////
    // PUBLIC METHODS
    ////////////////////////////////////////////

    public String getToken() {
        return this.token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    ////////////////////////////////////////////
    // HTTP METHODS
    ////////////////////////////////////////////

    // POST

    protected Map post(String uri, Object httpEntity) throws IOException {
        return post(uri, httpEntity, new TypeReference<Map>() { });
    }

    protected <T> T post(String uri, Object httpEntity, TypeReference<T> clazz) throws IOException {
        return post(uri, buildRequestEntity(httpEntity), clazz);
    }

    protected <T> T post(String uri, HttpEntity httpEntity, TypeReference<T> clazz) throws IOException {
        HttpPost httpPost = new HttpPost();
        httpPost.setURI(getAbsoluteURI(uri));
        httpPost.setEntity(httpEntity);
        HttpResponse response = executeRequest(httpPost);
        return unmarshallResponse(clazz, response);
    }

    // GET

    protected Map get(String uri) throws IOException {
        return get(uri, new TypeReference<Map>() { });
    }

    protected HttpResponse getHttpResponse(String uri) throws IOException {
        HttpGet httpGet = new HttpGet();
        httpGet.setURI(getAbsoluteURI(uri));
        return executeRequest(httpGet);
    }

    protected <T> T get(String uri, TypeReference<T> clazz) throws IOException {
        HttpGet httpGet = new HttpGet();
        httpGet.setURI(getAbsoluteURI(uri));
        HttpResponse response = executeRequest(httpGet);
        return unmarshallResponse(clazz, response);
    }

    // PUT

    protected void put(String uri) throws IOException {
        put(uri, "");
    }

    protected Map put(String uri, Object httpEntity) throws IOException {
        return put(uri, httpEntity, new TypeReference<Map>() { });
    }

    protected <T> T put(String uri, TypeReference<T> clazz) throws IOException {
        return put(uri, "", clazz);
    }

    protected <T> T put(String uri, Object httpEntity, TypeReference<T> clazz) throws IOException {
        return put(uri, buildRequestEntity(httpEntity), clazz);
    }

    protected <T> T put(String uri, HttpEntity httpEntity, TypeReference<T> clazz) throws IOException {
        HttpPut httpPut = new HttpPut();
        httpPut.setURI(getAbsoluteURI(uri));
        httpPut.setEntity(httpEntity);
        HttpResponse response = executeRequest(httpPut);
        return unmarshallResponse(clazz, response);
    }

    // DELETE

    protected void delete(String uri) throws IOException {
        HttpDelete httpDelete = new HttpDelete();
        httpDelete.setURI(getAbsoluteURI(uri));
        HttpResponse response = executeRequest(httpDelete);
        unmarshallResponse(new TypeReference<Void>() { }, response);
    }

    protected <T> T delete(String uri, TypeReference<T> clazz) throws IOException {
        HttpDelete httpDelete = new HttpDelete();
        httpDelete.setURI(getAbsoluteURI(uri));
        HttpResponse response = executeRequest(httpDelete);
        return unmarshallResponse(clazz, response);
    }

    // BASE HTTP METHODS

    protected HttpResponse executeRequest(HttpUriRequest httpUriRequest) throws IOException {
        logRequest(httpUriRequest);
        setHeaders(httpUriRequest);
        return doExecute(httpUriRequest);
    }

    protected void logRequest(HttpUriRequest httpUriRequest) {
        LOGGER.info(String.format("%s %s", httpUriRequest.getMethod(), httpUriRequest.getURI()));
    }

    protected void setHeaders(HttpUriRequest httpUriRequest) throws IOException {
        setTokenHeader(httpUriRequest);

        // Content type and accept header are determined by the child class
        setContentTypeHeader(httpUriRequest);
        setAcceptHeader(httpUriRequest);
    }

    protected void setTokenHeader(HttpUriRequest httpUriRequest) throws IOException {
        Header authHeader = new BasicHeader("Authorization", String.format("SSWS %s", this.token));
        httpUriRequest.setHeader(authHeader);
    }

    protected HttpResponse doExecute(HttpUriRequest httpUriRequest) throws IOException {
        return httpClient.execute(httpUriRequest);
    }

    ////////////////////////////////////////////
    // UTILITY METHODS
    ////////////////////////////////////////////

    protected static String encode(String str) throws UnsupportedEncodingException {
        return URLEncoder.encode(str, "UTF-8");
    }

    protected String getEncodedPath(String formatStr, String ... args) throws IOException {
        return getFullPath(getEncodedString(formatStr, args));
    }

    private String getEncodedString(String formatStr, String[] args) throws UnsupportedEncodingException {
        final int ARGS_LENGTH = args.length;
        String[] encodedArgs = new String[ARGS_LENGTH];
        for (int i = 0; i < ARGS_LENGTH; i++) {
            encodedArgs[i] = encode(args[i]);
        }
        return String.format(formatStr, encodedArgs);
    }

    protected String getEncodedPathWithQueryParams(String formatStr, Map<String, String> params, String... args) throws UnsupportedEncodingException {
        String uri = getEncodedString(formatStr, args);
        for (String key : params.keySet()) {
            uri = addParameterToUri(uri, key, params.get(key));
        }
        return getFullPath(uri);
    }

    private String addParameterToUri(String uri, String name, String value) throws UnsupportedEncodingException {
        int queryStringStart = uri.indexOf('?');
        if (queryStringStart == -1) {
            return uri + "?" + encode(name) + "=" + encode(value);
        } else if (queryStringStart == uri.length() - 1) {
            return uri + encode(name) + "=" + encode(value);
        } else {
            return uri + "&" + encode(name) + "=" + encode(value);
        }
    }

    protected URI getAbsoluteURI(String relativeURI) throws IOException {
        try {
            if(relativeURI.startsWith("http:") || relativeURI.startsWith("https:")) {
                return new URI(relativeURI);
            }
            else {
                return new URL(String.format("%s%s", this.baseUrl, relativeURI)).toURI();
            }
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    protected <T> T unmarshallResponse(TypeReference<T> clazz, HttpResponse httpResponse) throws IOException {
        boolean contentReturned = checkResponse(httpResponse);
        if (contentReturned) {
            return unmarshall(httpResponse, clazz);
        } else {
            return null;
        }
    }

    protected boolean checkResponse(HttpResponse response) throws IOException {
        if(response == null) {
            throw new SdkException("A response wasn't received");
        }

        StatusLine statusLine = response.getStatusLine();
        if(statusLine == null) {
            throw new SdkException("A statusLine wasn't present in the response");
        }

        int statusCode = statusLine.getStatusCode();

        if ((statusCode == 200 || statusCode == 201) && response.getEntity() != null) {
            // success; content returned
            return true;
        } else if (statusCode == 204 && response.getEntity() == null) {
            // success; no content returned
            return false;
        } else {
            extractError(response);
            LOGGER.info("We found an api exception and didn't throw an error");
            return false; // will never hit this line
        }
    }

    protected void extractError(HttpResponse response) throws IOException {
        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();
        ErrorResponse errors = unmarshall(response, new TypeReference<ErrorResponse>() { });
        throw new ApiException(statusCode, errors);
    }
}
