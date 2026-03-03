package net.davidschuld.kafka_training.inventory

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class InventoryProcessor(
    private val inventoryConnector: InventoryConnector,
    private val objectMapper: ObjectMapper,
    private val processedRepository: InventoryProcessedRepository,
    private val outboxRepository: InventoryOutboxRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["order-events"], groupId = "inventory-service")
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
        log.info("Received ORDER_CREATED [orderId={}, offset={}]", order.id, record.offset())

        val result = inventoryConnector.reserveStock(order)
        val eventType: String

        when (result) {
            is ReservationResult.Reserved -> {
                eventType = "INVENTORY_RESERVED"
                log.info(
                    "Inventory reserved [orderId={}, reservationId={}]",
                    order.id,
                    result.reservationId
                )
            }
            is ReservationResult.Failed -> {
                eventType = "RESERVATION_FAILED"
                log.warn("Inventory reservation failed [orderId={}]", order.id)
            }
        }

        outboxRepository.save(
            InventoryOutbox(
                orderId = order.id!!,
                eventType = eventType,
                payload = record.value(),
            )
        )

        if (idempotencyKey != null) {
            processedRepository.save(
                InventoryProcessed(
                    messageId = idempotencyKey,
                    result = eventType,
                )
            )
        }
    }
}
