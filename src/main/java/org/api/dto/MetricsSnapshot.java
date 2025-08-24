package org.api.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

// Snapshot imutável para transporte/JSON
@RegisterForReflection
public record MetricsSnapshot(
		String nomeApi,
		long qtdRequisicoes,
		long tempoMedio,
		long tempoMinimo,
		long tempoMaximo,
		double percentualSucesso,
		Long p99,
		Long p95) {
}