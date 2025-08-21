package org.api.event;

// Placeholder para produtor de eventos EventHub.
// Em produção, crie um @ApplicationScoped bean que inicializa o EventHubProducerClient
// com a connection-string via config e envie o JSON da simulação de forma assíncrona.
import java.util.concurrent.CompletableFuture;

import org.api.service.RedisQueueService.QueueStruct;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubProducerClient;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EventHubProducer {

	private static final Logger LOG = Logger.getLogger(EventHubProducer.class);

	@ConfigProperty(name = "eventhub.connection-string", defaultValue = "")
	String connectionString;

	private EventHubProducerClient client;

    @PostConstruct
    void init() {
        LOG.info("Inicializando EventHubProducer...");
        client = new EventHubClientBuilder()
                .connectionString(connectionString)
                .buildProducerClient();
    }

	/**
	 * Envia um payload JSON para o Event Hub.
	 * Retorna um CompletableFuture que completa quando o envio terminar (ou
	 * falhar).
	 */
	public CompletableFuture<Void> sendJson(QueueStruct item) {
		if (client == null) {
			LOG.warn("EventHubProducerClient não inicializado. Não será possível enviar eventos.");
			return CompletableFuture.failedFuture(new IllegalStateException("EventHubProducerClient não inicializado"));
		}

		String json = item.toJsonString();

		try {
			EventData ev = new EventData(json.getBytes());
			ev.setContentType("application/json");
			client.send(java.util.List.of(ev));
			LOG.infov("Evento enviado para EventHub (tamanho={0})", json.length());
			return CompletableFuture.completedFuture(null);
		} catch (Exception ex) {
			LOG.error("Falha ao enviar evento para EventHub", ex);
			return CompletableFuture.failedFuture(ex);
		}
	}
}
