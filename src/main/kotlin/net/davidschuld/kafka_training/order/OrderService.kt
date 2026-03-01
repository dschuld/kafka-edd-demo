package net.davidschuld.kafka_training.order

import net.davidschuld.kafka_training.order.OrderOutboxRepository
import net.davidschuld.kafka_training.order.OrderRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val outboxRepository: OrderOutboxRepository,
    private val objectMapper: ObjectMapper,
) {
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
}