package com.invoices.invoice.domain.ports;

import com.invoices.invoice.domain.entities.Client;

/**
 * Port for publishing client events.
 *
 * This is a domain interface that will be implemented by infrastructure layer.
 * Keeps domain layer independent of event infrastructure (Redis, Kafka, etc.).
 *
 * Implementations are responsible for:
 * - Serializing events
 * - Publishing to message broker (Redis, Kafka, RabbitMQ, etc.)
 * - Handling publish failures (logging, retrying, etc.)
 */
public interface ClientEventPublisher {

    /**
     * Publishes an event when a client is created.
     *
     * @param client the created client
     */
    void publishClientCreated(Client client);

    /**
     * Publishes an event when a client is updated.
     *
     * @param client the updated client
     */
    void publishClientUpdated(Client client);

    /**
     * Publishes an event when a client is deleted.
     *
     * @param client the deleted client
     */
    void publishClientDeleted(Client client);
}
