package com.okta.sdk.framework;

public class ApiClientConfiguration {

    private String baseUrl;
    private String proxyUrl;
    private int proxyPort;
    private String proxyScheme;
    private int apiVersion = 1;
    private String apiToken;

    public ApiClientConfiguration(String baseUrl, String apiToken) {
        this.baseUrl = baseUrl;
        this.apiToken = apiToken;
    }

    public ApiClientConfiguration(String baseUrl, String apiToken, String proxyUrl, int proxyPort, String proxyScheme) {
        this.baseUrl = baseUrl;
        this.apiToken = apiToken;
        this.proxyUrl = proxyUrl;
        this.proxyPort = proxyPort;
        this.proxyScheme = proxyScheme;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getApiVersion() {
        return apiVersion;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public String getProxyUrl() {
        return proxyUrl;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public String getProxyScheme() {
        return proxyScheme;
    }
}
