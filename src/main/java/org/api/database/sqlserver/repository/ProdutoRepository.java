package org.api.database.sqlserver.repository;

import java.math.BigDecimal;
import java.util.List;

import org.api.database.sqlserver.model.Produto;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProdutoRepository implements PanacheRepository<Produto> {

	public List<Produto> filterProducts(BigDecimal valor, int prazo) {
		return list(
				"VR_MINIMO <= ?1 and (VR_MAXIMO is null or VR_MAXIMO >= ?1) and NU_MINIMO_MESES <= ?2 and (NU_MAXIMO_MESES is null or NU_MAXIMO_MESES >= ?2)",
				valor, prazo);
	}
}
