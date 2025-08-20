package org.api.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SimulationRequest(
		@NotNull @Min(1) BigDecimal valorDesejado,
		@Min(1) int prazo) {
}
