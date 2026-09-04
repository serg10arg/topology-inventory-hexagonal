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
con el exterior. Tres tipos de piezas:

| Pieza | Qué es | Ejemplos |
|---|---|---|
| **Casos de uso** (puertos de entrada) | El catálogo de operaciones que ofrece el sistema, expresado como contratos. | `RouterManagementUseCase`, `SwitchManagementUseCase`, `NetworkManagementUseCase` |
| **Servicios de aplicación** | La implementación de esos casos de uso: coordinan al dominio paso a paso. Gestionados como beans CDI (`@ApplicationScoped`). | `RouterManagementInputPort`, `SwitchManagementInputPort`, `NetworkManagementInputPort` |
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
  principio de *inversión de dependencia*: la infraestructura se adapta a lo que
  la aplicación pide, y no al contrario. El contenedor CDI enchufa la
  implementación del puerto de salida sin que este módulo conozca la clase
  concreta.

## ¿Cómo se implementó?

- Como un **módulo Java** (JPMS) que **depende del módulo `domain`** y declara la
  SPI estándar de CDI (`requires jakarta.cdi`).
- Cada caso de uso es una **interfaz** (el contrato) con una clase que lo
  implementa (el servicio que orquesta). Los servicios son beans
  `@ApplicationScoped`: Arc los descubre y los enchufa en los adapters de entrada
  por `@Inject`.
- La persistencia se declara con un **puerto de salida**: una interfaz que este
  módulo define y que **implementa el módulo de framework**. La resolución la hace
  el contenedor CDI por `@Inject`, no `ServiceLoader`; como el bean del puerto es
  `@ApplicationScoped`, lo que se inyecta es un *client proxy* que difiere el
  arranque de la base de datos hasta la primera operación de persistencia.

| Tecnología | Rol en el módulo |
|---|---|
| Java 21 | Lenguaje base |
| JPMS (Java Modules) | Declara la dependencia con `domain` y la SPI de CDI; aísla la capa |
| CDI (`jakarta.cdi`) | Marca los servicios de aplicación como beans (`@ApplicationScoped`, `@Inject`) |
| Lombok | Reduce código repetitivo |
| Cucumber + JUnit 5 | Pruebas de comportamiento (describen cada operación como un escenario legible) |

## ¿Cuál es su responsabilidad?

**Orquestar, no decidir.** Recibir una intención, ejecutarla apoyándose en el
dominio y delegar el trabajo técnico a través de los puertos.

| Sí es responsabilidad de la aplicación | NO es responsabilidad de la aplicación |
|---|---|
| Ofrecer el catálogo de operaciones (casos de uso) | Contener reglas de negocio (viven en `domain`) |
| Coordinar los pasos de cada operación | Saber qué base de datos o framework se usa |
| Definir qué necesita del exterior (puertos de salida) | Implementar esos puertos (lo hace `framework`) |

## Estado actual

El catálogo de operaciones y los puertos están definidos, y el **puerto de
salida** (persistencia) **se resuelve por CDI**: los servicios de aplicación son
beans `@ApplicationScoped` y `RouterManagementInputPort` recibe la implementación
del puerto por `@Inject`; la aporta el módulo `framework`. El *client proxy* del
bean conserva la laziness que antes daba la resolución por `ServiceLoader` —crear,
conectar y desconectar no arrastran el arranque de la base de datos— y el núcleo
sigue sin conocer la clase concreta del adapter. Los servicios de switch y red no
inyectan puerto de salida: en este sistema switches y redes se persisten a través
del agregado router.

Las pruebas de este módulo (Cucumber) siguen ejercitando solo las operaciones en
memoria y se mantienen fuera del contenedor Quarkus a propósito: no tocan el
puerto de salida, así que no necesitan CDI ni un proveedor de persistencia. El
camino con persistencia se verifica desde `framework`, con Quarkus vivo.

## ¿Cómo se relaciona con el proyecto?

Se sitúa **entre el núcleo y el mundo exterior**:

```
        framework  ─▶  application  ─▶  domain
      (mundo exterior)  (este módulo)    (núcleo)
```

- Hacia dentro, **usa `domain`** para ejecutar cada operación respetando sus reglas.
- Hacia fuera, **expone puertos** que el módulo `framework` implementa para
  conectar el sistema con bases de datos y APIs.

> Para la visión global del proyecto (arquitectura completa, stack y estado),
> consulta el **README raíz** del repositorio.
