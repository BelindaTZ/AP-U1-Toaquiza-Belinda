package aforoseguro;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

final class Bitacora {
    private final Path archivo;
    private final int id;
    private long reloj;

    Bitacora(int id) throws IOException {
        this.id = id;
        Files.createDirectories(Path.of("logs"));
        archivo = Path.of("logs", switch (id) {
            case 0 -> "coordinador.log";
            case 4 -> "carga.log";
            default -> "p" + id + ".log";
        });
        if (Files.exists(archivo)) {
            try (var lineas = Files.lines(archivo)) {
                reloj = lineas.mapToLong(linea -> Long.parseLong(linea.split(";", 2)[0]))
                        .max().orElse(0);
            }
        }
    }

    synchronized long local(String evento, String detalle) {
        return escribir(++reloj, evento, detalle);
    }

    synchronized void recibir(long remoto, String detalle) {
        reloj = Math.max(reloj, remoto) + 1;
        escribir(reloj, "RECIBIR", detalle);
    }

    private long escribir(long marca, String evento, String detalle) {
        String linea = marca + ";" + id + ";" + evento + ";" + detalle + System.lineSeparator();
        try {
            Files.writeString(archivo, linea, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException error) {
            throw new UncheckedIOException("No se pudo escribir " + archivo, error);
        }
        return marca;
    }
}
