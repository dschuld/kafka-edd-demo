package net.davidschuld.kafka_training.inventory

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

interface InventoryOutboxRepository : CoroutineCrudRepository<InventoryOutbox, UUID> {
    fun findByPublishedFalse(): Flow<InventoryOutbox>
}
