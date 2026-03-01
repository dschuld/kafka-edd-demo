package net.davidschuld.kafka_training.order

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("orders")
data class Order(
    @Id val id: UUID? = null,
    @Column("customer_id") val customerId: String,
    val product: String,
    val quantity: Int,
    val status: String = "PENDING",
    @Column("created_at") val createdAt: OffsetDateTime = OffsetDateTime.now(),
)