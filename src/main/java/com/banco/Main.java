package com.banco;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;

public class Main {

    private static final String URL = "jdbc:postgresql://localhost:5432/postgres";
    private static final String USUARIO = "postgres";

    public static void main(String[] args) {
        // La contraseña se toma de la variable de entorno DB_PASSWORD
        String password = System.getenv("DB_PASSWORD");
        String archivo = args.length > 0 ? args[0] : "entrada/transferencias.csv";

        try (Connection conexion = DriverManager.getConnection(URL, USUARIO, password)) {
            TransferRepository repository = new TransferRepository(conexion);
            repository.crearTablas();

            TransferService service = new TransferService(repository);
            System.out.println("Inicio del proceso de transferencia: " + archivo);
            service.procesarArchivo(Path.of(archivo));
            System.out.println("Fin del proceso");
        } catch (Exception e) {
            System.out.println("Error en el proceso: " + e.getMessage());
        }
    }
}
