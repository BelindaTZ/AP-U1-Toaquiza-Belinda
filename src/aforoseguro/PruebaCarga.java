package aforoseguro;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class PruebaCarga {
    public static void main(String[] args) throws Exception {
        var config = Configuracion.cargar(Path.of(args.length == 0 ? "config.properties" : args[0]));
        var preparados = new CountDownLatch(30);
        var inicio = new CountDownLatch(1);
        List<Future<String>> respuestas = new ArrayList<>();
        int permitidas = 0;
        int rechazadas = 0;
        int errores = 0;
        try (var ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 30; i++) {
                int puerta = i / 10 + 1;
                respuestas.add(ejecutor.submit(() -> {
                    preparados.countDown();
                    inicio.await();
                    return Conexion.solicitar(config, "ENTRAR|" + puerta + "|" + config.token());
                }));
            }
            preparados.await();
            inicio.countDown();
            for (var respuesta : respuestas) {
                try {
                    if (respuesta.get().startsWith("PERMITIDA|")) {
                        permitidas++;
                    } else {
                        rechazadas++;
                    }
                } catch (java.util.concurrent.ExecutionException error) {
                    errores++;
                    System.err.println(error.getCause().getMessage());
                }
            }
        }
        System.out.printf("Permitidas: %d | Rechazadas: %d | Errores: %d%n",
                permitidas, rechazadas, errores);
        System.out.println("Estado del coordinador: "
                + Conexion.solicitar(config, "ESTADO|1|" + config.token()));
    }
}
