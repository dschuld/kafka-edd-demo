package net.davidschuld.kafka_training.schemas

import org.apache.avro.Schema
import org.apache.avro.io.DecoderFactory
import org.apache.avro.io.EncoderFactory
import org.apache.avro.specific.SpecificDatumReader
import org.apache.avro.specific.SpecificDatumWriter
import org.apache.avro.specific.SpecificRecord
import java.io.ByteArrayOutputStream

fun SpecificRecord.toJson(): String {
    val writer = SpecificDatumWriter<SpecificRecord>(schema)
    val out = ByteArrayOutputStream()
    val encoder = EncoderFactory.get().jsonEncoder(schema, out)
    writer.write(this, encoder)
    encoder.flush()
    return out.toString(Charsets.UTF_8)
}

inline fun <reified T : SpecificRecord> avroFromJson(json: String): T {
    val schema = T::class.java.getField("SCHEMA\$").get(null) as Schema
    val reader = SpecificDatumReader<T>(schema)
    val decoder = DecoderFactory.get().jsonDecoder(schema, json)
    return reader.read(null, decoder)
}
