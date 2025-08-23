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
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class SimulationQueueWorker {

	private static final int BATCH_SIZE = 100;

	@ConfigProperty(name = "num.workers.process", defaultValue = "2")
	int workers;

	@Inject
	RedisQueueService redisService;

	@Inject
	EventHubProducer eventHubProducer;

	@Inject
	SimulacaoRepository simulacaoRepository;

	private ExecutorService executor;
	private final AtomicBoolean running = new AtomicBoolean(false);

	public void onStart(@Observes StartupEvent ev) {
		executor = Executors.newFixedThreadPool(workers);
		running.set(true);
		for (int i = 0; i < workers; i++) {
			executor.submit(this::loop);
		}
	}

	private void loop() {
		while (running.get()) {
			try {
				List<QueueStruct> itens = redisService.dequeueBatch(BATCH_SIZE);
				insertInPostgres(itens);
				// sendEvent(itens);
			} catch (Exception e) {
				Log.error("Falha processando item da fila", e);
			}
		}
	}

	@Transactional
	void sendEvent(List<QueueStruct> itens) {
		if (itens.isEmpty())
			return;
		eventHubProducer.sendItens(itens, BATCH_SIZE);
	}

	void insertInPostgres(List<QueueStruct> itens) {
		if (itens == null || itens.isEmpty())
			return;

		int batchSize = 50; // Ajuste conforme DB/memória
		List<Simulacao> batch = new ArrayList<>(batchSize);

		var em = simulacaoRepository.getEntityManager();

		int count = 0;
		for (QueueStruct item : itens) {
			batch.add(new Simulacao(item));
			count++;

			if (count % batchSize == 0) {
				// Persistir batch
				batch.forEach(em::persist);
				em.flush();
				em.clear();
				batch.clear();
			}
		}

		// Persistir o que sobrou
		batch.forEach(em::persist);
		em.flush();
		em.clear();
	}

	public void onStop(@Observes ShutdownEvent ev) {
		running.set(false);
		if (executor != null) {
			executor.shutdownNow();
		}
		Log.info("SimulationQueueWorker finalizado");
	}
}