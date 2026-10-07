package com.banco;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class TransferRepository {

    private final Connection conexion;

    public TransferRepository(Connection conexion) {
        this.conexion = conexion;
    }

    public void crearTablas() throws SQLException {
        try (Statement st = conexion.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS transferencias ("
                    + "correlation_id VARCHAR(50) PRIMARY KEY, "
                    + "cuenta_origen VARCHAR(30) NOT NULL, "
                    + "cuenta_destino VARCHAR(30) NOT NULL, "
                    + "valor NUMERIC(15,2) NOT NULL, "
                    + "fecha_carga TIMESTAMP NOT NULL DEFAULT now())");
            st.execute("CREATE TABLE IF NOT EXISTS transferencias_rechazadas ("
                    + "id BIGSERIAL PRIMARY KEY, "
                    + "numero_linea INT NOT NULL, "
                    + "correlation_id VARCHAR(50), "
                    + "contenido TEXT NOT NULL, "
                    + "motivo VARCHAR(20) NOT NULL, "
                    + "detalle TEXT NOT NULL, "
                    + "fecha_rechazo TIMESTAMP NOT NULL DEFAULT now())");
        }
    }

    /** Devuelve true si se guardó; false si el correlation_id ya existía (duplicado). */
    public boolean guardar(Transfer t) throws SQLException {
        String sql = "INSERT INTO transferencias (correlation_id, cuenta_origen, cuenta_destino, valor) "
                + "VALUES (?, ?, ?, ?) ON CONFLICT (correlation_id) DO NOTHING";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, t.getCorrelationId());
            ps.setString(2, t.getCuentaOrigen());
            ps.setString(3, t.getCuentaDestino());
            ps.setBigDecimal(4, t.getValor());
            return ps.executeUpdate() == 1;
        }
    }

    public void guardarRechazo(int numeroLinea, String correlationId, String contenido,
                               String motivo, String detalle) throws SQLException {
        String sql = "INSERT INTO transferencias_rechazadas (numero_linea, correlation_id, contenido, motivo, detalle) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, numeroLinea);
            ps.setString(2, correlationId);
            ps.setString(3, contenido);
            ps.setString(4, motivo);
            ps.setString(5, detalle);
            ps.executeUpdate();
        }
    }
}
