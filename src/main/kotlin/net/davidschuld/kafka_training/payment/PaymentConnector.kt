package net.davidschuld.kafka_training.payment

import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID
import kotlin.random.Random

sealed interface PaymentResult {
    data class Success(val transactionId: String) : PaymentResult
    data object Failure : PaymentResult
    data object Timeout : PaymentResult
}

@Component
class PaymentConnector {

    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun processPayment(orderId: String): PaymentResult {
        log.debug("Sending payment request for order [id={}]", orderId)

        return when (Random.nextInt(10)) {
            in 0..6 -> {
                delay (200)
                PaymentResult.Success(transactionId = UUID.randomUUID().toString())
            }
            in 7..8 -> {
                delay (500)
                PaymentResult.Failure
            }
            else    -> {
                delay(2000)
                PaymentResult.Timeout
            }
        }
    }
}
