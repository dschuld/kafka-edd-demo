package net.davidschuld.kafka_training.order

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

interface OrderProcessedRepository : CoroutineCrudRepository<OrderProcessed, UUID> {
    suspend fun existsByMessageId(messageId: String): Boolean
}
