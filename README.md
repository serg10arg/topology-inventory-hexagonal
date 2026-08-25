# Topology Inventory — Hexagonal Architecture with Java

Inventario de red y topología de una telco (routers, switches, networks),
construido con **arquitectura hexagonal**, **DDD** y **Java Modules (JPMS)**,
y preparado para cloud-native con **Quarkus**. El proyecto se desarrolla de
forma incremental, capítulo a capítulo, siguiendo el libro *Designing Hexagonal
Architecture with Java* (Davi Vieira), modernizado al stack actual.

## Stack tecnológico

| Tecnología | Versión | Rol en el sistema |
|------------|---------|-------------------|
| Java | 21 (LTS) | Lenguaje base |
| Maven | 3.9.x | Build multi-módulo |
| Java Modules (JPMS) | — | Aísla los hexágonos y fuerza la inversión de dependencias |
| Lombok | 1.18.34 | Reducción de boilerplate en el dominio |
| JUnit 5 | 5.11.x | Tests |
| Quarkus | 3.x (LTS) | Cloud-native _(a partir del Cap. 10)_ |

## Arquitectura

![Arquitectura hexagonal](docs/images/hexagonal-architecture.png)

Una petición entra por el **Framework hexagon** a través de un *input adapter*
(REST, gRPC…), que la entrega a un *input port* del **Application hexagon**. El
input port implementa un *use case* y orquesta las reglas del **Domain hexagon**
(entities, value objects y specifications). Cuando el caso de uso necesita datos
externos, los pide mediante un *output port* (una abstracción), que un *output
adapter* del Framework hexagon resuelve contra la tecnología concreta (base de
datos, broker…). La dirección de dependencia siempre apunta hacia adentro: el
negocio no conoce la tecnología, y por eso puede evolucionar sin verse arrastrado
por cambios de framework.

## Módulos

| Módulo (JPMS) | Hexágono | Responsabilidad |
|---------------|----------|-----------------|
| `domain` | Domain | Entities, value objects, domain services y specifications |
| `application` | Application | Use cases e input/output ports _(pendiente)_ |
| `framework` | Framework | Input/output adapters _(pendiente)_ |
| `bootstrap` | — | Ensambla los hexágonos y arranca la app con Quarkus _(pendiente)_ |

## Ejecución

**Prerrequisitos:** JDK 21 y Maven 3.9+.

```bash
# Compilar y pasar los tests de todo el proyecto
mvn clean verify

# Solo el módulo de dominio
mvn -pl domain test
```

_(Los comandos de Quarkus dev/build/run se añadirán a partir del Cap. 10.)_

## Estado del proyecto

| Etapa | Cap. | Descripción | Estado |
|-------|------|-------------|--------|
| 0 | — | Scaffold multi-módulo (parent + domain), Java 21 | 🚧 |
| 1 | 6 | Domain hexagon: entities, value objects, specifications | ⏸️ |
| 2 | 7 | Application hexagon: use cases e input/output ports | ⏸️ |
| 3 | 8 | Framework hexagon: input/output adapters | ⏸️ |
| 4 | 9 | Inversión de dependencias con JPMS | ⏸️ |
| 5 | 10 | Añadir Quarkus (módulo bootstrap) | ⏸️ |
| 6 | 11 | Ports y use cases como CDI beans | ⏸️ |
| 7 | 12 | Input adapters con RESTEasy Reactive | ⏸️ |
| 8 | 13 | Output adapters con Hibernate Reactive | ⏸️ |
| 9 | 14 | Dockerfile + Kubernetes | ⏸️ |
| 10 | 15 | Buenas prácticas de diseño | ⏸️ |
