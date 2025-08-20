package org.api.controller;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/telemetria")
@Produces(MediaType.APPLICATION_JSON)
public class TelemetryResource {

    @GET
    public Map<String, Object> telemetria() {
        // Esqueleto simples; em produção usar Micrometer/Prometheus
        return Map.of(
                "dataReferencia", java.time.LocalDate.now().toString(),
                "listaEndpoints", List.of(
                        Map.of(
							"nomeApi", "Simulacao",
							"qtdRequisicoes", 0,
							"tempoMedio", 0,
							"tempoMinimo", 0,
							"tempoMaximo", 0,
							"percentualSucesso", 1.0
                        )
                )
        );
    }
}
