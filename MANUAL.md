# Manual paso a paso: Procesador de transferencias (prueba técnica, 40 min)

**Qué vamos a construir:** una aplicación Java que lee un archivo plano (CSV) que llega por SFTP/GoAnywhere,
valida cada línea, guarda las válidas en PostgreSQL y registra las inválidas o duplicadas en otra tabla.

**Flujo para decirle al entrevistador:**
GoAnywhere/SFTP deja el archivo en una carpeta → mi programa Java lo lee → valida → guarda en PostgreSQL →
lo compruebo en DBeaver → versiono con Git.

| Min | Parte |
|---|---|
| 0-4 | Parte 1: archivo plano |
| 4-9 | Parte 2: DBeaver, crear la base de datos |
| 9-14 | Parte 3: IntelliJ, crear el proyecto Maven |
| 14-30 | Parte 4: crear las 4 clases |
| 30-34 | Parte 5: ejecutar |
| 34-38 | Parte 6: verificar en DBeaver |
| 38-40 | Parte 7: Git |

---

## PARTE 1. El archivo plano (lo creas dentro de IntelliJ, en la Parte 3)

Es un archivo de texto con una línea por transferencia. Los campos van separados por coma:

```
correlation_id,cuenta_origen,cuenta_destino,valor
TRX-001,100,200,500000
TRX-002,101,201,150000.50
TRX-003,102,202,75000
TRX-001,100,200,500000
TRX-004,,203,20000
TRX-005,104,104,30000
TRX-006,105,205,-100
TRX-007,106,206,abc
,107,207,40000
TRX-008,108,208
TRX-009,109,209,90000
```

Qué decir:
- La primera línea es la **cabecera** (nombres de columnas). El programa la salta.
- `correlation_id` es el identificador único de cada transferencia.
- Hay casos buenos y malos a propósito:
  - `TRX-001` aparece dos veces: **duplicado**.
  - `TRX-004` no tiene cuenta origen: **campo obligatorio vacío**.
  - `TRX-005` tiene origen igual a destino: **inválido**.
  - `TRX-006` tiene valor negativo y `TRX-007` tiene valor "abc": **inválidos**.
  - La línea sin ID, y `TRX-008` con solo 3 campos: **inválidas**.
- Resultado esperado: **3 guardadas, 7 inválidas, 1 duplicada**.

---

## PARTE 2. DBeaver: crear la base de datos

### 2.1 Crear la conexión
1. Abre **DBeaver**.
2. En la barra superior, arriba a la izquierda, pulsa el **icono del enchufe con un +** ("Nueva conexión de base de datos").
   Otra opción: menú **Base de datos → Nueva conexión de base de datos**.
3. En la lista, elige **PostgreSQL** y pulsa **Siguiente**.
4. Rellena:
   - **Host:** `localhost`
   - **Puerto:** `5432`
   - **Base de datos:** `postgres`
   - **Usuario:** `postgres`
   - **Contraseña:** la clave de tu usuario `postgres`
5. Abajo a la izquierda pulsa **Probar conexión**.
   - Si pide descargar el driver, pulsa **Descargar**.
   - Debe salir "Conectado".
6. Pulsa **Finalizar**. La conexión aparece en el panel izquierdo.

### 2.2 Base de datos: usar `postgres` (la más rápida, sin crear nada)
Si DBeaver muestra "Cannot create a database when multi-database mode is disabled", **no crees nada**:
el proyecto ya usa la base `postgres` que viene instalada, y las tablas quedan en `postgres → Esquemas → public`.
Salta directo a la Parte 3. En las consultas de la Parte 6 trabaja sobre la conexión `postgres`.

*(Opcional)* Si prefieres una base propia, sigue 2.2b y cambia `postgres` por `pruebatecnica` en la URL de `Main`.

### 2.2b Crear la base de datos `pruebatecnica` (opcional)
**Con clic (gráfico):**
1. En el panel izquierdo, haz **clic derecho sobre la conexión `postgres`**.
2. Elige **Crear → Base de datos**.
3. En **Nombre** escribe `pruebatecnica` y pulsa **Aceptar**.

**Con SQL (alternativa):**
1. Selecciona la conexión y pulsa `Ctrl+]` (abre un editor SQL nuevo).
2. Escribe:
   ```sql
   CREATE DATABASE pruebatecnica;
   ```
3. Pulsa `Ctrl+Enter` para ejecutar.

Después haz clic derecho sobre la conexión → **Refrescar** (`F5`). Debe aparecer `pruebatecnica`.

### 2.3 Apuntar la conexión a la base nueva
1. Clic derecho en la conexión → **Editar conexión** (`F4`).
2. Cambia **Base de datos** de `postgres` a `pruebatecnica`.
3. Pulsa **Aceptar**. Si pide reconectar, acepta.

### 2.4 Las tablas (dos opciones; haz una)

**Opción A (recomendada, sin hacer nada):** el programa las crea solo cuando lo ejecutas.

**Opción B (para mostrarlas antes):**
1. Con la conexión `pruebatecnica` seleccionada, pulsa `Ctrl+]` para abrir un editor SQL.
2. Pega este script, pulsa **Ejecutar script** (`Alt+X`):
   ```sql
   CREATE TABLE IF NOT EXISTS transferencias (
     correlation_id VARCHAR(50) PRIMARY KEY,
     cuenta_origen  VARCHAR(30) NOT NULL,
     cuenta_destino VARCHAR(30) NOT NULL,
     valor          NUMERIC(15,2) NOT NULL,
     fecha_carga    TIMESTAMP NOT NULL DEFAULT now()
   );

   CREATE TABLE IF NOT EXISTS transferencias_rechazadas (
     id BIGSERIAL PRIMARY KEY,
     numero_linea INT NOT NULL,
     correlation_id VARCHAR(50),
     contenido TEXT NOT NULL,
     motivo VARCHAR(20) NOT NULL,
     detalle TEXT NOT NULL,
     fecha_rechazo TIMESTAMP NOT NULL DEFAULT now()
   );
   ```
3. Refresca (`F5`) y despliega: `pruebatecnica → Esquemas → public → Tablas`.

**Opción C (gráfica, tabla por tabla):**
1. Despliega `pruebatecnica → Esquemas → public`.
2. Clic derecho sobre **Tablas → Crear → Tabla**.
3. Nombre: `transferencias`.
4. En la pestaña **Columnas**, pulsa **+** y agrega: `correlation_id` (varchar 50, marca *Not Null*), `cuenta_origen` (varchar 30), `cuenta_destino` (varchar 30), `valor` (numeric 15,2) y `fecha_carga` (timestamp, valor por defecto `now()`).
5. Clic derecho sobre `correlation_id` → **Marcar como clave primaria**.
6. Pulsa **Guardar** (`Ctrl+S`) y **Persistir**.

> **Qué explicar:** la clave primaria (`PRIMARY KEY`) en `correlation_id` es lo que **impide guardar dos veces la misma transferencia**.

---

## PARTE 3. IntelliJ: crear el proyecto Maven

### 3.1 Nuevo proyecto
1. Abre **IntelliJ IDEA**.
2. En la pantalla de bienvenida, arriba a la derecha, pulsa **New Project**.
   Si ya hay un proyecto abierto: menú **File → New → Project**.
3. En el panel izquierdo elige **Java**.
4. Rellena:
   - **Name:** `Transfer`
   - **Location:** la carpeta donde lo quieres guardar
   - **Build system:** selecciona **Maven**
   - **JDK:** elige **17**
   - **Add sample code:** **desmárcalo** (así no crea la clase de ejemplo)
5. Abre **Advanced Settings** y escribe **GroupId:** `org.example`.
6. Pulsa **Create**. Espera a que cargue (barra azul de abajo).

### 3.2 Agregar el driver de PostgreSQL
1. En el panel izquierdo (Project), abre `pom.xml` con doble clic.
2. Antes de la etiqueta de cierre `</project>`, agrega este bloque:
   ```xml
   <dependencies>
       <dependency>
           <groupId>org.postgresql</groupId>
           <artifactId>postgresql</artifactId>
           <version>42.7.4</version>
       </dependency>
   </dependencies>
   ```
3. Arriba a la derecha del editor aparece un icono azul **m** (Load Maven Changes). Púlsalo.
   Atajo: `Ctrl+Shift+O`.
4. Espera a que termine de descargar.

> **Qué explicar:** Maven administra las dependencias. Aquí descarga el driver que permite a Java hablar con PostgreSQL.

### 3.3 Crear el paquete
1. Despliega `src → main → java`.
2. Clic derecho sobre **java → New → Package**.
3. Escribe `com.banco` y pulsa **Enter**.

### 3.4 Crear el archivo plano
1. Clic derecho sobre la **carpeta raíz del proyecto** (`Transfer`) → **New → Directory**. Nombre: `entrada`.
2. Clic derecho sobre `entrada` → **New → File**. Nombre: `transferencias.csv`.
3. Pega el contenido de la **Parte 1** y guarda (`Ctrl+S`).

---

## PARTE 4. Crear las 4 clases

Para cada clase: clic derecho sobre el paquete `com.banco` → **New → Java Class** → escribe el nombre → **Enter**.
Después borra lo que IntelliJ genere y pega el código. Hazlas **en este orden**.

> **Idea para explicar:** cada clase tiene una sola responsabilidad.
> `Main` arranca → `TransferService` valida → `TransferRepository` guarda → PostgreSQL.

### 4.1 Clase `Transfer` (el modelo)
Representa una fila del archivo.

```java
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
```
Qué decir: uso `BigDecimal` para el valor porque con dinero `double` pierde precisión.

### 4.2 Clase `TransferRepository` (acceso a datos)
Es la **única clase que habla con PostgreSQL**.

```java
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
```
Qué decir:
- `crearTablas`: crea las tablas si no existen.
- `guardar`: inserta. Si el `correlation_id` ya existe, `ON CONFLICT DO NOTHING` no inserta nada y devuelve `false`: ese es mi **duplicado**.
- `guardarRechazo`: deja trazabilidad de lo rechazado.
- Uso `PreparedStatement` con `?` para evitar inyección SQL.

### 4.3 Clase `TransferService` (lógica de negocio)
Valida y decide si guarda o rechaza.

```java
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
```
Qué decir, en el orden en que trabaja `procesarArchivo`:
1. Lee todas las líneas y salta la cabecera.
2. Separa cada línea por comas.
3. Si no tiene 4 campos: **rechazada como INVALIDO**.
4. Valida con `validar`. Si hay errores: **rechazada como INVALIDO** con el detalle.
5. Intenta guardar. Si no se pudo (ya existía): **rechazada como DUPLICADO**.
6. Si todo está bien: **guardada**.
7. Al final imprime el resumen (trazabilidad por consola).

### 4.4 Clase `Main` (arranque)
Marca la clase con `public static void main`. IntelliJ muestra un triángulo verde al lado.

```java
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
```
Qué decir:
- `jdbc:postgresql://localhost:5432/pruebatecnica` es la dirección de la base de datos.
- La contraseña **no se escribe en el código**; se lee de una variable de entorno (buena práctica de seguridad).
- El `try/catch` captura cualquier error y lo muestra, para que no se caiga sin explicación.

---

## PARTE 5. Ejecutar desde IntelliJ

### 5.1 Primera ejecución (crea la configuración)
1. Abre la clase `Main`.
2. Pulsa el **triángulo verde** al lado de `public static void main` → **Run 'Main.main()'**.
   Fallará porque aún no hay contraseña; es normal. Así IntelliJ crea la configuración.

### 5.2 Poner la contraseña
1. Arriba a la derecha, junto al triángulo verde, abre el desplegable `Main` → **Edit Configurations...**
   Atajo: `Alt+Shift+F10`, luego `0`.
2. Selecciona `Main`.
3. Si no ves el campo **Environment variables**, pulsa **Modify options** → **Environment variables**.
4. En ese campo escribe: `DB_PASSWORD=tu_clave`
5. Pulsa **Apply** y luego **OK**.

### 5.3 Ejecutar
1. Pulsa el **triángulo verde** (o `Shift+F10`).
2. En la consola de abajo verás las líneas rechazadas y el resumen:
   ```
   Resumen -> guardadas: 3, inválidas: 7, duplicadas: 1
   ```

---

## PARTE 6. Verificar en DBeaver

1. Abre DBeaver y selecciona la conexión `pruebatecnica`. Refresca con `F5`.
2. Abre un editor SQL con `Ctrl+]`.
3. Ejecuta cada consulta con `Ctrl+Enter` (pon el cursor sobre la consulta).

**Transferencias válidas (deben ser 3):**
```sql
SELECT * FROM transferencias;
```

**Rechazadas, con su motivo (deben ser 8: 7 inválidas + 1 duplicada):**
```sql
SELECT numero_linea, correlation_id, motivo, detalle
FROM transferencias_rechazadas
ORDER BY id;
```

**Conteo por motivo:**
```sql
SELECT motivo, COUNT(*) FROM transferencias_rechazadas GROUP BY motivo;
```

**Prueba de duplicados contra la base de datos:**
1. Vuelve a IntelliJ y ejecuta `Main` otra vez (`Shift+F10`).
2. Ahora las 3 válidas salen como **DUPLICADO**, porque ya existen en la base de datos.
3. Repite las consultas en DBeaver para mostrarlo.

---

## PARTE 7. Git

### 7.1 Con menús
1. Menú **Git → Create Git Repository...** (en versiones nuevas está en **VCS** o en el menú principal ☰).
2. Selecciona la carpeta del proyecto y pulsa **OK**.
3. Pulsa `Ctrl+K` (Commit).
4. Marca todos los archivos. Si ves `.idea` o `target`, **desmárcalos**.
5. Escribe el mensaje: `Procesador de transferencias con validacion y PostgreSQL`
6. Pulsa **Commit**.

### 7.2 Con terminal (más rápido)
1. Abre la terminal de IntelliJ: `Alt+F12`.
2. Ejecuta:
```
git init
git add .
git commit -m "Procesador de transferencias con validacion y PostgreSQL"
git log --oneline
```

> **Qué explicar:** Git guarda el historial de versiones. Maven (`pom.xml`) administra las dependencias y compila.

---

## Si algo falla

| Mensaje | Qué significa | Qué hacer |
|---|---|---|
| `la autentificación password falló` | Clave incorrecta | Revisa `DB_PASSWORD` en Edit Configurations |
| `no existe la base de datos "pruebatecnica"` | No se creó la base | Repite la Parte 2.2 |
| `No suitable driver` o `ClassNotFound` | Maven no descargó el driver | Pulsa Load Maven Changes (`Ctrl+Shift+O`) |
| `Connection refused` | PostgreSQL apagado | Abre `services.msc` y arranca `postgresql-x64-17` |
| `NoSuchFileException: entrada/...` | El archivo no está donde se espera | Revisa que `entrada` esté en la raíz del proyecto |
| Todas las líneas salen "INVALIDO" | El CSV tiene otro formato | Revisa que use comas y 4 columnas |

---

## Preguntas típicas del entrevistador

- **¿Cómo evitas duplicados?** Con `PRIMARY KEY` en `correlation_id` y `ON CONFLICT DO NOTHING`. Si el insert no afecta ninguna fila, es duplicado.
- **¿Por qué `BigDecimal`?** Porque `double` pierde precisión con dinero.
- **¿Por qué separas en clases?** Para que cada una tenga una sola responsabilidad y sea fácil de mantener y probar.
- **¿Dónde entra GoAnywhere?** Recibe el archivo por SFTP y lo deja en la carpeta de entrada; mi programa lo lee desde ahí.
- **¿Cómo das trazabilidad?** Mensajes en consola con el número de línea y el motivo, y la tabla `transferencias_rechazadas`.
- **¿Qué mejorarías con más tiempo?** Pruebas con JUnit, un logger en lugar de `System.out`, mover el archivo a una carpeta `procesados`, y una tabla con el resumen de cada ejecución.
