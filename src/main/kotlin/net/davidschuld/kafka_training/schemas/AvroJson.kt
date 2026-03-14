package net.davidschuld.kafka_training.schemas

import org.apache.avro.io.DecoderFactory
import org.apache.avro.io.EncoderFactory
import org.apache.avro.specific.SpecificDatumReader
import org.apache.avro.specific.SpecificDatumWriter
import java.io.ByteArrayOutputStream

fun OrderCreated.toJson(): String {
    val writer = SpecificDatumWriter<OrderCreated>(OrderCreated.getClassSchema())
    val out = ByteArrayOutputStream()
    val encoder = EncoderFactory.get().jsonEncoder(OrderCreated.getClassSchema(), out)
    writer.write(this, encoder)
    encoder.flush()
    return out.toString(Charsets.UTF_8)
}

fun orderCreatedFromJson(json: String): OrderCreated {
    val reader = SpecificDatumReader<OrderCreated>(OrderCreated.getClassSchema())
    val decoder = DecoderFactory.get().jsonDecoder(OrderCreated.getClassSchema(), json)
    return reader.read(null, decoder)
}
