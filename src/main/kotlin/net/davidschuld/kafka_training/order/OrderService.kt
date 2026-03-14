package net.davidschuld.kafka_training.order

import net.davidschuld.kafka_training.config.EventTypes
import net.davidschuld.kafka_training.schemas.OrderCreated
import net.davidschuld.kafka_training.schemas.toJson
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val outboxRepository: OrderOutboxRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    // Both the order row and the outbox row are written in a single DB transaction.
    // If either write fails, neither is persisted — no phantom events, no missing events.
    @Transactional
    suspend fun createOrder(request: CreateOrderRequest): Order {
        val order = orderRepository.save(
            Order(
                customerId = request.customerId,
                product = request.product,
                quantity = request.quantity,
            )
        )

        val payload = OrderCreated.newBuilder()
            .setId(order.id.toString())
            .setCustomerId(order.customerId)
            .setProduct(order.product)
            .setQuantity(order.quantity)
            .setStatus(order.status)
            .setCreatedAt(order.createdAt.toString())
            .build()
            .toJson()

        outboxRepository.save(
            OrderOutbox(
                orderId = order.id!!,
                eventType = EventTypes.ORDER_CREATED,
                payload = payload,
            )
        )

        return order
    }

    @Transactional
    suspend fun cancelOrder(orderId: String) {
        val id = UUID.fromString(orderId)
        val order = orderRepository.findById(id)
        if (order == null) {
            log.warn("Cannot cancel order [id={}]: not found", orderId)
            return
        }
        if (order.status == "CANCELLED") {
            log.info("Order [id={}] is already cancelled", orderId)
            return
        }
        orderRepository.save(order.copy(status = "CANCELLED"))
        log.info("Order cancelled [id={}]", orderId)
    }

    @Transactional
    suspend fun confirmOrder(orderId: String) {
        val id = UUID.fromString(orderId)
        val order = orderRepository.findById(id)
        if (order == null) {
            log.warn("Cannot confirm order [id={}]: not found", orderId)
            return
        }
        if (order.status == "CONFIRMED") {
            log.info("Order [id={}] is already confirmed", orderId)
            return
        }
        orderRepository.save(order.copy(status = "CONFIRMED"))
        log.info("Order confirmed [id={}]", orderId)
    }
}