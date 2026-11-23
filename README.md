# AforoSeguro

Práctica previa de aplicaciones distribuidas con Java 21 y sockets TCP. Se compila con `javac` y usa la biblioteca estándar.

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
| Solicitud | `OPERACIÓN\|ID_PUERTA\|TOKEN\|ID_EMISOR\|LAMPORT` |
| Respuesta permitida | `PERMITIDA\|DENTRO\|AFORO\|LAMPORT` |
| Respuesta de estado | `PERMITIDA\|DENTRO\|AFORO\|ESTADO_PUERTAS\|LAMPORT` |
| Respuesta rechazada | `RECHAZADA\|MOTIVO\|LAMPORT` |
| Respuesta a latido | `RECIBIDA\|LAMPORT` |

Las operaciones son `ENTRAR`, `SALIR`, `ESTADO` y `LATIDO`. El coordinador registra los rechazos de autenticación sin guardar el token. El emisor de `PruebaCarga` usa el identificador 4 y su bitácora queda en `logs/carga.log`.

## Pruebas manuales de la parte A

1. Con el coordinador recién iniciado, ejecuta `salir` y comprueba que se rechaza porque el recinto está vacío.
2. Ejecuta `entrar` una vez en cada puerta y `salir` en P2. Consulta `estado` en las tres puertas. Deben mostrar `2/10`.
3. Reinicia el coordinador para partir de cero. Envía once órdenes `entrar`. La última debe rechazarse por aforo completo.
4. Crea una copia de `config.properties` con otro token, sin cambiar el archivo del coordinador. Arranca una puerta con la copia y ejecuta `entrar`. Comprueba el rechazo y la línea `TOKEN_INVALIDO` en la bitácora.

## Carga, fallos y orden total

1. Arranca el coordinador normal con el recinto vacío. En otra consola ejecuta `java -cp target/aforoseguro aforoseguro.PruebaCarga`. Con aforo 10 deben verse 10 permitidas, 20 rechazadas y 10 personas dentro.
2. Detén el coordinador y arráncalo con `java -cp target/aforoseguro aforoseguro.Coordinador --modo-inseguro`. Repite la prueba y anota el resultado. Este modo hace una pausa de 50 ms entre la comprobación del límite y el incremento.
3. Vuelve al modo normal y arranca las tres puertas. Detén P3 y observa su estado cuando pasen 6 segundos desde su último latido. Arranca P3 de nuevo y confirma que vuelve a `ACTIVA`.
4. Detén el coordinador y ejecuta `entrar` desde P1. La puerta avisa que la operación quedó sin confirmar.
5. Detén las puertas y ejecuta `java -cp target/aforoseguro aforoseguro.OrdenTotal`. Genera `logs/orden_total.log` desde las cuatro bitácoras principales. El programa acepta una ruta alternativa a la carpeta de registros.

Las bitácoras se agregan de forma acumulativa. Cada proceso retoma la mayor marca de su archivo si se reinicia. Para una nueva demostración, conserva una copia de los registros anteriores antes de apartar la carpeta `logs`. El contador de personas se almacena en memoria y vuelve a cero al reiniciar el coordinador. Ejecuta una sola instancia por identificador desde la misma carpeta de trabajo.

Los resultados de una ejecución local están en `docs/evidencia/`. Para documentar una repetición con capturas, muestra `whoami`, `hostname` y `Get-Date` junto a la consola y guarda las imágenes en `docs/img/`. El estado de la red se consulta con `Get-NetAdapter | Format-Table Name,Status`.

## Compilar los PDF

Usa pdfLaTeX con los paquetes `fontenc`, `geometry`, `graphicx` y `hyperref`. Desde la raíz:

```powershell
Set-Location docs
pdflatex -interaction=nonstopmode -halt-on-error informe.tex
pdflatex -interaction=nonstopmode -halt-on-error informe.tex
pdflatex -interaction=nonstopmode -halt-on-error entrega_SGA.tex
```

`entrega_SGA.pdf` es la hoja de una página para la práctica. Antes de cualquier entrega, verifica los datos personales y comprueba que la URL apunte a un repositorio con contenido publicado.

## Código reutilizado

No se reutilizó código.
