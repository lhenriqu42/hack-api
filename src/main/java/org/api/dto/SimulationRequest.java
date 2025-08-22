package org.api.dto;

import java.math.BigDecimal;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@RegisterForReflection
public record SimulationRequest(
		@NotNull(message = "O valor desejado não pode ser nulo.") @Min(value = 1, message = "O valor desejado deve ser no mínimo 1.") BigDecimal valorDesejado,
		@Min(value = 1, message = "O prazo deve ser no mínimo 1.") int prazo) {
}
