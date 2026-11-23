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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class Coordinador {
    private final Configuracion config;
    private final AtomicInteger dentro = new AtomicInteger();
    private final boolean inseguro;
    private final long[] latidos = new long[3];
    private final boolean[] activas = new boolean[3];

    private Coordinador(Configuracion config, boolean inseguro) {
        this.config = config;
        this.inseguro = inseguro;
    }

    public static void main(String[] args) throws IOException {
        boolean inseguro = args.length > 0 && args[0].equals("--modo-inseguro");
        int indice = inseguro ? 1 : 0;
        Path archivo = Path.of(args.length > indice ? args[indice] : "config.properties");
        new Coordinador(Configuracion.cargar(archivo), inseguro).ejecutar();
    }

    private void ejecutar() throws IOException {
        Files.createDirectories(Path.of("logs"));
        try (var servidor = new ServerSocket();
             var monitor = Executors.newSingleThreadScheduledExecutor()) {
            servidor.bind(new InetSocketAddress(config.host(), config.puerto()));
            monitor.scheduleAtFixedRate(this::detectarCaidas, 100, 100, TimeUnit.MILLISECONDS);
            System.out.printf("Coordinador 0 en %s:%d, aforo %d, modo %s%n",
                    config.host(), config.puerto(), config.aforo(), inseguro ? "inseguro" : "normal");
            while (true) {
                Socket cliente = servidor.accept();
                Thread.startVirtualThread(() -> atender(cliente));
            }
        }
    }

    private void atender(Socket conexion) {
        try (conexion;
             var entrada = new DataInputStream(conexion.getInputStream());
             var salida = new DataOutputStream(conexion.getOutputStream())) {
            conexion.setSoTimeout(5000);
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

        if (campos[0].equals("LATIDO")) {
            registrarLatido(Integer.parseInt(campos[1]));
            return "RECIBIDA";
        }
        String respuesta = inseguro ? operar(campos[0]) : operarSeguro(campos[0]);
        return campos[0].equals("ESTADO") ? respuesta + "|" + estadoPuertas() : respuesta;
    }

    private synchronized void registrarLatido(int puerta) {
        int indice = puerta - 1;
        latidos[indice] = System.nanoTime();
        if (!activas[indice]) {
            activas[indice] = true;
            registrarTransicion(puerta, "ACTIVA");
        }
    }

    private synchronized void detectarCaidas() {
        long ahora = System.nanoTime();
        for (int i = 0; i < activas.length; i++) {
            if (activas[i] && ahora - latidos[i] >= TimeUnit.SECONDS.toNanos(6)) {
                activas[i] = false;
                registrarTransicion(i + 1, "CAIDA");
            }
        }
    }

    private void registrarTransicion(int puerta, String estado) {
        String evento = "P" + puerta + " " + estado;
        System.out.println(evento);
        try {
            Files.writeString(Path.of("logs", "coordinador.log"), evento + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException error) {
            System.err.println("Error de bitácora: " + error.getMessage());
        }
    }

    private synchronized String estadoPuertas() {
        return "P1=" + estado(0) + ", P2=" + estado(1) + ", P3=" + estado(2);
    }

    private String estado(int indice) {
        return activas[indice] ? "ACTIVA" : "CAIDA";
    }

    private synchronized String operarSeguro(String orden) {
        return operar(orden);
    }

    private String operar(String orden) {
        int total = dentro.get();
        switch (orden) {
            case "ENTRAR" -> {
                if (total >= config.aforo()) {
                    return "RECHAZADA|aforo completo";
                }
                if (inseguro) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException error) {
                        Thread.currentThread().interrupt();
                        return "RECHAZADA|operación interrumpida";
                    }
                }
                total = dentro.incrementAndGet();
            }
            case "SALIR" -> {
                int anterior = dentro.getAndUpdate(actual -> Math.max(0, actual - 1));
                if (anterior == 0) {
                    return "RECHAZADA|nadie dentro";
                }
                total = anterior - 1;
            }
            case "ESTADO" -> { }
            default -> {
                return "RECHAZADA|operación desconocida";
            }
        }
        return "PERMITIDA|" + total + "|" + config.aforo();
    }
}
