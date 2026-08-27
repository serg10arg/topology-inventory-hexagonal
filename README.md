# Topology Inventory — Hexagonal Architecture with Java

Sistema de inventario de red y topología (routers, switches y redes) que modela
el dominio de una operadora de telecomunicaciones. Construido con arquitectura
hexagonal, DDD y Java Modules (JPMS) para aislar el núcleo de negocio de los
detalles tecnológicos, y preparado para entornos cloud-native con Quarkus. Se
desarrolla de forma incremental, añadiendo capacidades por fases.

## Stack tecnológico

| Tecnología | Versión | Rol en el sistema |
|------------|---------|-------------------|
| Java | 21 (LTS) | Lenguaje base |
| Maven | 3.9.x | Build multi-módulo |
| Java Modules (JPMS) | — | Aísla los hexágonos y fuerza la inversión de dependencias |
| Lombok | 1.18.34 | Reduce boilerplate en el modelo de dominio |
| JUnit 5 | 5.11.x | Tests |
| Quarkus | 3.x (LTS) | Runtime cloud-native *(fase futura)* |

## Arquitectura

![Arquitectura hexagonal](docs/images/hexagonal-architecture.png)

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

## Módulos

| Módulo (JPMS) | Hexágono | Responsabilidad |
|---------------|----------|-----------------|
| `domain` | Domain | Entities, value objects, domain services y specifications |
| `application` | Application | Use cases e input/output ports *(pendiente)* |
| `framework` | Framework | Input/output adapters *(pendiente)* |
| `bootstrap` | — | Ensambla los hexágonos y arranca la aplicación *(pendiente)* |

## Decisiones técnicas

- **Java 21 (LTS)** por records, pattern matching y virtual threads.
- **Java Modules (JPMS)** para forzar la inversión de dependencias y garantizar
  que el dominio no dependa de la infraestructura.
- **Patrón Specification** para expresar las reglas de negocio como objetos
  componibles y testables de forma aislada, en lugar de condicionales dispersos.
- **Jakarta EE 10 (`jakarta.*`)** como base del stack empresarial *(desde la fase
  de integración cloud-native)*.
- **Quarkus 3.x** por su arranque rápido y su enfoque cloud-native.

## Ejecución

**Prerrequisitos:** JDK 21 y Maven 3.9+.

```bash
# Compilar y ejecutar los tests de todo el proyecto
mvn clean verify

# Solo el módulo de dominio
mvn -pl domain test
```

*(Los comandos de Quarkus dev/build/run se añadirán en la fase de integración
cloud-native.)*

## Estado del proyecto

| Fase | Incremento | Estado |
|------|------------|--------|
| 1 | Modelo de dominio (entities, value objects, reglas de negocio) | ✅ |
| 2 | Casos de uso y puertos (capa de aplicación) | ⏸️ |
| 3 | Adapters y frontera tecnológica (capa de framework) | ⏸️ |
| 4 | Inversión de dependencias entre módulos (JPMS) | ⏸️ |
| 5 | Integración cloud-native con Quarkus | ⏸️ |
| 6 | Gestión del ciclo de vida con CDI | ⏸️ |
| 7 | API REST reactiva | ⏸️ |
| 8 | Persistencia reactiva | ⏸️ |
| 9 | Contenedores y despliegue (Docker / Kubernetes) | ⏸️ |
| 10 | Endurecimiento y buenas prácticas | ⏸️ |

