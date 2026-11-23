package aforoseguro;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

final class Conexion {
    static String solicitar(Configuracion config, String mensaje) throws IOException {
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress(config.host(), config.puerto()), 2000);
            socket.setSoTimeout(2000);
            var salida = new DataOutputStream(socket.getOutputStream());
            var entrada = new DataInputStream(socket.getInputStream());
            salida.writeUTF(mensaje);
            salida.flush();
            return entrada.readUTF();
        }
    }
}
