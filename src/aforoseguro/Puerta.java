package aforoseguro;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class Puerta {
    public static void main(String[] args) throws IOException {
        if (args.length < 1 || args.length > 2 || !args[0].matches("[123]")) {
            System.err.println("Uso: Puerta <1|2|3> [config.properties]");
            return;
        }

        int id = Integer.parseInt(args[0]);
        String nombre = switch (id) {
            case 1 -> "Norte";
            case 2 -> "Sur";
            default -> "Este";
        };
        Path archivo = Path.of(args.length == 2 ? args[1] : "config.properties");
        Configuracion config = Configuracion.cargar(archivo);
        var bitacora = new Bitacora(id);
        bitacora.local("INICIO", nombre);
        System.out.printf("P%s %s. Órdenes: entrar, salir, estado, cerrar.%n", id, nombre);
        try (var consola = new Scanner(System.in);
             var latidos = Executors.newSingleThreadScheduledExecutor()) {
            latidos.scheduleAtFixedRate(() -> enviarLatido(config, bitacora, id), 0, 2, TimeUnit.SECONDS);
            while (consola.hasNextLine()) {
                String orden = consola.nextLine().strip().toUpperCase(Locale.ROOT);
                switch (orden) {
                    case "CERRAR" -> { return; }
                    case "ENTRAR", "SALIR", "ESTADO" -> solicitar(config, bitacora, id, orden);
                    default -> System.out.println("Orden desconocida.");
                }
            }
        }
    }

    private static void enviarLatido(Configuracion config, Bitacora bitacora, int id) {
        try {
            Conexion.solicitar(config, bitacora, id, id, "LATIDO");
        } catch (IOException error) {
            bitacora.local("SIN_RESPUESTA", "LATIDO");
        }
    }

    private static void solicitar(Configuracion config, Bitacora bitacora, int id, String orden) {
        try {
            String[] respuesta = Conexion.solicitar(config, bitacora, id, id, orden)
                    .split("\\|", -1);
            if (respuesta[0].equals("PERMITIDA") && respuesta.length >= 3) {
                System.out.printf("PERMITIDA. Dentro: %s/%s%n", respuesta[1], respuesta[2]);
                if (respuesta.length == 4) {
                    System.out.println(respuesta[3]);
                }
            } else if (respuesta[0].equals("RECHAZADA") && respuesta.length == 2) {
                System.out.println("RECHAZADA. " + respuesta[1]);
            } else {
                System.out.println("Respuesta del coordinador inválida.");
            }
        } catch (IOException error) {
            bitacora.local("SIN_RESPUESTA", orden);
            System.out.println("Coordinador sin respuesta. Operación sin confirmar.");
        }
    }
}
