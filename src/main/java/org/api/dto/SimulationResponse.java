package org.api.dto;

import java.math.BigDecimal;
import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record SimulationResponse(
		long idSimulacao,
		int codigoProduto,
		String descricaoProduto,
		BigDecimal taxaJuros,
		List<ResultadoDTO> resultadoSimulacao) {
}
