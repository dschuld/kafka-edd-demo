package net.davidschuld.kafka_training.inventory

import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import net.davidschuld.kafka_training.config.EventTypes
import net.davidschuld.kafka_training.schemas.InventoryReserved
import net.davidschuld.kafka_training.schemas.ReservationFailed
import net.davidschuld.kafka_training.schemas.avroFromJson
import org.apache.avro.specific.SpecificRecord
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
    private val kafkaTemplate: KafkaTemplate<String, SpecificRecord>,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 5_000)
    fun publishPendingEvents() = runBlocking {
        outboxRepository.findByPublishedFalse().collect { outbox ->
            try {
                val avroEvent: SpecificRecord = when (outbox.eventType) {
                    EventTypes.INVENTORY_RESERVED -> avroFromJson<InventoryReserved>(outbox.payload)
                    EventTypes.RESERVATION_FAILED -> avroFromJson<ReservationFailed>(outbox.payload)
                    else -> error("Unknown event type: ${outbox.eventType}")
                }

                kafkaTemplate.send(
                    ProducerRecord(
                        TOPIC,
                        null,
                        outbox.orderId.toString(),
                        avroEvent,
                        listOf(
                            RecordHeader("idempotency-key", outbox.id.toString().toByteArray()),
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
