package org.api.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.api.dto.QueueStruct;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.list.KeyValue;
import io.quarkus.redis.datasource.list.ListCommands;
import io.quarkus.redis.datasource.list.Position;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RedisQueueService {

	private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(1);
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
		try {
			var item = commands.blpop(timeout, QUEUE_NAME);
			if (item == null) {
				return null; // timeout atingido, fila vazia
			}
			return item.value();
		} catch (Exception e) {
			return null; // Em caso de erro, retorna null
		}
	}

	/**
	 * Remove e retorna um lote de itens da fila (FIFO).
	 */
	public List<QueueStruct> dequeueBatch(int batchSize) {
		try {

			List<KeyValue<String, QueueStruct>> items = commands.blmpop(DEFAULT_TIMEOUT, Position.LEFT, batchSize,
					QUEUE_NAME);
			if (items == null || items.isEmpty()) {
				return List.of(); // fila vazia
			}
			List<QueueStruct> result = new ArrayList<>();
			for (KeyValue<String, QueueStruct> kv : items) {
				result.add(kv.value());
			}
			return result;
		} catch (Exception e) {
			return List.of(); // Em caso de erro, retorna lista vazia
		}
	}

	/**
	 * Verifica o tamanho atual da fila.
	 */
	public long size() {
		return commands.llen(QUEUE_NAME);
	}

}