/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.kubernetesclient;

import io.kubernetes.client.openapi.ApiResponse;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesExtractor;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import okhttp3.Request;
import org.checkerframework.checker.nullness.qual.Nullable;

class KubernetesHttpAttributesExtractor
    extends HttpClientAttributesExtractor<Request, ApiResponse<?>> {
  @Override
  protected String method(Request request) {
    return request.method();
  }

  @Override
  protected String url(Request request) {
    return request.url().toString();
  }

  @Override
  protected @Nullable String userAgent(Request request) {
    return request.header("user-agent");
  }

  @Override
  protected @Nullable Long requestContentLength(
      Request request, @Nullable ApiResponse<?> apiResponse) {
    return null;
  }

  @Override
  protected @Nullable Long requestContentLengthUncompressed(
      Request request, @Nullable ApiResponse<?> apiResponse) {
    return null;
  }

  @Override
  protected String flavor(Request request, @Nullable ApiResponse<?> apiResponse) {
    return SemanticAttributes.HttpFlavorValues.HTTP_1_1;
  }

  @Override
  protected Integer statusCode(Request request, ApiResponse<?> apiResponse) {
    return apiResponse.getStatusCode();
  }

  @Override
  protected @Nullable Long responseContentLength(Request request, ApiResponse<?> apiResponse) {
    return null;
  }

  @Override
  protected @Nullable Long responseContentLengthUncompressed(
      Request request, ApiResponse<?> apiResponse) {
    return null;
  }
}
