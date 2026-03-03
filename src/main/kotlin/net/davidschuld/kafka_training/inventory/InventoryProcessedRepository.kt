package net.davidschuld.kafka_training.inventory

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

interface InventoryProcessedRepository : CoroutineCrudRepository<InventoryProcessed, UUID> {
    suspend fun existsByMessageId(messageId: String): Boolean
}
