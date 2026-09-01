# Módulo `application` — Hexágono de Aplicación

> La capa que **coordina**. Traduce lo que el usuario quiere hacer en operaciones
> sobre el núcleo del negocio, sin contener reglas de negocio propias y sin saber
> nada de la tecnología de infraestructura.

Una analogía: si el módulo `domain` es el reglamento y el inventario de la red,
este módulo es **el gestor** que atiende una solicitud ("crea un router y
conéctalo a este otro"), da los pasos en el orden correcto apoyándose en el
reglamento, y delega en otros el trabajo técnico (por ejemplo, guardar el
resultado). No inventa reglas: las aplica llamando al dominio.

## ¿Qué se implementó?

La definición de **qué puede hacer el sistema** y de sus **puntos de conexión**
con el exterior. Dos tipos de piezas:

| Pieza | Qué es | Ejemplos |
|---|---|---|
| **Casos de uso** (puertos de entrada) | El catálogo de operaciones que ofrece el sistema, expresado como contratos. | `RouterManagementUseCase`, `SwitchManagementUseCase`, `NetworkManagementUseCase` |
| **Servicios de aplicación** | La implementación de esos casos de uso: coordinan al dominio paso a paso. | `RouterManagementInputPort`, `SwitchManagementInputPort`, `NetworkManagementInputPort` |
| **Puerto de salida** | Un contrato que declara lo que la aplicación necesita del exterior (p. ej. guardar/recuperar), sin decir cómo. | `RouterManagementOutputPort` |

## ¿Por qué se implementó así?

- **Para separar "coordinar" de "decidir".** Las decisiones (las reglas) viven en
  el dominio; aquí solo se orquesta la secuencia de pasos. Esta separación
  mantiene cada parte simple y enfocada.
- **Para definir una frontera clara con el exterior.** Los *puertos* son esa
  frontera: la aplicación dice *qué* necesita (por ejemplo, "persistir un
  router") y deja que otro módulo aporte el *cómo* (qué base de datos, qué
  tecnología). Así se puede cambiar la tecnología sin tocar la lógica.
- **Para que el núcleo no dependa de la infraestructura, sino al revés.** Es el
  principio de *inversión de dependencia*: la infraestructura se adaptará a lo que
  la aplicación pide, y no al contrario.

## ¿Cómo se implementó?

- Como un **módulo Java** (JPMS) que **depende del módulo `domain`** y de nada más.
- Cada caso de uso es una **interfaz** (el contrato) con una clase que lo
  implementa (el servicio que orquesta). Esta separación entre "lo que se ofrece"
  y "cómo se hace" es lo que permite enchufar el exterior más adelante sin
  sorpresas.
- La persistencia se declara con un **puerto de salida**: una interfaz que este
  módulo define pero que **implementará el módulo de infraestructura** en una
  etapa posterior.

| Tecnología | Rol en el módulo |
|---|---|
| Java 21 | Lenguaje base |
| JPMS (Java Modules) | Declara la dependencia con `domain` y aísla la capa |
| Lombok | Reduce código repetitivo |
| Cucumber + JUnit 5 | Pruebas de comportamiento (describen cada operación como un escenario legible) |

## ¿Cuál es su responsabilidad?

**Orquestar, no decidir.** Recibir una intención, ejecutarla apoyándose en el
dominio y delegar el trabajo técnico a través de los puertos.

| Sí es responsabilidad de la aplicación | NO es responsabilidad de la aplicación |
|---|---|
| Ofrecer el catálogo de operaciones (casos de uso) | Contener reglas de negocio (viven en `domain`) |
| Coordinar los pasos de cada operación | Saber qué base de datos o framework se usa |
| Definir qué necesita del exterior (puertos de salida) | Implementar esos puertos (lo hará `framework`) |

## Estado actual

El catálogo de operaciones y los puertos están definidos, y el **puerto de
salida** (persistencia) **ya está enchufado**: este módulo declara `uses` en su
`module-info` y `RouterManagementInputPort` resuelve la implementación con
`ServiceLoader` la primera vez que hace falta; la aporta el módulo `framework`
con su `provides`. La resolución es perezosa a propósito —crear, conectar y
desconectar no arrastran el arranque de la base de datos— y el núcleo sigue sin
conocer la clase concreta del adapter.

Las pruebas de este módulo (Cucumber) siguen ejercitando solo las operaciones en
memoria: sin `framework` en su grafo de módulos no hay proveedor que cargar, y
así debe ser. El camino con persistencia se verifica desde `framework`.

## ¿Cómo se relaciona con el proyecto?

Se sitúa **entre el núcleo y el mundo exterior**:

```
        framework  ─▶  application  ─▶  domain
      (mundo exterior)  (este módulo)    (núcleo)
```

- Hacia dentro, **usa `domain`** para ejecutar cada operación respetando sus reglas.
- Hacia fuera, **expone puertos** que el futuro módulo `framework` implementará
  para conectar el sistema con bases de datos y APIs.

> Para la visión global del proyecto (arquitectura completa, stack y estado),
> consulta el **README raíz** del repositorio.