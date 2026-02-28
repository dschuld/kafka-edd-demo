package net.davidschuld.kafka_training.repository

import net.davidschuld.kafka_training.domain.Order
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

interface OrderRepository : CoroutineCrudRepository<Order, UUID>
