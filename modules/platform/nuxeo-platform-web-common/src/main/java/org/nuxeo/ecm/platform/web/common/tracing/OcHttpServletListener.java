/*
 * Copyright 2018, OpenCensus Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.nuxeo.ecm.platform.web.common.tracing;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Closeable;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.google.errorprone.annotations.MustBeClosed;

import io.opencensus.common.ExperimentalApi;
import io.opencensus.common.Scope;
import io.opencensus.contrib.http.HttpRequestContext;
import io.opencensus.contrib.http.HttpServerHandler;
import io.opencensus.trace.Tracing;

/**
 * This class implements {@link AsyncListener} to handle span completion for async request handling.
 * 
 * @implNote Copied from {@code io.opencensus:opencensus-contrib-http-servlet} to make it Jakarta compatible
 */
@ExperimentalApi
public final class OcHttpServletListener implements Closeable, AsyncListener {
    private final HttpRequestContext context;

    private final HttpServerHandler<HttpServletRequest, HttpServletResponse, HttpServletRequest> handler;

    OcHttpServletListener(HttpServerHandler<HttpServletRequest, HttpServletResponse, HttpServletRequest> handler,
            HttpRequestContext context) {
        checkNotNull(context, "context");
        checkNotNull(handler, "handler");
        this.context = context;
        this.handler = handler;
    }

    @Override
    public void close() {
    }

    @Override
    public void onComplete(AsyncEvent event) {
        ServletResponse response = event.getSuppliedResponse();
        if (response instanceof HttpServletResponse) {
            OcHttpServletUtil.recordMessageSentEvent(handler, context, (HttpServletResponse) response);
        }
        handler.handleEnd(context, (HttpServletRequest) event.getSuppliedRequest(),
                (HttpServletResponse) event.getSuppliedResponse(), null);
        this.close();
    }

    @Override
    public void onError(AsyncEvent event) {
        handler.handleEnd(context, (HttpServletRequest) event.getSuppliedRequest(),
                (HttpServletResponse) event.getSuppliedResponse(), event.getThrowable());
    }

    @Override
    public void onStartAsync(AsyncEvent event) {
        AsyncContext eventAsyncContext = event.getAsyncContext();
        if (eventAsyncContext != null) {
            eventAsyncContext.addListener(this, event.getSuppliedRequest(), event.getSuppliedResponse());
        }
    }

    @Override
    public void onTimeout(AsyncEvent event) {
        handler.handleEnd(context, (HttpServletRequest) event.getSuppliedRequest(),
                (HttpServletResponse) event.getSuppliedResponse(), null);
    }

    @MustBeClosed
    Scope withSpan() {
        return Tracing.getTracer().withSpan(handler.getSpanFromContext(context));
    }
}
