package net.davidschuld.kafka_training.payment

enum class CrashSimulation {
    NONE,
    AFTER_READ,
    AFTER_PROCESSING,
    AFTER_SAVE,
}
