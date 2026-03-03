package net.davidschuld.kafka_training.payment

import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

@Component
class PaymentEventPublisher(
    private val outboxRepository: PaymentOutboxRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 5_000)
    fun publishPendingEvents() = runBlocking {
        outboxRepository.findByPublishedFalse().collect { outbox ->
            try {
                kafkaTemplate.send(
                    ProducerRecord(
                        TOPIC,
                        null,
                        outbox.orderId.toString(),
                        outbox.payload,
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
                    "Published payment event [id={}, type={}] for order [id={}]",
                    outbox.id,
                    outbox.eventType,
                    outbox.orderId
                )
            } catch (e: Exception) {
                log.error("Failed to publish payment event [id={}]", outbox.id, e)
            }
        }
    }

    companion object {
        const val TOPIC = "payment-events"
    }
}
