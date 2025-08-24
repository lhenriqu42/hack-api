package org.api.performance;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import org.api.dto.MetricsSnapshot;

/**
 * Coletor simples de métricas por endpoint/API.
 * Armazena:
 * - total de requisições
 * - total de sucessos (HTTP 200, ou conforme informado)
 * - tempo total acumulado (ms)
 * - tempo mínimo / máximo (ms)
 *
 * Thread-safe e lock-free (usa LongAdder / AtomicLong).
 */
public class MetricsCalculator {

	private final String nomeApi;

	private final LongAdder totalRequisicoes = new LongAdder();
	private final LongAdder sucessos = new LongAdder();
	private final LongAdder tempoTotalMillis = new LongAdder();
	private final AtomicLong tempoMinimoMillis = new AtomicLong(Long.MAX_VALUE);
	private final AtomicLong tempoMaximoMillis = new AtomicLong(Long.MIN_VALUE);

	public MetricsCalculator(String nomeApi) {
		this.nomeApi = nomeApi;
	}

	/**
	 * Marca o início de uma medição (usa nanoTime para precisão).
	 */
	public long  startTimer() {
		return System.nanoTime();
	}

	/**
	 * Finaliza a medição a partir do valor retornado por startTimer().
	 * 
	 * @param sucesso true se a requisição foi considerada sucesso (ex: status 200)
	 */
	public void stopTimer(long startTime, boolean sucesso) {
		long durNanos = System.nanoTime() - startTime;
		long durMillis = TimeUnit.NANOSECONDS.toMillis(durNanos);
		record(durMillis, sucesso);
	}

	/**
	 * Registra uma requisição com a duração em milissegundos.
	 */
	private void record(long duracaoMillis, boolean sucesso) {
		totalRequisicoes.increment();
		tempoTotalMillis.add(duracaoMillis);
		// Atualiza min
		atualizarMin(duracaoMillis);
		// Atualiza max
		atualizarMax(duracaoMillis);
		if (sucesso) {
			sucessos.increment();
		}
	}

	private void atualizarMin(long valor) {
		long prev;
		do {
			prev = tempoMinimoMillis.get();
			if (valor >= prev)
				return;
		} while (!tempoMinimoMillis.compareAndSet(prev, valor));
	}

	private void atualizarMax(long valor) {
		long prev;
		do {
			prev = tempoMaximoMillis.get();
			if (valor <= prev)
				return;
		} while (!tempoMaximoMillis.compareAndSet(prev, valor));
	}

	public MetricsSnapshot snapshot() {
		long total = totalRequisicoes.sum();
		long sucesso = sucessos.sum();
		long soma = tempoTotalMillis.sum();
		long min = tempoMinimoMillis.get();
		long max = tempoMaximoMillis.get();

		if (total == 0) {
			return new MetricsSnapshot(
					nomeApi,
					0,
					0L,
					0L,
					0L,
					0.0);
		}

		if (min == Long.MAX_VALUE)
			min = 0L;
		if (max == Long.MIN_VALUE)
			max = 0L;

		long media = soma / total;
		double percentualSucesso = (sucesso * 100.0) / total;
		percentualSucesso = Math.round(percentualSucesso * 100.0) / 100.0;

		return new MetricsSnapshot(
				nomeApi,
				total,
				media,
				min,
				max,
				percentualSucesso);
	}
}