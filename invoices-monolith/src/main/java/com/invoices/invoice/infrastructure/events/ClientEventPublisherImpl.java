package com.invoices.invoice.infrastructure.events;

import com.invoices.invoice.domain.entities.Client;
import com.invoices.invoice.domain.ports.ClientEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ClientEventPublisherImpl implements ClientEventPublisher {

    @Override
    public void publishClientCreated(Client client) {
        log.info("Client created event published for client: {}", client.getId());
    }

    @Override
    public void publishClientUpdated(Client client) {
        log.info("Client updated event published for client: {}", client.getId());
    }

    @Override
    public void publishClientDeleted(Client client) {
        log.info("Client deleted event published for client: {}", client.getId());
    }
}
