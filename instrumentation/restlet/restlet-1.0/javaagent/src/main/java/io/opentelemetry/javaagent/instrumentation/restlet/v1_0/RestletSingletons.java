/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.restlet.v1_0;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.restlet.v1_0.RestletTracing;
import org.restlet.data.Request;
import org.restlet.data.Response;

public final class RestletSingletons {

  private static final Instrumenter<Request, Response> INSTRUMENTER =
      RestletTracing.create(GlobalOpenTelemetry.get()).getServerInstrumenter();

  public static Instrumenter<Request, Response> instrumenter() {
    return INSTRUMENTER;
  }

  private RestletSingletons() {}
}
