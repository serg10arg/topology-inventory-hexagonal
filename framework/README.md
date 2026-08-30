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

El **lado de salida** (*driven*) del hexágono: la implementación concreta del
puerto de persistencia sobre H2 con JPA, y la traducción entre el dominio y la
base de datos.

| Pieza | Qué es | Ejemplos |
|---|---|---|
| **Output adapter** | La implementación concreta de un puerto de salida sobre una tecnología. | `RouterManagementH2Adapter` |
| **Modelo de persistencia** | Clases espejo orientadas a la base de datos, sin lógica de negocio. | `RouterData`, `SwitchData`, `NetworkData`, `LocationData`, `IPData`, los enums `*Data`, `UUIDTypeConverter` |
| **Mapper** | El traductor entre las entidades del dominio y su espejo de persistencia. | `RouterH2Mapper` |
| **Configuración de persistencia** | La unidad JPA y los datos iniciales de arranque. | `persistence.xml`, `inventory.sql` |

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
- El output adapter se expone como **singleton** porque en esta fase el cableado de
  puertos es manual; más adelante lo gestionará la inyección de dependencias. El
  `EntityManager` se arranca de forma programática contra la unidad de persistencia
  `inventory`.
- Las entidades de persistencia usan **JPA (`jakarta.persistence`)**; el
  identificador `UUID` se mapea con un *converter* del proveedor. El paquete de
  entidades se **abre por reflexión** al proveedor en el `module-info`.

| Tecnología | Rol en el módulo |
|---|---|
| Java 21 | Lenguaje base |
| JPMS (Java Modules) | Declara la dependencia con `application`/`domain` y abre el paquete de entidades para reflexión |
| JPA (`jakarta.persistence`) | API de persistencia |
| EclipseLink 4.0.x | Proveedor JPA |
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

El **lado de salida está implementado**: el puerto de persistencia del router
queda enchufado sobre H2 con JPA, con su mapper y sus datos iniciales, y el módulo
compila. Los **adapters de entrada** (*driving*), que cablearán estos puertos a los
casos de uso de la aplicación, se implementarán en la siguiente etapa. Las pruebas
de integración del hexágono llegarán después de ese cableado.

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