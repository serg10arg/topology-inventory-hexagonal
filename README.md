# Topology Inventory — Hexagonal Architecture with Java

Sistema de inventario de red y topología (routers, switches y redes) que modela
el dominio de una operadora de telecomunicaciones. Construido con arquitectura
hexagonal, DDD y Java Modules (JPMS) para aislar el núcleo de negocio de los
detalles tecnológicos, y ejecutado sobre Quarkus para entornos cloud-native. Se
desarrolla de forma incremental, añadiendo capacidades por fases.

## Stack tecnológico

| Tecnología | Versión | Rol en el sistema |
|------------|---------|-------------------|
| Java | 21 (LTS) | Lenguaje base |
| Maven | 3.9.x | Build multi-módulo |
| Java Modules (JPMS) | — | Aísla los hexágonos y fuerza la inversión de dependencias |
| Lombok | 1.18.34 | Reduce boilerplate en el modelo de dominio |
| Quarkus | 3.33 (LTS) | Runtime cloud-native: motor de arranque, de inyección (Arc) y de persistencia |
| CDI (Jakarta / Quarkus Arc) | Quarkus BOM | Gestiona puertos, casos de uso y el output adapter como beans (`@ApplicationScoped`, `@Inject`) |
| Jakarta Persistence (JPA) | Quarkus BOM | API de persistencia; anota las entidades del modelo de salida |
| Hibernate Reactive | Quarkus BOM | Proveedor de persistencia **reactiva** (`Mutiny.SessionFactory`); genera el DDL desde las entidades |
| Cliente MySQL reactivo (Vert.x) | Quarkus BOM | Driver no bloqueante (`quarkus-reactive-mysql-client`); el stack reactivo no soporta H2 |
| MySQL | 8.x (Dev Services) | Base de datos, provisionada en contenedor por Quarkus Dev Services en dev/test |
| Docker | — | Prerrequisito de runtime: Dev Services levanta el contenedor MySQL |
| RESTEasy Reactive (Quarkus REST) | Quarkus BOM | Input adapters HTTP reactivos (`quarkus-rest` / `quarkus-rest-jackson`) |
| SmallRye Mutiny | Quarkus BOM | Tipo reactivo `Uni` de punta a punta: endpoints y frontera de persistencia |
| MicroProfile OpenAPI + Swagger UI | Quarkus BOM | Contrato de la API (`/q/openapi`) y UI navegable (`/q/swagger-ui`) |
| REST Assured | Quarkus BOM | Ejercita los endpoints por HTTP en los `@QuarkusTest` |
| Mutiny UniAsserter (`quarkus-test-vertx`) | Quarkus BOM | Aserciones reactivas sobre `Uni` en los `@QuarkusTest` del output adapter |
| Jandex (SmallRye) | 3.5.3 | Índice de clases en build-time (descubrimiento de entidades y beans entre módulos) |
| JUnit | 5.11.x · 6.0.3 (framework) | Tests unitarios y de integración (`@QuarkusTest`) |
| Cucumber | 7.20.x | Tests de aceptación (BDD) sobre JUnit 5 Platform |

## Arquitectura

![Arquitectura hexagonal](docs/images/the-hexagonal-architecture.png)

El sistema se organiza en tres hexágonos concéntricos. Una petición entra por el
**Framework hexagon** a través de un *input adapter* (REST, gRPC…), que la
entrega a un *input port* del **Application hexagon**. El input port implementa un
*use case* y orquesta las reglas del **Domain hexagon** (entities, value objects y
specifications). Cuando el caso de uso necesita datos externos, los solicita
mediante un *output port* (una abstracción), que un *output adapter* del Framework
hexagon resuelve contra la tecnología concreta (base de datos, broker…). La
dirección de dependencia siempre apunta hacia adentro: el negocio no conoce la
tecnología, y por eso puede evolucionar sin verse arrastrado por cambios de
framework.

Sobre esa base, el runtime cloud-native corre sobre Quarkus con una cadena **reactiva de
punta a punta**. Una petición HTTP entra por un **input adapter REST reactivo**
(`RouterManagementRestAdapter` y sus pares de switch y red), que devuelve `Uni<Response>`;
el adapter traduce la petición a DTOs de frontera (sin exponer entidades de dominio ni de
persistencia), delega en el caso de uso y proyecta la respuesta a DTOs superficiales (los
hijos del agregado viajan como ids). Cuando la operación llega a la frontera de salida, el
output adapter la resuelve con **Hibernate Reactive** (`Mutiny.SessionFactory`) sobre un
**cliente MySQL no bloqueante**, componiendo `Uni` sin cambiar de hilo: por eso ya no hay
`@Blocking` en ningún endpoint. El agregado se recorre entero de forma reactiva —la lectura
materializa los hijos con `session.fetch` por niveles y la escritura los persiste fila a
fila—, siempre serializado, porque una sesión reactiva no admite operaciones concurrentes.
Los puertos, casos de uso y el output adapter se gestionan como **beans CDI** enchufados
por `@Inject`; el output adapter recibe la `Mutiny.SessionFactory` y demarca con
`withTransaction`. Arc descubre los beans en build-time por el índice Jandex. Los hexágonos
declaran solo **SPI estándar** más librerías reactivas (`jakarta.cdi`, `jakarta.ws.rs`,
`io.smallrye.mutiny`, MicroProfile OpenAPI), de modo que el acoplamiento a Quarkus permanece
confinado en `bootstrap`. El módulo `bootstrap` arranca el contenedor con `@QuarkusMain` +
`Quarkus.run(...)` y **se mantiene sirviendo** hasta el shutdown; el contrato de la API se
publica en `/q/openapi` y se explora en `/q/swagger-ui`.

## Módulos

| Módulo (JPMS) | Hexágono | Responsabilidad |
|---------------|----------|-----------------|
| `domain` | Domain | Entities, value objects, domain services y specifications |
| `application` | Application | Use cases e input/output ports, gestionados como beans CDI |
| `framework` | Framework | Input/output adapters (beans CDI): input adapters **REST reactivos** con DTOs de frontera (entrada) y persistencia **reactiva** sobre MySQL con Hibernate Reactive (salida) |
| `bootstrap` | — | Ensambla los hexágonos y arranca la aplicación bajo Quarkus (`@QuarkusMain` + `Quarkus.run`, **server mode**); única costura con `quarkus.core` |

## Decisiones técnicas

- **Java 21 (LTS)** por records, pattern matching y virtual threads.
- **Java Modules (JPMS)** para forzar la inversión de dependencias y garantizar
  que el dominio no dependa de la infraestructura.
- **Patrón Specification** para expresar las reglas de negocio como objetos
  componibles y testables de forma aislada, en lugar de condicionales dispersos.
- **Jakarta EE 10 (`jakarta.*`)** como base del stack empresarial.
- **Hibernate Reactive + MySQL** en la frontera de salida, aislados del dominio
  mediante mappers; el DDL lo genera Hibernate desde las entidades y el seed vive en
  `import.sql`. El motor reactivo (`Mutiny.SessionFactory`) solo expone `Uni`, así que
  el puerto de salida es reactivo; el cliente de Vert.x no soporta H2, por lo que la
  base pasa a MySQL, provisionada por Dev Services en contenedor. El agregado se
  recorre entero de forma reactiva —lectura con `session.fetch` por niveles, escritura
  fila a fila con cascade manual—, siempre serializado, porque una sesión reactiva no
  admite operaciones concurrentes.
- **Cableado por CDI sin acoplar los hexágonos a Quarkus:** los puertos de entrada,
  los casos de uso y el output adapter son beans `@ApplicationScoped` que se
  enchufan por `@Inject`; el output adapter recibe la `Mutiny.SessionFactory` por
  `@Inject` y demarca la transacción con `withTransaction`. Los hexágonos requieren
  solo la SPI estándar `jakarta.cdi` y librerías reactivas —no módulos de Quarkus—,
  que Arc satisface en runtime; así el acoplamiento a Quarkus queda confinado en
  `bootstrap`.
- **API REST reactiva sobre SPI estándar:** los input adapters usan JAX-RS
  (`jakarta.ws.rs`) y devuelven `Uni` (Mutiny). Con la persistencia también reactiva,
  la `Uni` se compone de punta a punta y **ningún endpoint necesita `@Blocking`**; el
  contrato se documenta con MicroProfile OpenAPI. DTOs de entrada/salida aíslan la
  frontera: ni el dominio ni las entidades de persistencia viajan por HTTP. La costura
  JAX-RS/OpenAPI son módulos estándar de Jakarta/MicroProfile, no de Quarkus.
- **Quarkus 3.33 (LTS)** por su arranque rápido y su enfoque cloud-native; la app corre
  en **server mode** (`@QuarkusMain` + `Quarkus.run`, se mantiene sirviendo HTTP) y la
  costura JPMS↔Quarkus queda solo en el módulo `bootstrap`.

## Ejecución

**Prerrequisitos:** JDK 21, Maven 3.9+ y **Docker** (Quarkus Dev Services levanta el
contenedor MySQL para los tests y el modo dev; el demonio debe estar corriendo).

```bash
# Compilar y ejecutar los tests de todo el proyecto (requiere Docker en marcha)
mvn clean verify

# Solo el módulo de dominio
mvn -pl domain test

# Empaquetar la aplicación Quarkus (fast-jar)
mvn clean package

# Arrancar el servicio HTTP (server mode): se queda escuchando en el puerto 8080
# En perfil prod necesita un MySQL real (ver quarkus.datasource.reactive.url en bootstrap);
# en dev/test lo provisiona Dev Services en contenedor.
java -jar bootstrap/target/quarkus-app/quarkus-run.jar

# Modo dev con recarga en caliente (Dev Services levanta MySQL vía Docker)
mvn -pl bootstrap -am quarkus:dev
```

Con el servicio arriba, el contrato de la API está en `http://localhost:8080/q/openapi`
y la UI navegable en `http://localhost:8080/q/swagger-ui/`. Ejemplo de llamada:

```bash
curl http://localhost:8080/router/retrieve/b832ef4f-f894-4194-8feb-a99c2cd4be0c
```

## Estado del proyecto

| Fase | Incremento | Estado |
|------|------------|--------|
| 1 | Modelo de dominio (entities, value objects, reglas de negocio) | ✅     |
| 2 | Casos de uso y puertos (capa de aplicación) | ✅     |
| 3 | Adapters y frontera tecnológica (capa de framework) | ✅     |
| 4 | Inversión de dependencias entre módulos (JPMS) | ✅     |
| 5 | Integración cloud-native con Quarkus | ✅     |
| 6 | Gestión del ciclo de vida con CDI | ✅     |
| 7 | API REST reactiva | ✅     |
| 8 | Persistencia reactiva (Hibernate Reactive + MySQL) | ✅     |
| 9 | Contenedores y despliegue (Docker / Kubernetes) | ⏸️      |
| 10 | Endurecimiento y buenas prácticas | ⏸️      |