package net.davidschuld.kafka_training.inventory

import net.davidschuld.kafka_training.schemas.PaymentFailed
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class PaymentFailureHandler(
    private val inventoryConnector: InventoryConnector,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["payment-events"], groupId = "inventory-service")
    suspend fun onPaymentEvent(record: ConsumerRecord<String, SpecificRecord>) {
        val event = record.value()
        if (event !is PaymentFailed) return

        log.info("Received PaymentFailed for order [id={}], cancelling reservation", event.orderId)
        inventoryConnector.cancelReservation(event.orderId)
    }
}
