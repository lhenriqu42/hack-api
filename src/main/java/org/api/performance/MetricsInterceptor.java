package org.api.performance;

import org.api.performance.anottations.TrackMetrics;

import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@TrackMetrics
@Interceptor
public class MetricsInterceptor {

	@Inject
	MetricsManager metricsManager;

	@Context
	UriInfo uriInfo;

	@AroundInvoke
	public Object track(InvocationContext ctx) throws Exception {
		String path = uriInfo.getPath();
		var metric = metricsManager.getMetric(path);
		Long inicio = metric.startTimer();

		try {
			Object result = ctx.proceed();
			boolean success = true;
			if (result instanceof Response response) {
				int status = response.getStatus();
				success = status >= 200 && status < 400;
			}
			metric.stopTimer(inicio, success);
			return result;
		} catch (Exception e) {
			metric.stopTimer(inicio, false);
			throw e;
		}
	}
}
