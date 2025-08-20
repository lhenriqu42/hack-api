package org.api.model;

import java.math.BigDecimal;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "PRODUTO")
public class Produto extends PanacheEntityBase {

    @Id
    @Column(name = "CO_PRODUTO")
    public Integer codigo;

    @Column(name = "NO_PRODUTO", nullable = false, length = 200)
    public String nome;

    @Column(name = "PC_TAXA_JUROS", nullable = false, precision = 10, scale = 9)
    public BigDecimal taxaJurosMensal;

    @Column(name = "NU_MINIMO_MESES", nullable = false)
    public Short minimoMeses;

    @Column(name = "NU_MAXIMO_MESES")
    public Short maximoMeses;

    @Column(name = "VR_MINIMO", nullable = false, precision = 18, scale = 2)
    public BigDecimal valorMinimo;

    @Column(name = "VR_MAXIMO", precision = 18, scale = 2)
    public BigDecimal valorMaximo;
}
