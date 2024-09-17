package com.github.johantiden.osmheat;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import jdk.net.ExtendedSocketOptions;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.TrustSelfSignedStrategy;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.ssl.SSLContexts;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

@Configuration
public class Config {

    @Bean
    @Order(0)
    public CorsFilter corsFilter(){
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedHeader(HttpHeaders.CONTENT_TYPE);
        config.addAllowedHeader(HttpHeaders.ACCEPT);
        config.addAllowedHeader(HttpHeaders.AUTHORIZATION);
        config.addAllowedHeader(HttpHeaders.ORIGIN);
        config.addAllowedHeader(HttpHeaders.COOKIE);
        config.addAllowedHeader(HttpHeaders.REFERER);
        config.addAllowedHeader(HttpHeaders.ACCEPT_LANGUAGE);
        config.addAllowedMethod(HttpMethod.GET);
        config.addAllowedMethod(HttpMethod.PATCH);
        config.addAllowedMethod(HttpMethod.POST);
        config.addAllowedMethod(HttpMethod.PUT);
        config.addAllowedMethod(HttpMethod.DELETE);
        config.setAllowedOriginPatterns(List.of(CorsConfiguration.ALL));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }

    @Bean
    public static ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        customizeConnector(tomcat);
        return tomcat;
    }

    private static void customizeConnector(TomcatServletWebServerFactory tomcatFactory) {
        TomcatConnectorCustomizer customizer = connector -> {
            connector.setURIEncoding(StandardCharsets.UTF_8.name());
            connector.setProperty("server-header", getServerHeaderValue());
        };

        tomcatFactory.setTomcatConnectorCustomizers(Collections.singletonList(customizer));
    }

    private static String getServerHeaderValue() {
        return String.format("%s/%s", Main.NAME, Main.VERSION);
    }


    @Bean
    public CloseableHttpClient httpClient() {
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(30000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(30000, TimeUnit.MILLISECONDS)
                .build();

        SSLContext sslContext;
        try {
            sslContext = SSLContexts.custom().loadTrustMaterial(null, getTrustSelfSignedStrategy()).build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create SSL context", e);
        }

        // We don't want to depend on lib-util (that's the whole reason for this lib to exist),
        // so we can't use the KeepAliveSSLConnectionSocketFactory from there.
        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE) {
            @Override
            protected void prepareSocket(SSLSocket socket, HttpContext context) throws IOException {
                socket.setKeepAlive(true);

                try {
                    socket.setOption(ExtendedSocketOptions.TCP_KEEPIDLE, 60);
                    socket.setOption(ExtendedSocketOptions.TCP_KEEPINTERVAL, 60);
                    socket.setOption(ExtendedSocketOptions.TCP_KEEPCOUNT, 5);
                } catch (UnsupportedOperationException | IllegalArgumentException | SecurityException e) {
                    // Don't fail if we cannot set the extended socket options for TCP keep-alive
                    System.err.println("Could not configure TCP keep-alive");
                }
            }
        };

        final HttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(socketFactory)
                .setDefaultConnectionConfig(connectionConfig)
                .build();

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    @SuppressWarnings("deprecation")
    private static TrustSelfSignedStrategy getTrustSelfSignedStrategy() {
        return new TrustSelfSignedStrategy();
    }

    @Bean
    public JsonMapper jsonMapper() {
        return  JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(MapperFeature.USE_GETTERS_AS_SETTERS)
                .addModule(new JavaTimeModule())
                .addModule(new ParameterNamesModule())
                .addModule(new Jdk8Module())
                .build();
    }
}
