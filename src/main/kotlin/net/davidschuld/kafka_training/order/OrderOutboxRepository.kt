package net.davidschuld.kafka_training.order

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

interface OrderOutboxRepository : CoroutineCrudRepository<OrderOutbox, UUID> {
    fun findByPublishedFalse(): Flow<OrderOutbox>
}