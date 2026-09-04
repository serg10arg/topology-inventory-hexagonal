# Módulo `framework` — Hexágono de Framework

> La capa que **conecta**. Implementa la tecnología concreta (base de datos, y en
> el futuro protocolos de red) y adapta el mundo exterior a los puertos que define
> el núcleo. Es el anillo más externo de la arquitectura hexagonal.

Una analogía: si el módulo `domain` es el reglamento y el inventario, y el módulo
`application` es el gestor que atiende una solicitud, este módulo es **el operario
y la sala de máquinas**. Cuando el gestor dice "guarda este router", es el
framework quien abre el archivo real (la base de datos H2), lo guarda y traduce
entre el idioma del gestor (las entidades del dominio) y el formato del archivo
(las filas de la tabla). No decide nada: ejecuta el trabajo técnico.

## ¿Qué se implementó?

Ambos lados del hexágono: el **lado de salida** (*driven*), con la implementación
concreta del puerto de persistencia sobre H2 con JPA y la traducción dominio↔base
de datos; y el **lado de entrada** (*driving*), con los adapters genéricos que
reciben la petición y la reenvían al caso de uso.

| Pieza | Qué es | Ejemplos |
|---|---|---|
| **Output adapter** | La implementación concreta de un puerto de salida sobre una tecnología, gestionada como bean CDI. | `RouterManagementH2Adapter` |
| **Input adapters** | Los puntos de entrada del sistema (base del futuro adapter REST), gestionados como beans CDI. | `RouterManagementGenericAdapter`, `SwitchManagementGenericAdapter`, `NetworkManagementGenericAdapter` |
| **Modelo de persistencia** | Clases espejo orientadas a la base de datos, sin lógica de negocio. | `RouterData`, `SwitchData`, `NetworkData`, `LocationData`, `IPData`, los enums `*Data` |
| **Mapper** | El traductor entre las entidades del dominio y su espejo de persistencia. | `RouterH2Mapper` |
| **Configuración de persistencia** | La gestiona Quarkus desde el módulo de arranque: datasource, generación de esquema y semilla. | `application.properties`, `import.sql` (en `bootstrap`) |

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
  añade la frontera tecnológica (JPA + H2).
- El output adapter es un **bean CDI** (`@ApplicationScoped`) que Arc descubre por
  el índice Jandex y enchufa donde `application` declara el puerto de salida por
  `@Inject`. Recibe el `EntityManager` gestionado por `@Inject` y delega la
  transacción en `@Transactional`, sin abrirla ni confirmarla a mano. Los input
  adapters son también beans `@ApplicationScoped` que reciben su caso de uso por
  `@Inject`.
- El módulo declara solo la **SPI estándar de Jakarta** (`jakarta.cdi`,
  `jakarta.transaction`, `jakarta.persistence`), no módulos de Quarkus: Arc la
  satisface en runtime, de modo que este hexágono no `requires` nada de Quarkus.
- Las entidades de persistencia usan **JPA (`jakarta.persistence`)** con UUID
  nativo. El paquete de entidades se **abre por reflexión** al proveedor
  (Hibernate) en el `module-info` para que pueda acceder a sus campos privados.

| Tecnología | Rol en el módulo |
|---|---|
| Java 21 | Lenguaje base |
| JPMS (Java Modules) | Declara la dependencia con `application`/`domain` y abre el paquete de entidades para reflexión |
| CDI (`jakarta.cdi`) | Marca adapters y output adapter como beans (`@ApplicationScoped`, `@Inject`); Arc los resuelve |
| JPA (`jakarta.persistence`) | API de persistencia |
| Jakarta Transactions | Demarcación declarativa con `@Transactional` |
| Hibernate ORM (vía Quarkus) | Proveedor JPA; genera el DDL desde las entidades |
| H2 | Base de datos en memoria |
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

Los **dos lados están implementados, gestionados por CDI y verificados**. El de
salida enchufa el puerto de persistencia del router sobre H2 con JPA: el output
adapter es un bean `@ApplicationScoped` que Arc inyecta donde `application` declara
el puerto, con el `EntityManager` por `@Inject` y la transacción por
`@Transactional`. El de entrada son los generic adapters —beans CDI, hoy la base
del futuro adapter REST—, con `retrieveRouter`/`persistRouter` operativos.

Las pruebas del módulo (5) corren bajo `@QuarkusTest` y cubren la integración del
output adapter contra H2 y el recorrido end-to-end por los generic adapters (que,
al inyectarse los tres, hace además de *smoke test* del grafo de cableado).
**Deuda conocida:** los `@OneToMany` del modelo de persistencia no cascadan, así
que todavía no se guarda el agregado con sus hijos (solo routers sueltos).

## ¿Cómo se relaciona con el proyecto?

Es el anillo más externo; las dependencias apuntan siempre hacia dentro:

```
        framework  ─▶  application  ─▶  domain
      (este módulo)    (coordinación)    (núcleo)
```

- Hacia dentro, **implementa los puertos de salida** que define `application` y
  **usa las entidades** de `domain`.
- Hacia fuera, **conecta el sistema con la tecnología real** (hoy una base de datos
  H2; más adelante, otros adapters de entrada y salida).

> Para la visión global del proyecto (arquitectura completa, stack y estado),
> consulta el **README raíz** del repositorio.
