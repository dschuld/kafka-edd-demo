package net.davidschuld.kafka_training.inventory

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("inventory_processed")
data class InventoryProcessed(
    @Id val id: UUID? = null,
    @Column("message_id") val messageId: String,
    val result: String,
)
