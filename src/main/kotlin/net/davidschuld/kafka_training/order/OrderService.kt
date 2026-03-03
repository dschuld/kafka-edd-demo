package net.davidschuld.kafka_training.order

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val outboxRepository: OrderOutboxRepository,
    private val objectMapper: ObjectMapper,
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

        val payload = objectMapper.writeValueAsString(order)

        outboxRepository.save(
            OrderOutbox(
                orderId = order.id!!,
                eventType = "ORDER_CREATED",
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
}