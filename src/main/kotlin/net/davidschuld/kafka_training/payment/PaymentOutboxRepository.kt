package net.davidschuld.kafka_training.payment

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

interface PaymentOutboxRepository : CoroutineCrudRepository<PaymentOutbox, UUID> {
    fun findByPublishedFalse(): Flow<PaymentOutbox>
}
