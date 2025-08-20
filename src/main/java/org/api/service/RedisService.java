package org.api.service;

import org.api.dto.SimulationResponse;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.hash.HashCommands;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RedisService {

	private static final String INSTANCE = "singleton";

	private final HashCommands<String, String, SimulationResponse> commands;

	public RedisService(RedisDataSource ds) {
		commands = ds.hash(SimulationResponse.class);
	}

	public void set(String key, SimulationResponse value) {
		commands.hset(INSTANCE, key, value);
	}

	public SimulationResponse get(String key) {
		return commands.hget(INSTANCE, key);
	}
}