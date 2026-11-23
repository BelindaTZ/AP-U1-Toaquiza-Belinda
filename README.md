# AforoSeguro

AforoSeguro lleva la cuenta de cuántas personas hay dentro del Coliseo de Quevedo. Las puertas Norte, Sur y Este consultan al mismo coordinador para saber si queda espacio. Desde cualquiera de las tres se puede registrar una entrada o salida y consultar el aforo.

## Cómo arrancarlo

Necesitas Java 21. Abre PowerShell en la carpeta del proyecto y ejecuta estos dos comandos para compilar y arrancar el coordinador:

```powershell
javac -encoding UTF-8 -d target/aforoseguro (Get-ChildItem src/aforoseguro/*.java).FullName
java -cp target/aforoseguro aforoseguro.Coordinador
```

Deja el coordinador abierto. Arranca cada puerta en su propia consola, siempre desde la carpeta del proyecto:

```powershell
java -cp target/aforoseguro aforoseguro.Puerta 1
java -cp target/aforoseguro aforoseguro.Puerta 2
java -cp target/aforoseguro aforoseguro.Puerta 3
```

Escribe `entrar`, `salir` o `estado`. Con `cerrar` terminas esa puerta. El aforo, la dirección, el puerto y el token se leen de `config.properties`. Puedes pasar otro archivo de configuración como último argumento.

## Qué probé

1. **Operación normal.** Registra una entrada en cada puerta y una salida por P2. Las tres consultas de estado deben mostrar `2/10`.
2. **Aforo y token.** Arranca el coordinador desde cero y envía once entradas. La última debe rechazarse. Para probar el token, arranca una puerta con una copia de la configuración que tenga un token distinto. El rechazo queda en `logs/coordinador.log`.
3. **Carga.** Arranca el coordinador con el recinto vacío y ejecuta `java -cp target/aforoseguro aforoseguro.PruebaCarga` en otra consola. El modo normal dejó entrar a 10 personas y rechazó 20 solicitudes. Para comparar, reinicia el coordinador con `java -cp target/aforoseguro aforoseguro.Coordinador --modo-inseguro` y vuelve a lanzar la prueba. En la ejecución del informe, este modo aceptó las 30.
4. **Caída de una puerta.** Vuelve al modo normal, abre las tres puertas y detén P3. Tras unos seis segundos sin latidos debe aparecer como caída. Abre P3 de nuevo y comprueba que vuelve a aparecer activa.
5. **Caída del coordinador.** Detén el coordinador e intenta entrar desde P1. La puerta informa que la entrada quedó sin confirmar.
6. **Orden de los eventos.** Cierra los procesos y ejecuta `java -cp target/aforoseguro aforoseguro.OrdenTotal`. El resultado queda en `logs/orden_total.log`.

Al reiniciar el coordinador, la ocupación vuelve a cero. Los registros se conservan y el reloj de cada proceso continúa desde la última marca de su archivo. Guarda una copia de `logs/` si quieres separar las salidas de otra prueba. Las capturas y las transcripciones están en `docs/img/` y `docs/evidencia/`.

## Cómo se comunican las puertas

Por TCP, cada solicitud lleva la operación, el número de puerta, el token y los datos del reloj de Lamport. Cada trama tiene un prefijo de dos bytes con el tamaño del texto, en UTF-8 modificado de Java. Así, el coordinador puede leer varias solicitudes seguidas sin mezclarlas.

```text
OPERACIÓN|PUERTA|TOKEN|EMISOR|LAMPORT
PERMITIDA|DENTRO|AFORO|LAMPORT
PERMITIDA|DENTRO|AFORO|ESTADO_PUERTAS|LAMPORT
RECHAZADA|MOTIVO|LAMPORT
RECIBIDA|LAMPORT
```

`LATIDO` recibe `RECIBIDA`. `PruebaCarga` usa el identificador 4 y conserva sus eventos en `logs/carga.log`. El orden total reúne solo los registros del coordinador y las tres puertas.

## Regenerar los PDF

Usa pdfLaTeX con los paquetes `fontenc`, `geometry`, `graphicx` y `hyperref`. Desde la carpeta del proyecto, entra a `docs` y ejecuta:

```powershell
Set-Location docs
pdflatex -interaction=nonstopmode -halt-on-error informe.tex
pdflatex -interaction=nonstopmode -halt-on-error informe.tex
pdflatex -interaction=nonstopmode -halt-on-error entrega_SGA.tex
```

## Código reutilizado

No se reutilizó código.

## Registro posterior de los códigos de sesión

El 23 de noviembre de 2026 se anotaron los códigos facilitados después de publicar el historial original.

- Sesión 1 — `KD55-TD`
- Sesión 2 — `662D-M6`

Esta nota documenta los códigos de forma retrospectiva. No acredita que se incluyeran en los commits de inicio o del minuto 60.
