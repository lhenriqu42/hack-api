package org.api.worker;

import java.time.Duration;
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

	private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(5);

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
		int threads = workers;
		executor = Executors.newFixedThreadPool(threads);
		running.set(true);
		executor.submit(this::loop);
		Log.infof("SimulationQueueWorker iniciado (%d thread)", threads);
	}

	private void loop() {
		while (running.get()) {
			try {
				QueueStruct item = redisService.dequeue(WAIT_TIMEOUT);
				if (item == null) {
					continue;
				}
				insertInPostgres(item);
				// sendEvent(item);
			} catch (Exception e) {
				Log.error("Falha processando item da fila", e);
			}
		}
	}

	@Transactional
	void sendEvent(QueueStruct item) {
		eventHubProducer.sendJson(item)
				.whenComplete((res, ex) -> {
					if (ex != null) {
						Log.error("Falha ao enviar evento para EventHub", ex);
					} else {
						Log.infov("Evento enviado para EventHub: simulacaoId={0}", item.simulacaoId());
					}
				});
	}

	@Transactional
	void insertInPostgres(QueueStruct item) {
		simulacaoRepository.persist(new Simulacao(item));
	}

	public void onStop(@Observes ShutdownEvent ev) {
		running.set(false);
		if (executor != null) {
			executor.shutdownNow();
		}
		Log.info("SimulationQueueWorker finalizado");
	}
}