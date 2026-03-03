package net.davidschuld.kafka_training.inventory

import net.davidschuld.kafka_training.config.EventTypes
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class PaymentFailureHandler(
    private val inventoryConnector: InventoryConnector,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["payment-events"], groupId = "inventory-service")
    suspend fun onPaymentEvent(record: ConsumerRecord<String, String>) {
        val eventType = record.headers().headers("event-type").firstOrNull()?.value()
            ?.toString(Charsets.UTF_8)

        if (eventType != EventTypes.PAYMENT_FAILED) {
            return
        }

        val order = objectMapper.readValue(record.value(), Order::class.java)
        log.info("Received PAYMENT_FAILED for order [id={}], cancelling reservation", order.id)
        inventoryConnector.cancelReservation(order.id!!)
    }
}
