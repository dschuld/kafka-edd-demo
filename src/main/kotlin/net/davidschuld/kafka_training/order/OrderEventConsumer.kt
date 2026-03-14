package net.davidschuld.kafka_training.order

import net.davidschuld.kafka_training.config.EventTypes
import net.davidschuld.kafka_training.schemas.OrderCreated
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class OrderEventConsumer(
    private val orderService: OrderService,
    private val processedRepository: OrderProcessedRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["inventory-events", "payment-events"], groupId = "order-service")
    suspend fun onEvent(record: ConsumerRecord<String, OrderCreated>) {
        val eventType = record.headers().headers("event-type").firstOrNull()?.value()
            ?.toString(Charsets.UTF_8)

        if (eventType !in HANDLED_EVENTS) {
            return
        }

        val idempotencyKey = record.headers().headers("idempotency-key").firstOrNull()?.value()
            ?.toString(Charsets.UTF_8)

        if (idempotencyKey != null && processedRepository.existsByMessageId(idempotencyKey)) {
            log.warn(
                "Duplicate message detected [idempotencyKey={}, offset={}], skipping",
                idempotencyKey,
                record.offset()
            )
            return
        }

        val orderId = record.key()
        log.info("Received {} for order [id={}, offset={}]", eventType, orderId, record.offset())

        when (eventType) {
            EventTypes.PAYMENT_SUCCESS -> orderService.confirmOrder(orderId)
            EventTypes.RESERVATION_FAILED, EventTypes.PAYMENT_FAILED -> orderService.cancelOrder(orderId)
        }

        if (idempotencyKey != null) {
            processedRepository.save(
                OrderProcessed(
                    messageId = idempotencyKey,
                    result = eventType!!,
                )
            )
        }
    }

    companion object {
        private val HANDLED_EVENTS = setOf(
            EventTypes.RESERVATION_FAILED,
            EventTypes.PAYMENT_FAILED,
            EventTypes.PAYMENT_SUCCESS,
        )
    }
}
