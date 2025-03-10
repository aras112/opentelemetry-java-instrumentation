/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.kafkaclients

import io.opentelemetry.instrumentation.test.InstrumentationSpecification
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.IntegerDeserializer
import org.apache.kafka.common.serialization.IntegerSerializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.testcontainers.containers.KafkaContainer
import spock.lang.Shared

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

abstract class KafkaClientBaseTest extends InstrumentationSpecification {

  protected static final SHARED_TOPIC = "shared.topic"

  @Shared
  static KafkaContainer kafka
  @Shared
  static Producer<Integer, String> producer
  @Shared
  static Consumer<Integer, String> consumer
  @Shared
  static CountDownLatch consumerReady = new CountDownLatch(1)

  static TopicPartition topicPartition = new TopicPartition(SHARED_TOPIC, 0)

  def setupSpec() {
    kafka = new KafkaContainer()
    kafka.start()

    // create test topic
    AdminClient.create(["bootstrap.servers": kafka.bootstrapServers]).withCloseable { admin ->
      admin.createTopics([new NewTopic(SHARED_TOPIC, 1, (short) 1)]).all().get(10, TimeUnit.SECONDS)
    }

    producer = new KafkaProducer<>(producerProps())

    consumer = new KafkaConsumer<>(consumerProps())

    consumer.subscribe([SHARED_TOPIC], new ConsumerRebalanceListener() {
      @Override
      void onPartitionsRevoked(Collection<TopicPartition> collection) {
      }

      @Override
      void onPartitionsAssigned(Collection<TopicPartition> collection) {
        consumerReady.countDown()
      }
    })
  }

  Map<String, ?> producerProps() {
    // values copied from spring's KafkaTestUtils
    return [
      "bootstrap.servers": kafka.bootstrapServers,
      "retries"          : 0,
      "batch.size"       : "16384",
      "linger.ms"        : 1,
      "buffer.memory"    : "33554432",
      "key.serializer"   : IntegerSerializer,
      "value.serializer" : StringSerializer
    ]
  }

  Map<String, ?> consumerProps() {
    // values copied from spring's KafkaTestUtils
    return [
      "bootstrap.servers"      : kafka.bootstrapServers,
      "group.id"               : "test",
      "enable.auto.commit"     : "true",
      "auto.commit.interval.ms": "10",
      "session.timeout.ms"     : "30000",
      "key.deserializer"       : IntegerDeserializer,
      "value.deserializer"     : StringDeserializer
    ]
  }

  def cleanupSpec() {
    consumer?.close()
    producer?.close()
    kafka.stop()
  }

  // Kafka's eventual consistency behavior forces us to do a couple of empty poll() calls until it gets properly assigned a topic partition
  static void awaitUntilConsumerIsReady() {
    if (consumerReady.await(0, TimeUnit.SECONDS)) {
      return
    }
    for (i in 0..<10) {
      consumer.poll(0)
      if (consumerReady.await(1, TimeUnit.SECONDS)) {
        break
      }
    }
    if (consumerReady.getCount() != 0) {
      throw new AssertionError("Consumer wasn't assigned any partitions!")
    }
    consumer.seekToBeginning([])
  }
}
