package org.api.database.sqlserver.repository;

import java.math.BigDecimal;
import java.util.List;

import org.api.database.sqlserver.model.Produto;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProdutoRepository implements PanacheRepository<Produto> {

	public List<Produto> filterProducts(BigDecimal valor, int prazo) {
		// Use entity property names (not DB column names) in JPQL/HQL so Hibernate can resolve paths
		return list(
				"valorMinimo <= ?1 and (valorMaximo is null or valorMaximo >= ?1) and minimoMeses <= ?2 and (maximoMeses is null or maximoMeses >= ?2)",
				valor, prazo);
	}
}
