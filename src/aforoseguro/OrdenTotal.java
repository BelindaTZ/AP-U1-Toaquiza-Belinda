package aforoseguro;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class OrdenTotal {
    private record Evento(long marca, int id, String linea) { }

    public static void main(String[] args) throws IOException {
        Path carpeta = Path.of(args.length == 0 ? "logs" : args[0]);
        List<Evento> eventos = new ArrayList<>();
        for (String nombre : List.of("coordinador.log", "p1.log", "p2.log", "p3.log")) {
            for (String linea : Files.readAllLines(carpeta.resolve(nombre))) {
                String[] campos = linea.split(";", 4);
                if (campos.length != 4) {
                    throw new IOException("Línea inválida en " + nombre + ": " + linea);
                }
                eventos.add(new Evento(Long.parseLong(campos[0]), Integer.parseInt(campos[1]), linea));
            }
        }
        eventos.sort(Comparator.comparingLong(Evento::marca).thenComparingInt(Evento::id));
        Path destino = carpeta.resolve("orden_total.log");
        Files.write(destino, eventos.stream().map(Evento::linea).toList());
        System.out.printf("%d eventos escritos en %s%n", eventos.size(), destino);
    }
}
