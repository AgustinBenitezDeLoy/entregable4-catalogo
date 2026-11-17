# Entregable 4: catálogo de videojuegos con CI/CD

Universidad de Montevideo, Programación Avanzada 2026.
Agustín Benítez y Federico Otero. Equipo de **dos** personas.
Entrega oficial: 17/11/2026 a las 12:00 por Moodle.

Aplicación web para registrar videojuegos, clasificarlos por género y mostrarlos con una imagen local. Integra dos módulos Maven: `catalogo-core` (reglas y archivo JSON) y `catalogo-web` (Spring Boot, API y frontend). El foco es el pipeline declarativo Jenkins. El anexo de tres integrantes no aplica: no hay UAT ni aprobación manual.

## Requisitos e inicio rápido

- Windows 10/11 o Windows Server con PowerShell 5.1 para el despliegue.
- JDK 21 o 25 (compilación con `release=21`). Validado localmente con Temurin 25.
- Maven 3.9.11 o compatible, disponible como `mvn` en PATH.
- Internet para la primera resolución de dependencias Maven. No se requiere Node ni Python.
- Para CI/CD: Git, repositorio GitHub y Jenkins con agente Windows.

Abrir PowerShell en la carpeta que contiene este README y `pom.xml`:

```powershell
java -version
mvn -version
mvn -B -ntp clean verify
java -jar .\catalogo-web\target\catalogo.jar
```

Abrir <http://localhost:8080>. Para detener el arranque directo, presionar Ctrl+C.
Maven descarga las dependencias declaradas en los POM, compila ambos módulos, ejecuta JUnit y genera `catalogo-web/target/catalogo.jar`. El JAR incluye las dependencias y el módulo core. Los tests de integración usan un puerto aleatorio y datos aislados en `target/`.

## Funcionalidades y API

1. Escribir un nombre, elegir género e imagen y pulsar Registrar.
2. Filtrar por género. Todos los géneros restablece el listado completo.
3. Los datos quedan en `data/catalogo.json` por defecto. Reiniciar mantiene el catálogo.

Nombre: entre 1 y 80 caracteres tras quitar espacios de los extremos. No se admiten nombres duplicados sin distinguir mayúsculas. Géneros: ACCION, AVENTURA, ESTRATEGIA, DEPORTES, ROL. Imágenes: tres SVG originales incluidos, sin conexión externa. Se usa una imagen predeterminada si no se especifica.

| Método y ruta | Resultado |
|---|---|
| GET `/api/videojuegos` | Lista completa, inicialmente `[]` |
| GET `/api/videojuegos?genero=AVENTURA` | Solo aventuras |
| POST `/api/videojuegos` | Registra, HTTP 201 |
| GET `/api/generos` | Géneros permitidos |
| GET `/api/estado` | Estado, mensaje y revisión desplegada |

Errores esperados: 400 para entrada inválida, 409 para nombre duplicado, 503 para error de escritura. Un archivo JSON ilegible impide iniciar y se conserva sin sobrescribirlo. El repositorio soporta solicitudes concurrentes dentro de una única JVM, no varias instancias escribiendo el mismo archivo.

Ejemplo reproducible, con la aplicación recién iniciada:

```powershell
$base = 'http://localhost:8080'
$json = Get-Content -LiteralPath .\ejemplos\videojuego.json -Raw -Encoding UTF8
Invoke-WebRequest -UseBasicParsing -Uri "$base/api/videojuegos" -Method Post -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes($json))
Invoke-RestMethod "$base/api/videojuegos?genero=AVENTURA"
```

El primer comando devuelve 201 y un objeto con id, nombre, género e imagen. Repetir el registro devuelve 409. El filtro devuelve el juego registrado. La consigna propone aplicaciones, pero no fija entradas ni salidas oficiales adicionales.

## Producción simulada local

Desde la raíz del proyecto, después de `mvn clean verify`:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\deploy.ps1 -Jar .\catalogo-web\target\catalogo.jar -Destino .\produccion -Revision demo-1 -Puerto 8081
Invoke-RestMethod http://localhost:8081/api/estado
```

Abrir <http://localhost:8081>. Se espera `estado=OK` y `revision=demo-1`.
`ExecutionPolicy Bypass` se aplica solo al proceso que ejecuta el script; no modifica la política permanente de Windows.

El script copia el JAR probado a una carpeta de versión, compara SHA-256, detiene exclusivamente el PID que identifica como suyo, inicia Java oculto y espera un smoke test HTTP de estado, portada y listado. Solo entonces actualiza `actual.json`. Si el nuevo arranque falla, restaura el JAR anterior y conserva el estado estable. El proceso no requiere una ventana de terminal abierta.

Los datos quedan en `produccion/datos/catalogo.json`, separados de `produccion/versiones/`. No hay migraciones destructivas ni cambios de esquema entre versiones. Este despliegue implica una breve interrupción durante el reinicio. El rollback recupera el binario anterior y no restaura ni borra los datos.

Para detener sin borrar datos:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\detener.ps1 -Destino .\produccion
```

El usuario del agente necesita escribir en la carpeta de producción y consultar/detener sus propios procesos. Si el puerto está ocupado por otra aplicación, elegir otro puerto; el script no mata procesos ajenos. No compartir el directorio entre aplicaciones o despliegues simultáneos. Los logs `stdout.log` y `stderr.log` se guardan por versión.

## Jenkins y GitHub: configuración del entorno

Repositorio: <https://github.com/AgustinBenitezDeLoy/entregable4-catalogo>, rama `main`. Está conectado a Jenkins 2.580.2 local con el job `catalogo-ci-cd` (Pipeline script from SCM, rama `*/main`, Script Path `Jenkinsfile`). El 17/11/2026 el build #1 obtuvo de GitHub el commit `604f3c6`, pasó las 26 pruebas, archivó el JAR y lo desplegó en el puerto 8081; `/api/estado` publicó esa misma revisión. Antes de crear el repositorio, el pipeline se había validado con un arnés que sustituía SCM por una copia de archivos (un job exitoso y otro con la regresión de dos fallos, puerto 8091). Los pasos siguientes documentan la configuración usada, para reproducirla en otro equipo.

1. Instalar Jenkins LTS compatible con el JDK elegido. Para Java 25 se requiere Jenkins 2.541.1 o posterior. Instalar plugins Pipeline (incluye Declarative), Git y JUnit y sus dependencias.
2. Configurar un agente Windows con etiqueta `windows`. Para una demostración académica local puede ser el nodo integrado, con un solo ejecutor. El servicio debe usar una cuenta con permisos sobre sus procesos y la carpeta de producción.
3. En Manage Jenkins / Tools, registrar Maven **Maven3** y JDK **JDK**, apuntando a las instalaciones locales. No confundir el JDK del controlador con el disponible en el agente.
4. El repositorio contiene la raíz del proyecto completa, incluyendo `src/test`, los POM, scripts y Jenkinsfile. Cada integrante usa su cuenta Git y cambios pequeños revisados por el compañero. No subir `target`, datos ni producción. Ejecutar `mvn clean verify` antes de integrar cambios.
5. Crear un job Pipeline / Pipeline script from SCM / Git. Indicar la URL `https://github.com/AgustinBenitezDeLoy/entregable4-catalogo.git` (repositorio público, sin credenciales), la rama `*/main` y Script Path `Jenkinsfile`. La URL no está embebida en el código.
6. Ejecutar **Build Now** una vez para registrar el trigger. Configurar el parámetro `PRODUCCION_DIR` como carpeta absoluta escribible y fuera del workspace, por ejemplo `C:\Jenkins\produccion\catalogo`. Usar otra carpeta por proyecto.
7. `pollSCM('* * * * *')` consulta GitHub cada minuto. Al detectar un cambio programa el pipeline. Puede haber más de un commit entre consultas: Jenkins integra la última revisión detectada. No requiere exponer Jenkins a Internet ni configurar webhook.
8. Abrir Stage View, Console Output, Test Result y Artifacts. Verificar que `/api/estado` publica el hash Git del build. Conservar las consolas de ambos casos de defensa.

### Etapas del Jenkinsfile

1. Checkout de la revisión SCM y lectura de `git rev-parse HEAD`.
2. `mvn clean test`: compila el reactor y ejecuta las unitarias de ambos módulos.
3. `mvn verify -DskipUTs=true`: empaqueta una sola vez y ejecuta Failsafe/integración. `skipUTs` evita repetir las unitarias ya aprobadas; no omite la integración.
4. Archiva el JAR probado con fingerprint.
5. Despliega automáticamente ese mismo JAR a producción simulada y verifica el smoke test.

Un comando Maven con salida distinta de cero detiene la ejecución. Los reportes JUnit se publican incluso cuando una etapa falla. `skipStagesAfterUnstable` también impide promover un build inestable. `disableConcurrentBuilds` evita carreras entre versiones del mismo job. El bloqueo de archivo evita dos despliegues simultáneos sobre una carpeta.

## Defensa: casos exigidos por la consigna

Mostrar brevemente la web, registrar un juego, filtrar y localizar los dos módulos. Luego mostrar Jenkinsfile y ejecutar los dos escenarios. Las modificaciones y sus commits/pushes se realizan en vivo sobre el repositorio GitHub.

**Caso 1, integración exitosa.** Cambiar en `catalogo-web/src/main/resources/application.properties` el valor de `catalogo.mensaje` a `Catalogo actualizado por Fede y Agus`. Ejecutar `mvn clean verify`, guardar el cambio en Git y subirlo a GitHub. Esperar la detección de SCM. Mostrar todas las etapas verdes, el artefacto archivado y el nuevo mensaje en <http://localhost:8081>. Comparar la revisión de `/api/estado` con el commit. No pulsar Build Now para sustituir la demostración del trigger automático.

**Caso 2, prueba fallida.** En `catalogo-core/src/main/java/uy/edu/um/catalogo/core/CatalogoService.java`, cambiar únicamente `nombre.isBlank()` por `nombre.isEmpty()`. Introduce una regresión que acepta espacios. Para esta demostración controlada, subir ese cambio sabiendo que falla (excepción académica a la práctica de no integrar código roto). Jenkins debe fallar en las pruebas unitarias. Mostrar `CatalogoServiceTest.rechazaNombreVacio`, las etapas posteriores omitidas y la revisión de producción todavía igual a la última estable. Corregir restaurando `isBlank()` o revertir el commit, volver a subir y esperar el pipeline verde. No borrar ni desactivar los tests. Ambos integrantes deben participar.

La inyección local de esta regresión detectó dos fallos de aserciones. Los dos casos se ejecutan en vivo durante la defensa, con el sondeo sobre GitHub ya conectado. El historial Git está en el repositorio; el ZIP no incluye la carpeta `.git`.

## Estructura, pruebas e informe

| Archivo | Responsabilidad |
|---|---|
| `pom.xml` | Reactor, Java 21, versiones y plugins de pruebas |
| `catalogo-core/.../CatalogoService.java` | Registro, validaciones y filtro |
| `catalogo-core/.../JsonCatalogoRepository.java` | Persistencia y duplicados |
| `catalogo-web/.../CatalogoConfig.java` | Inyección de repositorio y configuración |
| `catalogo-web/.../CatalogoController.java` | API, géneros, estado y revisión |
| `catalogo-web/.../ErroresController.java` | Traducción de errores a HTTP |
| `catalogo-web/src/main/resources/static/` | Formulario, catálogo e imágenes originales |
| `*/src/test/java/` | Pruebas exigidas por el punto 2.a |
| `Jenkinsfile` | CI/CD declarativo, sondeo, informes y despliegue |
| `scripts/deploy.ps1`, `scripts/detener.ps1` | Producción simulada y control del proceso |
| `Informe_Tecnico_Entregable4.pdf` | Arquitectura, trazabilidad, decisiones y resultados |

```powershell
mvn -B -ntp clean test
mvn -B -ntp verify -DskipUTs=true
```

Se comprobaron 19 unitarias (16 core, 3 web) y 7 de integración, todas aprobadas en la versión corregida. Surefire genera los reportes unitarios en `target/surefire-reports` de cada módulo. Failsafe genera `catalogo-web/target/failsafe-reports`. Las pruebas utilizan datos temporales, nunca el catálogo de producción. El comando único recomendado para validar desde cero sigue siendo `mvn clean verify`.

## Referencias

Consigna oficial: `Entregable 4 - CI_CD.pdf`, pp. 1-3. Teórico del curso: `05- Integración Continua - Despliegue Continuo.pdf`, especialmente pp. 24-32, 36-55 y 94-99. Ejemplos del profesor: `EjemploWin1.txt`, `EjemploWin2.txt`, `EjemploWin3.txt`; el sondeo SCM y las etapas declarativas se adaptaron de esos ejemplos. Los originales no se incluyen en la entrega.

- Jenkins DSL: <https://www.jenkins.io/doc/book/pipeline/syntax/>
- Soporte Java en Jenkins: <https://www.jenkins.io/doc/book/platform-information/support-policy-java/>
- Spring Boot: <https://docs.spring.io/spring-boot/3.5/system-requirements.html>
