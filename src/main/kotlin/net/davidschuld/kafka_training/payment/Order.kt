package net.davidschuld.kafka_training.payment

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
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