package com.invoices.trace.events;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * TODO: Adaptar este test para usar Redis Streams en lugar de Kafka
 * 
 * Este test fue migrado del microservicio trace-service pero usa Kafka.
 * El monolito usa Redis Streams para eventos, por lo que este test
 * debe ser reescrito para validar el consumo de eventos desde Redis.
 */
@Disabled("Pendiente: Adaptar para Redis Streams en lugar de Kafka")
class InvoiceEventConsumerTest {

    @Test
    void placeholder() {
        // TODO: Implementar tests para Redis Streams consumer
    }
}
