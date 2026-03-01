package net.davidschuld.kafka_training.payment

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class PaymentProcessor(
    private val paymentConnector: PaymentConnector,
    private val objectMapper: ObjectMapper,
    private val processedRepository: OrderProcessedRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["order-events"], groupId = "payment-service")
    suspend fun onOrderEvent(record: ConsumerRecord<String, String>) {
        val idempotencyKey = record.headers().headers("idempotency-key").firstOrNull()?.value()
            ?.toString(Charsets.UTF_8)

        if (idempotencyKey == null) {
            log.warn(
                "Message at offset {} has no idempotency key, processing without deduplication",
                record.offset()
            )
        } else if (processedRepository.existsByMessageId(idempotencyKey)) {
            log.warn(
                "Duplicate message detected [idempotencyKey={}, offset={}], skipping",
                idempotencyKey,
                record.offset()
            )
            return
        }

        val order = objectMapper.readValue(record.value(), Order::class.java)
        log.info("Received order event [orderId={}, offset={}]", order.id, record.offset())
        val result = paymentConnector.processPayment(order)
        when (result) {
            is PaymentResult.Success ->
                log.info(
                    "Payment succeeded [orderId={}, transactionId={}]",
                    order.id,
                    result.transactionId
                )

            is PaymentResult.Failure ->
                log.warn("Payment declined [orderId={}]", order.id)

            is PaymentResult.Timeout ->
                log.warn("Payment timed out [orderId={}]", order.id)
        }

        if (idempotencyKey != null) {
            processedRepository.save(OrderProcessed(messageId = idempotencyKey, result = result))
        }
    }
}
