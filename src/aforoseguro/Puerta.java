package aforoseguro;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Scanner;

public final class Puerta {
    public static void main(String[] args) throws IOException {
        if (args.length < 1 || args.length > 2 || !args[0].matches("[123]")) {
            System.err.println("Uso: Puerta <1|2|3> [config.properties]");
            return;
        }

        String id = args[0];
        String nombre = switch (id) {
            case "1" -> "Norte";
            case "2" -> "Sur";
            default -> "Este";
        };
        Path archivo = Path.of(args.length == 2 ? args[1] : "config.properties");
        Configuracion config = Configuracion.cargar(archivo);
        System.out.printf("P%s %s. Órdenes: entrar, salir, estado, cerrar.%n", id, nombre);
        try (var consola = new Scanner(System.in)) {
            while (consola.hasNextLine()) {
                String orden = consola.nextLine().strip().toUpperCase(Locale.ROOT);
                switch (orden) {
                    case "CERRAR" -> { return; }
                    case "ENTRAR", "SALIR", "ESTADO" -> solicitar(config, id, orden);
                    default -> System.out.println("Orden desconocida.");
                }
            }
        }
    }

    private static void solicitar(Configuracion config, String id, String orden) {
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress(config.host(), config.puerto()), 2000);
            socket.setSoTimeout(2000);
            var salida = new DataOutputStream(socket.getOutputStream());
            var entrada = new DataInputStream(socket.getInputStream());
            salida.writeUTF(orden + "|" + id + "|" + config.token());
            salida.flush();
            String[] respuesta = entrada.readUTF().split("\\|", -1);
            if (respuesta[0].equals("PERMITIDA") && respuesta.length == 3) {
                System.out.printf("PERMITIDA. Dentro: %s/%s%n", respuesta[1], respuesta[2]);
            } else if (respuesta[0].equals("RECHAZADA") && respuesta.length == 2) {
                System.out.println("RECHAZADA. " + respuesta[1]);
            } else {
                System.out.println("Respuesta del coordinador inválida.");
            }
        } catch (IOException error) {
            System.out.println("Coordinador sin respuesta. Operación sin confirmar.");
        }
    }
}
