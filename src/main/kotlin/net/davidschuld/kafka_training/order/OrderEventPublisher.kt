package net.davidschuld.kafka_training.order

import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import net.davidschuld.kafka_training.schemas.OrderCreated
import net.davidschuld.kafka_training.schemas.orderCreatedFromJson
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

@Component
class OrderEventPublisher(
    private val outboxRepository: OrderOutboxRepository,
    private val kafkaTemplate: KafkaTemplate<String, OrderCreated>,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // Polls every 5 seconds for unpublished outbox entries.
    // Each entry is sent to Kafka and then marked published — these are separate steps
    // (not atomic), which means a crash between them causes a re-delivery on the next
    // poll. Consumers must therefore be idempotent.
    @Scheduled(fixedDelay = 5_000)
    fun publishPendingEvents() = runBlocking {
        outboxRepository.findByPublishedFalse().collect { outbox ->
            try {
                val avroOrder = orderCreatedFromJson(outbox.payload)

                kafkaTemplate.send(
                    ProducerRecord(
                        TOPIC,
                        null,
                        outbox.orderId.toString(),
                        avroOrder,
                        listOf(
                            RecordHeader("idempotency-key", outbox.id.toString().toByteArray()),
                            RecordHeader("event-type", outbox.eventType.toByteArray()),
                        )
                    )
            ).await()

                outboxRepository.save(
                    outbox.copy(
                        published = true,
                        publishedAt = OffsetDateTime.now(),
                    )
                )

                log.info(
                    "Published outbox event [id={}] for order [id={}]",
                    outbox.id,
                    outbox.orderId
                )
            } catch (e: Exception) {
                log.error("Failed to publish outbox event [id={}]", outbox.id, e)
            }
        }
    }

    companion object {
        const val TOPIC = "order-events"
    }
}
