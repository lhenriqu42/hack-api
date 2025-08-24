package org.api.worker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.api.database.postgres.model.Simulacao;
import org.api.database.postgres.repository.SimulacaoRepository;
import org.api.dto.QueueStruct;
import org.api.event.EventHubProducer;
import org.api.service.RedisQueueService;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.logging.Log;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class SimulationQueueWorker {

	@ConfigProperty(name = "queue.batch.size", defaultValue = "100")
	int batchSize;

	@Inject
	RedisQueueService redisService;

	@Inject
	EventHubProducer eventHubProducer;

	@Inject
	SimulacaoRepository simulacaoRepository;

	private ExecutorService executor;
	private final AtomicBoolean running = new AtomicBoolean(false);

	public void onStart(@Observes StartupEvent ev) {
		executor = Executors.newFixedThreadPool(1); // Somente 1 thread para processar a fila nesse caso é o suficiente
		running.set(true);
		executor.submit(this::loop);
	}

	private void loop() {
		while (running.get()) {
			try {
				List<QueueStruct> itens = redisService.dequeueBatch(batchSize);
				insertInPostgres(itens);
				// sendEvent(itens);
			} catch (Exception e) {
				Log.error("Falha processando item da fila", e);
			}
		}
	}

	void sendEvent(List<QueueStruct> itens) {
		if (itens.isEmpty())
			return;
		eventHubProducer.sendItens(itens);
	}

	@ActivateRequestContext
	@Transactional
	void insertInPostgres(List<QueueStruct> itens) {
		if (itens == null || itens.isEmpty())
			return;

		List<Simulacao> batch = new ArrayList<>(batchSize);

		for (QueueStruct item : itens) {
			batch.add(new Simulacao(item));
		}

		simulacaoRepository.persist(batch);
		simulacaoRepository.flush();
	}

	public void onStop(@Observes ShutdownEvent ev) {
		running.set(false);
		if (executor != null) {
			executor.shutdownNow();
		}
		Log.info("SimulationQueueWorker finalizado");
	}
}