package org.api.performance;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.api.dto.MetricsSnapshot;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Gerencia instâncias de MetricsCalculator, garantindo uma única instância por
 * endpoint.
 */
@ApplicationScoped
public class MetricsManager {

	private final Map<String, MetricsCalculator> metricsMap = new ConcurrentHashMap<>();

	/**
	 * Retorna a instância de MetricsCalculator para o endpoint especificado.
	 * Se não existir, cria uma nova.
	 *
	 * @param endpoint Nome do endpoint (ex: "/api/endpoint")
	 * @return Instância de MetricsCalculator
	 */
	public MetricsCalculator getMetric(String endpoint) {
		return metricsMap.computeIfAbsent(endpoint, key -> {
			return new MetricsCalculator(endpoint);
		});
	}

	/**
	 * Retorna uma lista de snapshots de métricas para todos os endpoints
	 * registrados.
	 *
	 * @return Lista de MetricsCalculator
	 */
	public List<MetricsSnapshot> getAllMetricsSnapshots() {
		return metricsMap.values().stream()
				.map(MetricsCalculator::snapshot)
				.collect(Collectors.toList());
	}
}