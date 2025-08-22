package org.api.controller;

import java.time.LocalDate;

import org.api.dto.ResponseTelemetry;
import org.api.performance.MetricsManager;
import org.api.performance.anottations.TrackMetrics;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/telemetria")
@Produces(MediaType.APPLICATION_JSON)
@TrackMetrics
public class TelemetryResource {

	@Inject
	private MetricsManager metricsManager;

	@GET
	public ResponseTelemetry telemetria() {
		return new ResponseTelemetry(LocalDate.now(), metricsManager.getAllMetricsSnapshots());
	}
}
