package net.davidschuld.kafka_training.order

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

interface OrderRepository : CoroutineCrudRepository<Order, UUID>