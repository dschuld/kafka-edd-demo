package net.davidschuld.kafka_training.config

import net.davidschuld.kafka_training.order.OutboxPublisher
import net.davidschuld.kafka_training.payment.PaymentProperties
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.TopicBuilder

@EnableKafka
@Configuration
@EnableConfigurationProperties(PaymentProperties::class)
class KafkaConfig {

    @Bean
    fun orderEventsTopic(): NewTopic = TopicBuilder.name(OutboxPublisher.TOPIC)
        .partitions(1)
        .replicas(1)
        .build()
}
