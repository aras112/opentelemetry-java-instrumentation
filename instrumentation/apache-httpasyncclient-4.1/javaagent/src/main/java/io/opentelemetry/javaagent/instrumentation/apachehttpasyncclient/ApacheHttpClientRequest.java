/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpasyncclient;

import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.ProtocolVersion;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ApacheHttpClientRequest {

  private static final Logger logger = LoggerFactory.getLogger(ApacheHttpClientRequest.class);

  @Nullable private final URI uri;

  private final HttpRequest delegate;

  public ApacheHttpClientRequest(HttpHost httpHost, HttpRequest httpRequest) {
    URI calculatedUri = getUri(httpRequest);
    if (calculatedUri != null && httpHost != null) {
      uri = getCalculatedUri(httpHost, calculatedUri);
    } else {
      uri = calculatedUri;
    }
    delegate = httpRequest;
  }

  public String getHeader(String name) {
    Header header = delegate.getFirstHeader(name);
    return header != null ? header.getValue() : null;
  }

  public void setHeader(String name, String value) {
    delegate.setHeader(name, value);
  }

  public String getMethod() {
    return delegate.getRequestLine().getMethod();
  }

  public String getUrl() {
    return uri != null ? uri.toString() : null;
  }

  public String getFlavor() {
    ProtocolVersion protocolVersion = delegate.getProtocolVersion();
    String protocol = protocolVersion.getProtocol();
    if (!protocol.equals("HTTP")) {
      return null;
    }
    int major = protocolVersion.getMajor();
    int minor = protocolVersion.getMinor();
    if (major == 1 && minor == 0) {
      return SemanticAttributes.HttpFlavorValues.HTTP_1_0;
    }
    if (major == 1 && minor == 1) {
      return SemanticAttributes.HttpFlavorValues.HTTP_1_1;
    }
    if (major == 2 && minor == 0) {
      return SemanticAttributes.HttpFlavorValues.HTTP_2_0;
    }
    logger.debug("unexpected http protocol version: " + protocolVersion);
    return null;
  }

  public String getPeerName() {
    return uri != null ? uri.getHost() : null;
  }

  public Integer getPeerPort() {
    if (uri == null) {
      return null;
    }
    int port = uri.getPort();
    if (port != -1) {
      return port;
    }
    switch (uri.getScheme()) {
      case "http":
        return 80;
      case "https":
        return 443;
      default:
        logger.debug("no default port mapping for scheme: {}", uri.getScheme());
        return null;
    }
  }

  @Nullable
  private static URI getUri(HttpRequest httpRequest) {
    try {
      // this can be relative or absolute
      return new URI(httpRequest.getRequestLine().getUri());
    } catch (URISyntaxException e) {
      logger.debug(e.getMessage(), e);
      return null;
    }
  }

  @Nullable
  private static URI getCalculatedUri(HttpHost httpHost, URI uri) {
    try {
      String path = uri.getPath();
      if (!path.startsWith("/")) {
        // elasticsearch RestClient sends relative urls
        // TODO(trask) add test for this and extend to Apache 4, 4.3 and 5
        path = "/" + path;
      }
      return new URI(
          httpHost.getSchemeName(),
          null,
          httpHost.getHostName(),
          httpHost.getPort(),
          path,
          uri.getQuery(),
          uri.getFragment());
    } catch (URISyntaxException e) {
      logger.debug(e.getMessage(), e);
      return null;
    }
  }
}
