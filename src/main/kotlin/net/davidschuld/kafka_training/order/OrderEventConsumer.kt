package net.davidschuld.kafka_training.order

import net.davidschuld.kafka_training.schemas.PaymentFailed
import net.davidschuld.kafka_training.schemas.PaymentSucceeded
import net.davidschuld.kafka_training.schemas.ReservationFailed
import org.apache.avro.specific.SpecificRecord
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
    suspend fun onEvent(record: ConsumerRecord<String, SpecificRecord>) {
        val event = record.value()

        if (event !is PaymentSucceeded && event !is ReservationFailed && event !is PaymentFailed) {
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
        val eventName = event::class.simpleName
        log.info("Received {} for order [id={}, offset={}]", eventName, orderId, record.offset())

        when (event) {
            is PaymentSucceeded -> orderService.confirmOrder(orderId)
            is ReservationFailed, is PaymentFailed -> orderService.cancelOrder(orderId)
        }

        if (idempotencyKey != null) {
            processedRepository.save(
                OrderProcessed(
                    messageId = idempotencyKey,
                    result = eventName!!,
                )
            )
        }
    }
}
