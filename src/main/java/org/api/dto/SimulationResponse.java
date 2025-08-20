package org.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record SimulationResponse(
		long idSimulacao,
		int codigoProduto,
		String descricaoProduto,
		BigDecimal taxaJuros,
		List<ResultadoDTO> resultadoSimulacao) {
}
