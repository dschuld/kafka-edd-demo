package net.davidschuld.kafka_training.order

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("order_outbox")
data class OrderOutbox(
    @Id val id: UUID? = null,
    @Column("order_id") val orderId: UUID,
    @Column("event_type") val eventType: String,
    val payload: String,
    val published: Boolean = false,
    @Column("created_at") val createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column("published_at") val publishedAt: OffsetDateTime? = null,
)