package net.davidschuld.kafka_training.inventory

import kotlinx.coroutines.delay
import net.davidschuld.kafka_training.schemas.OrderCreated
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID
import kotlin.random.Random

sealed interface ReservationResult {
    data class Reserved(val reservationId: String) : ReservationResult
    data object Failed : ReservationResult
}

@Component
class InventoryConnector {

    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun reserveStock(order: OrderCreated): ReservationResult {
        log.debug("Reserving stock for order [id={}]", order.orderId)

        return when (Random.nextInt(10)) {
            in 0..7 -> {
                delay(200)
                ReservationResult.Reserved(reservationId = UUID.randomUUID().toString())
            }
            else -> {
                delay(300)
                ReservationResult.Failed
            }
        }
    }

    suspend fun cancelReservation(orderId: String) {
        log.info("Cancelling inventory reservation for order [id={}]", orderId)
        delay(100)
        log.info("Reservation cancelled for order [id={}]", orderId)
    }
}
