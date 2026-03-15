package net.davidschuld.kafka_training.payment

import net.davidschuld.kafka_training.config.EventTypes
import net.davidschuld.kafka_training.schemas.InventoryReserved
import net.davidschuld.kafka_training.schemas.PaymentFailed
import net.davidschuld.kafka_training.schemas.PaymentSucceeded
import net.davidschuld.kafka_training.schemas.toJson
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.util.UUID
import kotlin.system.exitProcess

@Component
class PaymentProcessor(
    private val paymentConnector: PaymentConnector,
    private val processedRepository: ProcessedRepository,
    private val outboxRepository: PaymentOutboxRepository,
    private val paymentProperties: PaymentProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["inventory-events"], groupId = "payment-service")
    suspend fun onInventoryEvent(record: ConsumerRecord<String, SpecificRecord>) {
        val event = record.value()
        if (event !is InventoryReserved) return

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

        log.info("Received InventoryReserved [orderId={}, offset={}]", event.orderId, record.offset())
        val result = paymentConnector.processPayment(event.orderId)
        val paymentEventType: String
        val payload: String

        when (result) {
            is PaymentResult.Success -> {
                paymentEventType = EventTypes.PAYMENT_SUCCESS
                payload = PaymentSucceeded.newBuilder()
                    .setOrderId(event.orderId)
                    .setTransactionId(result.transactionId)
                    .build()
                    .toJson()
                log.info(
                    "Payment succeeded [orderId={}, transactionId={}]",
                    event.orderId,
                    result.transactionId
                )
            }

            is PaymentResult.Failure -> {
                paymentEventType = EventTypes.PAYMENT_FAILED
                payload = PaymentFailed.newBuilder()
                    .setOrderId(event.orderId)
                    .setReason("Payment declined")
                    .build()
                    .toJson()
                log.warn("Payment declined [orderId={}]", event.orderId)
            }

            is PaymentResult.Timeout -> {
                paymentEventType = EventTypes.PAYMENT_FAILED
                payload = PaymentFailed.newBuilder()
                    .setOrderId(event.orderId)
                    .setReason("Payment timed out")
                    .build()
                    .toJson()
                log.warn("Payment timed out [orderId={}]", event.orderId)
            }
        }

        if (paymentProperties.crashSimulation == CrashSimulation.AFTER_PROCESSING) {
            log.warn("Crash simulation: AFTER_PROCESSING — exiting before saving deduplication record")
            exitProcess(1)
        }

        outboxRepository.save(
            PaymentOutbox(
                orderId = UUID.fromString(event.orderId),
                eventType = paymentEventType,
                payload = payload,
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
