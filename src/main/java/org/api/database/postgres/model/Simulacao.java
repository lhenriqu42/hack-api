package org.api.database.postgres.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.api.dto.QueueStruct;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "SIMULACAO")
public class Simulacao extends PanacheEntityBase {

	@Id
	@Column(name = "ID_SIMULACAO", nullable = false, unique = true)
	public Long id;

	@Column(name = "CO_PRODUTO", nullable = false)
	public Integer codigoProduto;

	@Column(name = "NO_PRODUTO", nullable = false, length = 200)
	public String nomeProduto;

	@Column(name = "PC_TAXA_JUROS", nullable = false, precision = 10, scale = 9)
	public BigDecimal taxaJuros;

	@Column(name = "VALOR_DESEJADO", nullable = false, precision = 18, scale = 2)
	public BigDecimal valorDesejado;

	@Column(name = "PRAZO", nullable = false)
	public Integer prazo;

	@Column(name = "DATA_REFERENCIA", nullable = false)
	public LocalDate dataReferencia;

	@Column(name = "VALOR_TOTAL_PARCELAS", nullable = false, precision = 18, scale = 2)
	public BigDecimal valorTotalParcelas;

	// Default constructor required by Hibernate (proxy/enhancement)
	protected Simulacao() {
	}

	public Simulacao(QueueStruct item) {
		this.id = item.simulacaoId();
		this.codigoProduto = item.codigoProduto();
		this.nomeProduto = item.nomeProduto();
		this.taxaJuros = item.taxaJurosMensal();
		this.valorDesejado = item.valorDesejado();
		this.prazo = item.prazo();
		this.dataReferencia = item.dataReferencia();
		this.valorTotalParcelas = item.valorTotalParcelas();
	}
}
