package net.davidschuld.kafka_training

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class KafkaTrainingApplication

fun main(args: Array<String>) {
	runApplication<KafkaTrainingApplication>(*args)
}
