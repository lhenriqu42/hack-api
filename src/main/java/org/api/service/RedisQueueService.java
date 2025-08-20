package org.api.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.list.ListCommands;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RedisQueueService {

    public record QueueStruct(
            long simulacaoId,
            Integer codigoProduto,
            String nomeProduto,
            BigDecimal taxaJurosMensal,
            BigDecimal valorDesejado,
            Integer prazo,
            LocalDate dataReferencia,
            BigDecimal valorTotalParcelas) {
    }

    private static final String QUEUE_NAME = "simulationQueue";

    private final ListCommands<String, QueueStruct> commands;

    public RedisQueueService(RedisDataSource ds) {
        commands = ds.list(QueueStruct.class);
    }

    /**
     * Adiciona um item à fila (no final da fila).
     */
    public void enqueue(QueueStruct value) {
        commands.rpush(QUEUE_NAME, value);
    }

    /**
     * Remove e retorna o primeiro item da fila (FIFO).
     */
    public QueueStruct dequeue() {
        return commands.lpop(QUEUE_NAME);
    }

    /**
     * Verifica o tamanho atual da fila.
     */
    public long size() {
        return commands.llen(QUEUE_NAME);
    }

    /**
     * Retorna o próximo item da fila sem removê-lo.
     */
    public QueueStruct peek() {
        return commands.lindex(QUEUE_NAME, 0);
    }
}