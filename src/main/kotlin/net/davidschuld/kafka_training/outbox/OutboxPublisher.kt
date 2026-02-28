package net.davidschuld.kafka_training.outbox

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import net.davidschuld.kafka_training.repository.OrderOutboxRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

@Component
class OutboxPublisher(
    private val outboxRepository: OrderOutboxRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
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
                kafkaTemplate.send(TOPIC, outbox.orderId.toString(), outbox.payload).await()

                outboxRepository.save(
                    outbox.copy(
                        published = true,
                        publishedAt = OffsetDateTime.now(),
                    )
                )

                log.info("Published outbox event [id={}] for order [id={}]", outbox.id, outbox.orderId)
            } catch (e: Exception) {
                log.error("Failed to publish outbox event [id={}]", outbox.id, e)
            }
        }
    }

    companion object {
        const val TOPIC = "order-events"
    }
}
