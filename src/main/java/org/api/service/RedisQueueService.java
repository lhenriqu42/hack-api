package org.api.service;

import java.math.BigDecimal;
import java.time.Duration;
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

		public String toJsonString() {
			return """
					{
					    "simulacaoId": %d,
					    "codigoProduto": %d,
					    "nomeProduto": "%s",
					    "taxaJurosMensal": %s,
					    "valorDesejado": %s,
					    "prazo": %d,
					    "dataReferencia": "%s",
					    "valorTotalParcelas": %s
					}
					""".formatted(
					simulacaoId(),
					codigoProduto(),
					nomeProduto(),
					taxaJurosMensal(),
					valorDesejado(),
					prazo(),
					dataReferencia(),
					valorTotalParcelas());
		}
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
	 * Remove e retorna o primeiro item da fila (FIFO).
	 * Se a fila estiver vazia, aguarda até que um item esteja disponível ou até que
	 * o tempo limite seja atingido.
	 */
	public QueueStruct dequeue(Duration timeout) {
		var item = commands.blpop(timeout, QUEUE_NAME);
		if (item == null) {
			return null; // timeout atingido, fila vazia
		}
		return item.value();
	}

	/**
	 * Verifica o tamanho atual da fila.
	 */
	public long size() {
		return commands.llen(QUEUE_NAME);
	}

}