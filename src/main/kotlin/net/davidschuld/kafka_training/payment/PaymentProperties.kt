package net.davidschuld.kafka_training.payment

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "payment")
data class PaymentProperties(
    val crashSimulation: CrashSimulation = CrashSimulation.NONE,
)
