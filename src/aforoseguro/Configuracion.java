package aforoseguro;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

record Configuracion(String host, int puerto, int aforo, String token) {
    static Configuracion cargar(Path archivo) throws IOException {
        Properties valores = new Properties();
        try (var lector = Files.newBufferedReader(archivo)) {
            valores.load(lector);
        }
        String host = valores.getProperty("host", "").strip();
        String token = valores.getProperty("token", "");
        int puerto = Integer.parseInt(valores.getProperty("puerto", "0"));
        int aforo = Integer.parseInt(valores.getProperty("aforo", "0"));
        if (host.isEmpty() || token.isBlank() || token.contains("|")
                || puerto < 1 || puerto > 65535 || aforo < 1) {
            throw new IllegalArgumentException("Configuración inválida: " + archivo);
        }
        return new Configuracion(host, puerto, aforo, token);
    }
}
