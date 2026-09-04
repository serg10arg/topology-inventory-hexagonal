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

Sobre esa base, el runtime cloud-native arranca con Quarkus: el módulo `bootstrap`
enciende el contenedor (`@QuarkusMain`), y los puertos, casos de uso y adapters se
gestionan como **beans CDI** que se enchufan entre sí por `@Inject` —el output
adapter recibe su `EntityManager` gestionado y delega la transacción en
`@Transactional`—. Arc descubre esos beans en build-time a través del índice
Jandex. Los hexágonos declaran solo la **SPI estándar** (`jakarta.cdi`), que Arc
satisface en runtime, de modo que el acoplamiento a Quarkus permanece confinado en
`bootstrap`: ni el dominio ni la aplicación llegan a conocer el framework de
arranque.

## Módulos

| Módulo (JPMS) | Hexágono | Responsabilidad |
|---------------|----------|-----------------|
| `domain` | Domain | Entities, value objects, domain services y specifications |
| `application` | Application | Use cases e input/output ports, gestionados como beans CDI |
| `framework` | Framework | Input/output adapters (beans CDI): persistencia H2/JPA con Hibernate ORM gestionado por Quarkus (salida) y adapters genéricos de entrada |
| `bootstrap` | — | Ensambla los hexágonos y arranca la aplicación bajo Quarkus (`@QuarkusMain`); única costura con `quarkus.core` |

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
- **Quarkus 3.33 (LTS)** por su arranque rápido y su enfoque cloud-native; la
  costura JPMS↔Quarkus queda solo en el módulo `bootstrap` (`@QuarkusMain`,
  command mode).

## Ejecución

**Prerrequisitos:** JDK 21 y Maven 3.9+.

```bash
# Compilar y ejecutar los tests de todo el proyecto
mvn clean verify

# Solo el módulo de dominio
mvn -pl domain test

# Empaquetar la aplicación Quarkus (fast-jar)
mvn clean package

# Ejecutar el flujo de demostración (command mode): arranca, persiste y recupera, y termina
java -jar bootstrap/target/quarkus-app/quarkus-run.jar

# Modo dev con recarga en caliente
mvn -pl bootstrap -am quarkus:dev
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
| 7 | API REST reactiva | ⏸️      |
| 8 | Persistencia reactiva | ⏸️      |
| 9 | Contenedores y despliegue (Docker / Kubernetes) | ⏸️      |
| 10 | Endurecimiento y buenas prácticas | ⏸️      |
