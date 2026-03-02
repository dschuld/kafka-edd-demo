package net.davidschuld.kafka_training.payment

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
    private val processedRepository: OrderProcessedRepository,
    private val paymentProperties: PaymentProperties,
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

        if (paymentProperties.crashSimulation == CrashSimulation.AFTER_READ) {
            // If the consumer crashes here, no side effects have occured and the message will
            // safely be reprocessed on restart.
            log.warn("Crash simulation: AFTER_READ — exiting before payment processing")
            exitProcess(1)
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

        if (paymentProperties.crashSimulation == CrashSimulation.AFTER_PROCESSING) {
            // If the consumer crashes here, the offset will not be committed, and the message will be reprocessed on restart.
            // Each time the consumer crashes here, the message will be reprocessed because it
            // cannot be found in the table of processed messages.
            log.warn("Crash simulation: AFTER_PROCESSING — exiting before saving deduplication record")
            exitProcess(1)

        }

        if (idempotencyKey != null) {
            processedRepository.save(
                OrderProcessed(
                    messageId = idempotencyKey,
                    result = result.toString()
                )
            )
        }

        if (paymentProperties.crashSimulation == CrashSimulation.AFTER_SAVE) {
            // If the consumer crashes here, the offset will not be committed, and the message will be reprocessed on restart.
            // At the reprocessing, the idempotency key will be detected, and the payment will not be processed again,
            // but the offset will be committed, so the message will not be reprocessed again.
            log.warn("Crash simulation: AFTER_SAVE — exiting before offset is committed")
            exitProcess(1)

        }
    }
}
