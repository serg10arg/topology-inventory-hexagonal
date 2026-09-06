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
| Jakarta Persistence (JPA) | Quarkus BOM | API de persistencia (frontera de salida) |
| Hibernate ORM | Quarkus BOM | Proveedor JPA gestionado por Quarkus; genera el DDL desde las entidades |
| H2 | Quarkus BOM | Base de datos en memoria (driver `quarkus-jdbc-h2`) |
| Agroal / Narayana JTA | Quarkus BOM | Pool de conexiones y transacciones (`@Transactional`) |
| RESTEasy Reactive (Quarkus REST) | Quarkus BOM | Input adapters HTTP reactivos (`quarkus-rest` / `quarkus-rest-jackson`) |
| SmallRye Mutiny | Quarkus BOM | Tipo reactivo `Uni` en los endpoints |
| MicroProfile OpenAPI + Swagger UI | Quarkus BOM | Contrato de la API (`/q/openapi`) y UI navegable (`/q/swagger-ui`) |
| REST Assured | Quarkus BOM | Ejercita los endpoints por HTTP en los `@QuarkusTest` |
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

Sobre esa base, el runtime cloud-native corre sobre Quarkus. Una petición HTTP entra
por un **input adapter REST reactivo** (`RouterManagementRestAdapter` y sus pares de
switch y red), que devuelve `Uni<Response>` y, al llegar la operación hasta Hibernate
ORM —bloqueante—, se marca `@Blocking` para ejecutarse en un *worker thread* en lugar
del *event loop*. El adapter traduce la petición a DTOs de frontera (sin exponer
entidades de dominio ni de persistencia), delega en el caso de uso y proyecta la
respuesta a DTOs superficiales (los hijos del agregado viajan como ids). Los puertos,
casos de uso y el output adapter se gestionan como **beans CDI** enchufados por
`@Inject`; el output adapter recibe su `EntityManager` gestionado y delega la
transacción en `@Transactional`. Arc descubre los beans en build-time por el índice
Jandex. Los hexágonos declaran solo **SPI estándar** (`jakarta.cdi`, `jakarta.ws.rs`,
MicroProfile OpenAPI), de modo que el acoplamiento a Quarkus permanece confinado en
`bootstrap`. El módulo `bootstrap` arranca el contenedor con `@QuarkusMain` +
`Quarkus.run(...)` y **se mantiene sirviendo** hasta el shutdown; el contrato de la API
se publica en `/q/openapi` y se explora en `/q/swagger-ui`.

## Módulos

| Módulo (JPMS) | Hexágono | Responsabilidad |
|---------------|----------|-----------------|
| `domain` | Domain | Entities, value objects, domain services y specifications |
| `application` | Application | Use cases e input/output ports, gestionados como beans CDI |
| `framework` | Framework | Input/output adapters (beans CDI): input adapters **REST reactivos** con DTOs de frontera (entrada) y persistencia H2/JPA con Hibernate ORM gestionado por Quarkus (salida) |
| `bootstrap` | — | Ensambla los hexágonos y arranca la aplicación bajo Quarkus (`@QuarkusMain` + `Quarkus.run`, **server mode**); única costura con `quarkus.core` |

## Decisiones técnicas

- **Java 21 (LTS)** por records, pattern matching y virtual threads.
- **Java Modules (JPMS)** para forzar la inversión de dependencias y garantizar
  que el dominio no dependa de la infraestructura.
- **Patrón Specification** para expresar las reglas de negocio como objetos
  componibles y testables de forma aislada, en lugar de condicionales dispersos.
- **Jakarta EE 10 (`jakarta.*`)** como base del stack empresarial.
- **Hibernate ORM (gestionado por Quarkus) + H2** en memoria en la frontera de
  salida, aislados del dominio mediante mappers; el DDL lo genera Hibernate desde
  las entidades y el seed vive en `import.sql`.
- **Cableado por CDI sin acoplar los hexágonos a Quarkus:** los puertos de entrada,
  los casos de uso y el output adapter son beans `@ApplicationScoped` que se
  enchufan por `@Inject`; el output adapter recibe el `EntityManager` gestionado por
  `@Inject` y demarca la transacción con `@Transactional`. Los hexágonos requieren
  solo la SPI estándar `jakarta.cdi` —no módulos de Quarkus—, que Arc satisface en
  runtime; así el acoplamiento a Quarkus queda confinado en `bootstrap`.
- **API REST reactiva sobre SPI estándar:** los input adapters usan JAX-RS
  (`jakarta.ws.rs`) y devuelven `Uni` (Mutiny), con `@Blocking` en los endpoints que
  tocan la persistencia bloqueante; el contrato se documenta con MicroProfile OpenAPI.
  DTOs de entrada/salida aíslan la frontera: ni el dominio ni las entidades de
  persistencia viajan por HTTP. La costura JAX-RS/OpenAPI son módulos estándar de
  Jakarta/MicroProfile, no de Quarkus.
- **Quarkus 3.33 (LTS)** por su arranque rápido y su enfoque cloud-native; la app corre
  en **server mode** (`@QuarkusMain` + `Quarkus.run`, se mantiene sirviendo HTTP) y la
  costura JPMS↔Quarkus queda solo en el módulo `bootstrap`.

## Ejecución

**Prerrequisitos:** JDK 21 y Maven 3.9+.

```bash
# Compilar y ejecutar los tests de todo el proyecto
mvn clean verify

# Solo el módulo de dominio
mvn -pl domain test

# Empaquetar la aplicación Quarkus (fast-jar)
mvn clean package

# Arrancar el servicio HTTP (server mode): se queda escuchando en el puerto 8080
java -jar bootstrap/target/quarkus-app/quarkus-run.jar

# Modo dev con recarga en caliente
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
| 8 | Persistencia reactiva | ⏸️      |
| 9 | Contenedores y despliegue (Docker / Kubernetes) | ⏸️      |
| 10 | Endurecimiento y buenas prácticas | ⏸️      |
