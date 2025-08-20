package org.api.dto;

import java.math.BigDecimal;

public record SimulationRequest(
		BigDecimal valorDesejado,
		int prazo) {
}
