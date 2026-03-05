package com.gissoft.inspection_backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        ContentCachingRequestWrapper requestWrapper =
                new ContentCachingRequestWrapper(request);

        ContentCachingResponseWrapper responseWrapper =
                new ContentCachingResponseWrapper(response);

        try {

            filterChain.doFilter(requestWrapper, responseWrapper);

        } catch (Exception ex) {

            log.error("ERROR processing request", ex);
            throw ex;

        } finally {

            long duration = System.currentTimeMillis() - startTime;

            String requestBody = new String(
                    requestWrapper.getContentAsByteArray(),
                    StandardCharsets.UTF_8
            );

            String responseBody = new String(
                    responseWrapper.getContentAsByteArray(),
                    StandardCharsets.UTF_8
            );

            log.info("""
                    
                    ===== API REQUEST =====
                    METHOD   : {}
                    URI      : {}
                    QUERY    : {}
                    BODY     : {}
                    
                    ===== API RESPONSE =====
                    STATUS   : {}
                    BODY     : {}
                    TIME(ms) : {}
                    
                    """,
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getQueryString(),
                    requestBody,
                    responseWrapper.getStatus(),
                    responseBody,
                    duration
            );

            responseWrapper.copyBodyToResponse();
        }
    }
}