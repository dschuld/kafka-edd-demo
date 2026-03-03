package net.davidschuld.kafka_training.config

object EventTypes {
    const val ORDER_CREATED = "ORDER_CREATED"
    const val INVENTORY_RESERVED = "INVENTORY_RESERVED"
    const val RESERVATION_FAILED = "RESERVATION_FAILED"
    const val PAYMENT_SUCCESS = "PAYMENT_SUCCESS"
    const val PAYMENT_FAILED = "PAYMENT_FAILED"
}
