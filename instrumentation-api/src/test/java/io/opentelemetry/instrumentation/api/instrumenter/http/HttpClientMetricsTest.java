/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.instrumenter.http;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.attributeEntry;
import static io.opentelemetry.sdk.testing.assertj.metrics.MetricAssertions.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.RequestListener;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.MetricData;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class HttpClientMetricsTest {

  @Test
  void collectsMetrics() {
    SdkMeterProvider meterProvider = SdkMeterProvider.builder().build();

    RequestListener listener = HttpClientMetrics.get().create(meterProvider.get("test"));

    Attributes requestAttributes =
        Attributes.builder()
            .put("http.method", "GET")
            .put("http.host", "host")
            .put("http.scheme", "https")
            .put("net.host.name", "localhost")
            .put("net.host.port", 1234)
            .build();

    // Currently ignored.
    Attributes responseAttributes =
        Attributes.builder()
            .put("http.flavor", "2.0")
            .put("http.server_name", "server")
            .put("http.status_code", 200)
            .build();

    Context context1 = listener.start(Context.current(), requestAttributes, nanos(100));

    Collection<MetricData> metrics = meterProvider.collectAllMetrics();
    assertThat(metrics).isEmpty();

    Context context2 = listener.start(Context.current(), requestAttributes, nanos(150));

    metrics = meterProvider.collectAllMetrics();
    assertThat(metrics).isEmpty();

    listener.end(context1, responseAttributes, nanos(250));

    metrics = meterProvider.collectAllMetrics();
    assertThat(metrics).hasSize(1);
    assertThat(metrics)
        .anySatisfy(
            metric ->
                assertThat(metric)
                    .hasName("http.client.duration")
                    .hasDoubleHistogram()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point)
                                .hasSum(150 /* millis */)
                                .attributes()
                                .containsOnly(
                                    attributeEntry("http.host", "host"),
                                    attributeEntry("http.method", "GET"),
                                    attributeEntry("http.scheme", "https"),
                                    attributeEntry("net.host.name", "localhost"),
                                    attributeEntry("net.host.port", 1234L))));

    listener.end(context2, responseAttributes, nanos(300));

    metrics = meterProvider.collectAllMetrics();
    assertThat(metrics).hasSize(1);
    assertThat(metrics)
        .anySatisfy(
            metric ->
                assertThat(metric)
                    .hasName("http.client.duration")
                    .hasDoubleHistogram()
                    .points()
                    .satisfiesExactly(point -> assertThat(point).hasSum(300 /* millis */)));
  }

  private static long nanos(int millis) {
    return TimeUnit.MILLISECONDS.toNanos(millis);
  }
}
