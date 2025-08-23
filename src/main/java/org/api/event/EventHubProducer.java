package org.api.event;

import java.util.ArrayList;
import java.util.List;

import org.api.dto.QueueStruct;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubProducerClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class EventHubProducer {

	private static final Logger LOG = Logger.getLogger(EventHubProducer.class);

	@ConfigProperty(name = "eventhub.connection-string", defaultValue = "")
	String connectionString;

	@Inject
	ObjectMapper mapper;

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
	 */
	public void sendItens(List<QueueStruct> itens, int batchSize) {

		List<EventData> batch = new ArrayList<>(batchSize);

		for (QueueStruct item : itens) {
			try {
				String json = mapper.writeValueAsString(item);
				EventData ev = new EventData(json.getBytes());
				ev.setContentType("application/json");
				batch.add(ev);
			} catch (Exception ex) {
				LOG.errorf(ex, "Falha ao serializar simulacaoId=%s", item.simulacaoId());
			}
		}

		if (!batch.isEmpty()) {
			client.send(batch);
		}
	}
}
