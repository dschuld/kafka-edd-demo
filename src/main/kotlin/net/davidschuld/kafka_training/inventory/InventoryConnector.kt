package net.davidschuld.kafka_training.inventory

import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import kotlin.random.Random

sealed interface ReservationResult {
    data class Reserved(val reservationId: String) : ReservationResult
    data object Failed : ReservationResult
}

@Component
class InventoryConnector {

    private val log = LoggerFactory.getLogger(javaClass)

    // Simulates communication with an external inventory system.
    // Returns Reserved (~80%) or Failed (~20%).
    suspend fun reserveStock(order: Order): ReservationResult {
        log.debug("Reserving stock for order [id={}]", order.id)

        return when (Random.nextInt(10)) {
            in 0..7 -> {
                delay(200)
                ReservationResult.Reserved(reservationId = java.util.UUID.randomUUID().toString())
            }
            else -> {
                delay(300)
                ReservationResult.Failed
            }
        }
    }

    suspend fun cancelReservation(orderId: java.util.UUID) {
        log.info("Cancelling inventory reservation for order [id={}]", orderId)
        delay(100)
        log.info("Reservation cancelled for order [id={}]", orderId)
    }
}
