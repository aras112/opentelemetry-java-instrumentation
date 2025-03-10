/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.rediscala;

import io.opentelemetry.instrumentation.api.instrumenter.db.DbAttributesExtractor;
import io.opentelemetry.instrumentation.api.tracer.ClassNames;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.Locale;
import org.checkerframework.checker.nullness.qual.Nullable;
import redis.RedisCommand;

final class RediscalaAttributesExtractor extends DbAttributesExtractor<RedisCommand<?, ?>, Void> {
  @Override
  protected String system(RedisCommand<?, ?> redisCommand) {
    return SemanticAttributes.DbSystemValues.REDIS;
  }

  @Override
  protected @Nullable String user(RedisCommand<?, ?> redisCommand) {
    return null;
  }

  @Override
  protected @Nullable String name(RedisCommand<?, ?> redisCommand) {
    return null;
  }

  @Override
  protected @Nullable String connectionString(RedisCommand<?, ?> redisCommand) {
    return null;
  }

  @Override
  protected @Nullable String statement(RedisCommand<?, ?> redisCommand) {
    return null;
  }

  @Override
  protected String operation(RedisCommand<?, ?> redisCommand) {
    return ClassNames.simpleName(redisCommand.getClass()).toUpperCase(Locale.ROOT);
  }
}
