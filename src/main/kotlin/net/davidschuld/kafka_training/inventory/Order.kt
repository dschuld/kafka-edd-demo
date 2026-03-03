package net.davidschuld.kafka_training.inventory

import java.time.OffsetDateTime
import java.util.UUID

data class Order(
    val id: UUID? = null,
    val customerId: String,
    val product: String,
    val quantity: Int,
    val status: String = "PENDING",
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
