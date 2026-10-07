package com.banco;

import java.math.BigDecimal;

public class Transfer {

    private String correlationId;
    private String cuentaOrigen;
    private String cuentaDestino;
    private BigDecimal valor;

    public Transfer(String correlationId, String cuentaOrigen, String cuentaDestino, BigDecimal valor) {
        this.correlationId = correlationId;
        this.cuentaOrigen = cuentaOrigen;
        this.cuentaDestino = cuentaDestino;
        this.valor = valor;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getCuentaOrigen() {
        return cuentaOrigen;
    }

    public String getCuentaDestino() {
        return cuentaDestino;
    }

    public BigDecimal getValor() {
        return valor;
    }
}
