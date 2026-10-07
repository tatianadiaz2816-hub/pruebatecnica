package com.banco;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TransferService {

    private final TransferRepository repository;

    public TransferService(TransferRepository repository) {
        this.repository = repository;
    }

    /** Devuelve la lista de errores; si está vacía, la transferencia es válida. */
    public List<String> validar(Transfer transfer) {
        List<String> errores = new ArrayList<>();

        if (transfer.getCorrelationId() == null || transfer.getCorrelationId().isBlank()) {
            errores.add("ID de correlación vacío");
        }

        if (transfer.getCuentaOrigen() == null || transfer.getCuentaOrigen().isBlank()) {
            errores.add("cuenta origen vacía");
        }

        if (transfer.getCuentaDestino() == null || transfer.getCuentaDestino().isBlank()) {
            errores.add("cuenta destino vacía");
        }

        if (transfer.getCuentaOrigen() != null && transfer.getCuentaOrigen().equals(transfer.getCuentaDestino())) {
            errores.add("cuenta origen y destino son iguales");
        }

        if (transfer.getValor() == null || transfer.getValor().signum() <= 0) {
            errores.add("valor inválido");
        }

        return errores;
    }

    /** Lee el archivo CSV (con cabecera), valida cada línea y guarda o rechaza. */
    public void procesarArchivo(Path archivo) throws IOException, SQLException {
        List<String> lineas = Files.readAllLines(archivo);
        int guardadas = 0, invalidas = 0, duplicadas = 0;

        for (int i = 1; i < lineas.size(); i++) {
            String linea = lineas.get(i);
            if (linea.isBlank()) {
                continue;
            }
            int numeroLinea = i + 1;
            String[] campos = linea.split(",", -1);
            String correlationId = campos[0].trim().isEmpty() ? null : campos[0].trim();

            if (campos.length != 4) {
                rechazar(numeroLinea, correlationId, linea, "INVALIDO", "Se esperaban 4 columnas");
                invalidas++;
                continue;
            }

            BigDecimal valor = null;
            try {
                valor = new BigDecimal(campos[3].trim());
            } catch (NumberFormatException e) {
                // queda null y validar() lo reporta como "valor inválido"
            }

            Transfer transfer = new Transfer(correlationId, campos[1].trim(), campos[2].trim(), valor);
            List<String> errores = validar(transfer);

            if (!errores.isEmpty()) {
                rechazar(numeroLinea, correlationId, linea, "INVALIDO", String.join("; ", errores));
                invalidas++;
            } else if (!repository.guardar(transfer)) {
                rechazar(numeroLinea, correlationId, linea, "DUPLICADO", "correlation_id ya existe");
                duplicadas++;
            } else {
                guardadas++;
            }
        }

        System.out.println("Resumen -> guardadas: " + guardadas
                + ", inválidas: " + invalidas + ", duplicadas: " + duplicadas);
    }

    private void rechazar(int numeroLinea, String correlationId, String linea, String motivo, String detalle)
            throws SQLException {
        System.out.println("Línea " + numeroLinea + " rechazada (" + motivo + "): " + detalle);
        repository.guardarRechazo(numeroLinea, correlationId, linea, motivo, detalle);
    }
}
