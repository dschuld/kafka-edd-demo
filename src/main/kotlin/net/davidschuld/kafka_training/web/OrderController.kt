package net.davidschuld.kafka_training.web

import net.davidschuld.kafka_training.domain.Order
import net.davidschuld.kafka_training.service.OrderService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

data class CreateOrderRequest(
    val customerId: String,
    val product: String,
    val quantity: Int,
)

@RestController
@RequestMapping("/orders")
class OrderController(private val orderService: OrderService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createOrder(@RequestBody request: CreateOrderRequest): Order =
        orderService.createOrder(request)
}
