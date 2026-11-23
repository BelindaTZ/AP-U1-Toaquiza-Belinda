package aforoseguro;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

final class Conexion {
    static String solicitar(Configuracion config, Bitacora bitacora,
                            int origen, int puerta, String operacion) throws IOException {
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress(config.host(), config.puerto()), 2000);
            socket.setSoTimeout(2000);
            var salida = new DataOutputStream(socket.getOutputStream());
            var entrada = new DataInputStream(socket.getInputStream());
            long marca = bitacora.local("ENVIAR", operacion + " puerta=" + puerta + " destino=0");
            salida.writeUTF(operacion + "|" + puerta + "|" + config.token() + "|" + origen + "|" + marca);
            salida.flush();
            String trama = entrada.readUTF();
            int separador = trama.lastIndexOf('|');
            if (separador < 0) {
                throw new IOException("Respuesta sin reloj");
            }
            String respuesta = trama.substring(0, separador);
            try {
                long remoto = Long.parseLong(trama.substring(separador + 1));
                bitacora.recibir(remoto, "origen=0 reloj=" + remoto + " solicitud=" + marca
                        + " " + respuesta.replace('|', ' '));
            } catch (NumberFormatException error) {
                throw new IOException("Reloj de respuesta inválido", error);
            }
            return respuesta;
        }
    }
}
