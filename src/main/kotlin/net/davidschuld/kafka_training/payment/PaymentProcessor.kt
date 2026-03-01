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
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["order-events"], groupId = "payment-service")
    suspend fun onOrderEvent(record: ConsumerRecord<String, String>) {
        val order = objectMapper.readValue(record.value(), Order::class.java)
        log.info("Received order event [orderId={}, offset={}]", order.id, record.offset())

        when (val result = paymentConnector.processPayment(order)) {
            is PaymentResult.Success ->
                log.info("Payment succeeded [orderId={}, transactionId={}]", order.id, result.transactionId)
            is PaymentResult.Failure ->
                log.warn("Payment declined [orderId={}]", order.id)
            is PaymentResult.Timeout ->
                log.warn("Payment timed out [orderId={}]", order.id)
        }
    }
}
