package net.davidschuld.kafka_training.repository

import kotlinx.coroutines.flow.Flow
import net.davidschuld.kafka_training.domain.OrderOutbox
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

interface OrderOutboxRepository : CoroutineCrudRepository<OrderOutbox, UUID> {
    fun findByPublishedFalse(): Flow<OrderOutbox>
}
