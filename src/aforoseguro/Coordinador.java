package aforoseguro;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class Coordinador {
    private final Configuracion config;
    private int dentro;

    private Coordinador(Configuracion config) {
        this.config = config;
    }

    public static void main(String[] args) throws IOException {
        Path archivo = Path.of(args.length == 0 ? "config.properties" : args[0]);
        new Coordinador(Configuracion.cargar(archivo)).ejecutar();
    }

    private void ejecutar() throws IOException {
        Files.createDirectories(Path.of("logs"));
        try (var servidor = new ServerSocket()) {
            servidor.bind(new InetSocketAddress(config.host(), config.puerto()));
            System.out.printf("Coordinador 0 en %s:%d, aforo %d%n",
                    config.host(), config.puerto(), config.aforo());
            while (true) {
                atender(servidor.accept());
            }
        }
    }

    private void atender(Socket conexion) {
        try (conexion;
             var entrada = new DataInputStream(conexion.getInputStream());
             var salida = new DataOutputStream(conexion.getOutputStream())) {
            while (true) {
                salida.writeUTF(procesar(entrada.readUTF()));
                salida.flush();
            }
        } catch (EOFException fin) {
            // El cliente cerró la conexión.
        } catch (IOException error) {
            System.err.println("Error al atender conexión: " + error.getMessage());
        }
    }

    private String procesar(String mensaje) throws IOException {
        String[] campos = mensaje.split("\\|", -1);
        String token = campos.length > 2 ? campos[2] : "";
        if (!config.token().equals(token)) {
            String evento = "TOKEN_INVALIDO" + System.lineSeparator();
            Files.writeString(Path.of("logs", "coordinador.log"), evento,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            System.out.print(evento);
            return "RECHAZADA|token inválido";
        }

        if (campos.length != 3 || !campos[1].matches("[123]")) {
            return "RECHAZADA|solicitud inválida";
        }

        switch (campos[0]) {
            case "ENTRAR" -> {
                if (dentro >= config.aforo()) {
                    return "RECHAZADA|aforo completo";
                }
                dentro++;
            }
            case "SALIR" -> {
                if (dentro == 0) {
                    return "RECHAZADA|nadie dentro";
                }
                dentro--;
            }
            case "ESTADO" -> { }
            default -> {
                return "RECHAZADA|operación desconocida";
            }
        }
        return "PERMITIDA|" + dentro + "|" + config.aforo();
    }
}
