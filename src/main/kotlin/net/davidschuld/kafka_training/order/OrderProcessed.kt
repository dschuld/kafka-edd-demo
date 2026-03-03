package net.davidschuld.kafka_training.order

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("order_events_processed")
data class OrderProcessed(
    @Id val id: UUID? = null,
    @Column("message_id") val messageId: String,
    val result: String,
)
