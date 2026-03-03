package net.davidschuld.kafka_training.payment

import net.davidschuld.kafka_training.config.EventTypes
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import kotlin.system.exitProcess

@Component
class PaymentProcessor(
    private val paymentConnector: PaymentConnector,
    private val objectMapper: ObjectMapper,
    private val processedRepository: ProcessedRepository,
    private val outboxRepository: PaymentOutboxRepository,
    private val paymentProperties: PaymentProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["inventory-events"], groupId = "payment-service")
    suspend fun onInventoryEvent(record: ConsumerRecord<String, String>) {
        val eventType = record.headers().headers("event-type").firstOrNull()?.value()
            ?.toString(Charsets.UTF_8)

        if (eventType != EventTypes.INVENTORY_RESERVED) {
            return
        }

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

        if (paymentProperties.crashSimulation == CrashSimulation.AFTER_READ) {
            log.warn("Crash simulation: AFTER_READ — exiting before payment processing")
            exitProcess(1)
        }

        val order = objectMapper.readValue(record.value(), Order::class.java)
        log.info("Received INVENTORY_RESERVED [orderId={}, offset={}]", order.id, record.offset())
        val result = paymentConnector.processPayment(order)
        val paymentEventType: String

        when (result) {
            is PaymentResult.Success -> {
                paymentEventType = EventTypes.PAYMENT_SUCCESS
                log.info(
                    "Payment succeeded [orderId={}, transactionId={}]",
                    order.id,
                    result.transactionId
                )
            }

            is PaymentResult.Failure -> {
                paymentEventType = EventTypes.PAYMENT_FAILED
                log.warn("Payment declined [orderId={}]", order.id)
            }

            is PaymentResult.Timeout -> {
                paymentEventType = EventTypes.PAYMENT_FAILED
                log.warn("Payment timed out [orderId={}]", order.id)
            }
        }

        if (paymentProperties.crashSimulation == CrashSimulation.AFTER_PROCESSING) {
            log.warn("Crash simulation: AFTER_PROCESSING — exiting before saving deduplication record")
            exitProcess(1)
        }

        outboxRepository.save(
            PaymentOutbox(
                orderId = order.id!!,
                eventType = paymentEventType,
                payload = record.value(),
            )
        )

        if (idempotencyKey != null) {
            processedRepository.save(
                OrderProcessed(
                    messageId = idempotencyKey,
                    result = result.toString()
                )
            )
        }

        if (paymentProperties.crashSimulation == CrashSimulation.AFTER_SAVE) {
            log.warn("Crash simulation: AFTER_SAVE — exiting before offset is committed")
            exitProcess(1)
        }
    }
}
