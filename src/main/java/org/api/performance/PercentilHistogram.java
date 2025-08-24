package org.api.performance;

import java.util.concurrent.atomic.LongAdder;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PercentilHistogram {

	// Definição dos limites (buckets)
	private final long[] limites = { 10, 50, 100, 500, 1000, 5000, Long.MAX_VALUE };
	private final LongAdder[] contadores = new LongAdder[limites.length];

	public PercentilHistogram() {
		for (int i = 0; i < contadores.length; i++) {
			contadores[i] = new LongAdder();
		}
	}

	/** Registra a duração de uma requisição em ms */
	public void registrar(long duracaoMs) {
		for (int i = 0; i < limites.length; i++) {
			if (duracaoMs <= limites[i]) {
				contadores[i].increment();
				break;
			}
		}
	}

	/** Calcula um percentil aproximado (ex: 95 ou 99) */
	public long calcularPercentil(double percentil) {
		long total = 0;
		for (LongAdder c : contadores) {
			total += c.sum();
		}
		if (total == 0)
			return 0;

		long alvo = (long) Math.ceil((percentil / 100.0) * total);

		long acumulado = 0;
		for (int i = 0; i < limites.length; i++) {
			acumulado += contadores[i].sum();
			if (acumulado >= alvo) {
				return limites[i]; // retorno aproximado = limite do bucket
			}
		}
		return limites[limites.length - 1];
	}

	public long p95() {
		return calcularPercentil(95.0);
	}

	public long p99() {
		return calcularPercentil(99.0);
	}
}
