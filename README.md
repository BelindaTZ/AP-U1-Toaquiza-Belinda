# AforoSeguro

Práctica previa de aplicaciones distribuidas. La parte A usa Java 21 y sockets TCP. Conserva el proyecto original en la subcarpeta `RegistroDistribuido`.

## Compilar y arrancar

Desde la raíz del repositorio en PowerShell:

```powershell
javac -encoding UTF-8 -d target/aforoseguro (Get-ChildItem src/aforoseguro/*.java).FullName
java -cp target/aforoseguro aforoseguro.Coordinador
```

Con el coordinador activo, ejecuta cada puerta en una consola distinta desde la misma carpeta:

```powershell
java -cp target/aforoseguro aforoseguro.Puerta 1
java -cp target/aforoseguro aforoseguro.Puerta 2
java -cp target/aforoseguro aforoseguro.Puerta 3
```

Cada puerta acepta `entrar`, `salir`, `estado` y `cerrar`. El coordinador y las puertas leen `config.properties` por defecto y aceptan una ruta alternativa como último argumento.

## Mensajes TCP

Cada trama usa el formato de Java `DataOutputStream.writeUTF` y `DataInputStream.readUTF`. Un prefijo de dos bytes indica el tamaño del contenido en UTF-8 modificado. Se pueden enviar varias tramas consecutivas en la misma conexión.

| Mensaje | Campos |
| --- | --- |
| Solicitud | `OPERACIÓN\|ID_PUERTA\|TOKEN` |
| Respuesta permitida | `PERMITIDA\|DENTRO\|AFORO` |
| Respuesta rechazada | `RECHAZADA\|MOTIVO` |

Las operaciones del protocolo son `ENTRAR`, `SALIR` y `ESTADO`. El coordinador escribe los rechazos de autenticación en `logs/coordinador.log` sin guardar el token.

## Pruebas manuales de la parte A

1. Con el coordinador recién iniciado, ejecuta `salir` y comprueba que se rechaza porque el recinto está vacío.
2. Ejecuta `entrar` una vez en cada puerta y `salir` en P2. Consulta `estado` en las tres puertas. Deben mostrar `2/10`.
3. Reinicia el coordinador para partir de cero. Envía once órdenes `entrar`. La última debe rechazarse por aforo completo.
4. Crea una copia de `config.properties` con otro token, sin cambiar el archivo del coordinador. Arranca una puerta con la copia y ejecuta `entrar`. Comprueba el rechazo y la línea `TOKEN_INVALIDO` en la bitácora.
