package net.davidschuld.kafka_training.inventory

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
class InventoryEventPublisher(
    private val outboxRepository: InventoryOutboxRepository,
    private val kafkaTemplate: KafkaTemplate<String, OrderCreated>,
) {
    private val log = LoggerFactory.getLogger(javaClass)

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
                    "Published inventory event [id={}, type={}] for order [id={}]",
                    outbox.id,
                    outbox.eventType,
                    outbox.orderId
                )
            } catch (e: Exception) {
                log.error("Failed to publish inventory event [id={}]", outbox.id, e)
            }
        }
    }

    companion object {
        const val TOPIC = "inventory-events"
    }
}
