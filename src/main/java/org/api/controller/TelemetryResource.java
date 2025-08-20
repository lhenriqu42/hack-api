package org.api.controller;

import java.time.LocalDate;
import java.util.List;

import org.api.performance.MetricsCalculator.MetricsSnapshot;
import org.api.performance.MetricsManager;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/telemetria")
@Produces(MediaType.APPLICATION_JSON)
public class TelemetryResource {

    private record ResponseTelemetry(
            LocalDate dataReferencia,
            List<MetricsSnapshot> listaEndpoints) {
    }

    @Inject
    private MetricsManager metricsManager;

    @GET
    public ResponseTelemetry telemetria() {
        return new ResponseTelemetry(LocalDate.now(), metricsManager.getAllMetricsSnapshots());
    }
}
