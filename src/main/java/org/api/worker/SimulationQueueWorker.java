package org.api.worker;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.api.model.Simulacao;
import org.api.service.RedisQueueService;
import org.api.service.RedisQueueService.QueueStruct;

import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class SimulationQueueWorker {

    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(5);

    @Inject
    RedisQueueService redisService;

    // Se precisar persistir no banco, injete aqui seu repository/service (@Inject SimulacaoRepository repo)

    private ExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @PostConstruct
    void start() {
        int threads = 1; // manter 1 enquanto simples; aumentar depois se necessário
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
                    continue; // sem item, volta a esperar
                }
                long ini = System.nanoTime();
                processItem(item);
                Log.debugf("Item simulacaoId=%d processado em %d ms",
                        item.simulacaoId(),
                        (System.nanoTime() - ini) / 1_000_000);
            } catch (Exception e) {
                Log.error("Falha processando item da fila", e);
                // simples: ignora e segue. (Melhoria futura: mandar para DLQ)
            }
        }
    }

    @Transactional
    void processItem(QueueStruct item) {
        Simulacao s = new Simulacao();
        s.id = item.simulacaoId();
        s.codigoProduto = item.codigoProduto();
        s.nomeProduto = item.nomeProduto();
        s.taxaJuros = item.taxaJurosMensal();
        s.valorDesejado = item.valorDesejado();
        s.prazo = item.prazo();
        s.dataReferencia = item.dataReferencia();
        s.valorTotalParcelas = item.valorTotalParcelas();

        s.persist();

        //send event in eventhub(SimulationResponse)

    }

    @PreDestroy
    void stop() {
        running.set(false);
        if (executor != null) {
            executor.shutdownNow();
        }
        Log.info("SimulationQueueWorker finalizado");
    }
}