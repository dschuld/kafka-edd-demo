package net.davidschuld.kafka_training.config

import net.davidschuld.kafka_training.outbox.OutboxPublisher
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

@Configuration
class KafkaConfig {

    @Bean
    fun orderEventsTopic(): NewTopic = TopicBuilder.name(OutboxPublisher.TOPIC)
        .partitions(1)
        .replicas(1)
        .build()
}
