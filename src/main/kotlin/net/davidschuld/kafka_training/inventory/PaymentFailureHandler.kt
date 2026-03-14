package net.davidschuld.kafka_training.inventory

import net.davidschuld.kafka_training.config.EventTypes
import net.davidschuld.kafka_training.schemas.OrderCreated
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class PaymentFailureHandler(
    private val inventoryConnector: InventoryConnector,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["payment-events"], groupId = "inventory-service")
    suspend fun onPaymentEvent(record: ConsumerRecord<String, OrderCreated>) {
        val eventType = record.headers().headers("event-type").firstOrNull()?.value()
            ?.toString(Charsets.UTF_8)

        if (eventType != EventTypes.PAYMENT_FAILED) {
            return
        }

        val order = record.value()
        log.info("Received PAYMENT_FAILED for order [id={}], cancelling reservation", order.id)
        inventoryConnector.cancelReservation(UUID.fromString(order.id.toString()))
    }
}
