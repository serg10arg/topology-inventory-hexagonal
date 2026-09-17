# Módulo `framework` — Hexágono de Framework

> La capa que **conecta**. Implementa la tecnología concreta (base de datos, y en
> el futuro protocolos de red) y adapta el mundo exterior a los puertos que define
> el núcleo. Es el anillo más externo de la arquitectura hexagonal.

Una analogía: si el módulo `domain` es el reglamento y el inventario, y el módulo
`application` es el gestor que atiende una solicitud, este módulo es **el operario
y la sala de máquinas**. Cuando el gestor dice "guarda este router", es el
framework quien abre el archivo real (la base de datos MySQL), lo guarda y traduce
entre el idioma del gestor (las entidades del dominio) y el formato del archivo
(las filas de la tabla). No decide nada: ejecuta el trabajo técnico.

## ¿Qué se implementó?

Ambos lados del hexágono: el **lado de salida** (*driven*), con la implementación
concreta del puerto de persistencia **reactiva** sobre MySQL con Hibernate Reactive y la
traducción dominio↔base de datos; y el **lado de entrada** (*driving*), con los **input
adapters REST reactivos** que reciben la petición HTTP, la traducen a DTOs de frontera y la
delegan en el caso de uso.

| Pieza | Qué es | Ejemplos |
|---|---|---|
| **Output adapter** | La implementación concreta del puerto de salida con Hibernate Reactive (`Mutiny.SessionFactory`), gestionada como bean CDI; devuelve `Uni`. | `RouterManagementH2Adapter` |
| **Input adapters (REST)** | Los puntos de entrada HTTP del sistema: recursos JAX-RS que devuelven `Uni<Response>`, con DTOs de frontera. Reactivos de punta a punta, sin `@Blocking`. Beans CDI. | `RouterManagementRestAdapter`, `SwitchManagementRestAdapter`, `NetworkManagementRestAdapter` |
| **DTOs de frontera** | Objetos de entrada/salida que aíslan el dominio del contrato HTTP. La salida es superficial (hijos como ids). | `CreateRouterRequest`, `LocationRequest`, `RouterResponse`, `SwitchResponse`, `NetworkResponse` |
| **Modelo de persistencia** | Clases espejo orientadas a la base de datos, sin lógica de negocio; ids como `String` sobre `VARCHAR(36)`. | `RouterData`, `SwitchData`, `NetworkData`, `LocationData`, `IPData`, los enums `*Data` |
| **Mapper** | El traductor entre las entidades del dominio y su espejo de persistencia (convierte `UUID`↔texto en la frontera). | `RouterH2Mapper` |
| **Configuración de persistencia** | La gestiona Quarkus desde el módulo de arranque: datasource reactivo MySQL, generación de esquema y semilla. | `application.properties`, `import.sql` (en `bootstrap`) |

## ¿Por qué se implementó así?

- **Para encapsular la tecnología en un solo lugar.** Las decisiones técnicas (qué
  base de datos, qué proveedor JPA) se postergaron a propósito hasta este anillo
  externo. El resto del sistema no las conoce.
- **Para invertir la dependencia.** El adapter *implementa* el puerto que define la
  aplicación: la infraestructura se dobla al núcleo, y no al revés. Cambiar de base
  de datos no obliga a tocar el dominio ni la aplicación.
- **Para aislar el dominio de la base de datos.** El mapper es la aduana: nada del
  mundo JPA entra al núcleo, y ninguna entidad de dominio se guarda sin traducirse
  antes a su espejo de persistencia.
- **Para respetar la raíz del agregado.** Solo hay puerto de salida para el router;
  switches y redes se persisten *a través* de él, porque el router es la raíz que
  controla el ciclo de vida del agregado.

## ¿Cómo se implementó?

- Como un **módulo Java** (JPMS) que **depende de `application` y `domain`** y
  añade la frontera tecnológica (Hibernate Reactive + cliente MySQL reactivo).
- El output adapter es un **bean CDI** (`@ApplicationScoped`) que Arc descubre por
  el índice Jandex y enchufa donde `application` declara el puerto de salida por
  `@Inject`. Recibe la `Mutiny.SessionFactory` por `@Inject` y abre sesión/transacción
  reactivas con `withSession`/`withTransaction`, devolviendo `Uni`. El agregado se
  recorre entero de forma reactiva y **serializada** (una sesión reactiva no admite
  operaciones concurrentes): la lectura materializa los hijos con `session.fetch` por
  niveles y la escritura los persiste fila a fila en orden de FK (cascade manual, sin
  `CascadeType`). Los input adapters son también beans `@ApplicationScoped` que reciben
  su caso de uso por `@Inject` y componen `Uni` sin `@Blocking`.
- El módulo declara solo **SPI estándar de Jakarta/MicroProfile** (`jakarta.cdi`,
  `jakarta.persistence`, `jakarta.ws.rs`, MicroProfile OpenAPI) y las librerías
  reactivas (`io.smallrye.mutiny`, `hibernate.reactive.core`), no módulos de Quarkus:
  Arc, RESTEasy Reactive e Hibernate Reactive las satisfacen en runtime, de modo que
  este hexágono no `requires` nada de Quarkus. **Nota de costura:** `hibernate.reactive.core`
  es un módulo *automático por nombre de fichero* (su jar no trae `module-info.class` ni
  `Automatic-Module-Name`); es el eslabón frágil del descriptor y Maven lo advierte
  (`Required filename-based automodules detected`).
- Las entidades de persistencia usan **JPA (`jakarta.persistence`)** con ids `String`
  sobre `VARCHAR(36)` (el dominio conserva `UUID` en su value object `Id`; la conversión
  vive en el mapper). El paquete de entidades se **abre por reflexión** al proveedor
  (Hibernate Reactive) en el `module-info` para que pueda acceder a sus campos privados.

| Tecnología | Rol en el módulo |
|---|---|
| Java 21 | Lenguaje base |
| JPMS (Java Modules) | Declara la dependencia con `application`/`domain` y abre el paquete de entidades para reflexión |
| CDI (`jakarta.cdi`) | Marca adapters y output adapter como beans (`@ApplicationScoped`, `@Inject`); Arc los resuelve |
| JPA (`jakarta.persistence`) | API de persistencia; anota las entidades del modelo de salida |
| SmallRye Mutiny (`io.smallrye.mutiny`) | Tipo `Uni` de los endpoints y del puerto de salida; composición reactiva |
| Hibernate Reactive (`hibernate.reactive.core`, vía Quarkus) | Proveedor de persistencia reactiva (`Mutiny.SessionFactory`); genera el DDL desde las entidades |
| Cliente MySQL reactivo (Vert.x) | Driver no bloqueante (`quarkus-reactive-mysql-client`) |
| MySQL (Dev Services) | Base de datos, provisionada en contenedor (requiere Docker) |
| Lombok | Builders y getters de las entidades de persistencia |
| Maven | Construcción multi-módulo |

## ¿Cuál es su responsabilidad?

**Implementar la tecnología y traducir en la frontera.** Aportar el *cómo*
concreto de lo que la aplicación pide, sin contener reglas ni orquestación.

| Sí es responsabilidad del framework | NO es responsabilidad del framework |
|---|---|
| Implementar los puertos de salida | Contener reglas de negocio (viven en `domain`) |
| Hablar con la base de datos | Orquestar los casos de uso (lo hace `application`) |
| Traducir entre dominio y persistencia | Exponer el dominio a tipos de base de datos |
| Encapsular las decisiones tecnológicas | Definir *qué* operaciones ofrece el sistema |

## Estado actual

Los **dos lados están implementados, gestionados por CDI y verificados**. El de salida
enchufa el puerto de persistencia **reactiva** del router sobre MySQL con Hibernate
Reactive: el output adapter es un bean `@ApplicationScoped` que Arc inyecta donde
`application` declara el puerto, con la `Mutiny.SessionFactory` por `@Inject` y la
transacción por `withTransaction`, devolviendo `Uni`. Recorre el agregado entero de forma
reactiva y serializada: lo lee con `session.fetch` por niveles y lo persiste fila a fila
(cascade manual). El de entrada son los **REST adapters reactivos** (`/router`, `/switch`,
`/network`): devuelven `Uni<Response>`, componen la `Uni` de punta a punta **sin
`@Blocking`**, y traducen entre DTOs de frontera y el dominio. El contrato se publica con
MicroProfile OpenAPI (`@Tag`/`@Operation`) en `/q/openapi`.

Las pruebas del módulo (16) corren bajo `@QuarkusTest` + REST Assured (con MySQL vía Dev
Services) y cubren los endpoints de router, switch y red por HTTP real, la integración
reactiva del output adapter (lectura del semilla, escritura y **round-trip del agregado con
hijos**), y el contrato OpenAPI. **Deuda saldada:** la escritura profunda persiste ya el
agregado con sus hijos (el `@OneToMany` sin cascade que se arrastraba desde la Fase 4).
**Deuda pendiente:** la superficie REST aún no usa esa escritura profunda — `switch/add` y
`network/add` recuperan el agregado, lo mutan y lo devuelven, pero no persisten la conexión;
y el paquete/clases conservan el nombre `h2` pese a correr ya sobre MySQL (rename cosmético
diferido).

## ¿Cómo se relaciona con el proyecto?

Es el anillo más externo; las dependencias apuntan siempre hacia dentro:

```
        framework  ─▶  application  ─▶  domain
      (este módulo)    (coordinación)    (núcleo)
```

- Hacia dentro, **implementa los puertos de salida** que define `application` y
  **usa las entidades** de `domain`.
- Hacia fuera, **conecta el sistema con la tecnología real** (hoy una base de datos
  MySQL con Hibernate Reactive; más adelante, otros adapters de entrada y salida).

> Para la visión global del proyecto (arquitectura completa, stack y estado),
> consulta el **README raíz** del repositorio.