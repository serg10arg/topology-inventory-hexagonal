# Paquete `input/generic` — Adapters de entrada genéricos

> El **lado de entrada** (*driving*) del hexágono de framework. Son los puntos por
> los que el mundo exterior invoca al sistema: reciben la petición y la reenvían
> al caso de uso correspondiente, sin lógica propia. Hoy son beans CDI; serán la
> base de los futuros adapters REST.

Una analogía: son los **camareros**. Toman la comanda en la mesa y la reenvían a
la cocina (los casos de uso); no cocinan ni deciden la receta. El restaurante ya
los tiene en plantilla (el contenedor CDI los gestiona y les asigna su cocina);
más adelante se pondrán el uniforme REST, pero su trabajo —recibir y reenviar—
será el mismo.

## ¿Qué se implementó?

Un adapter por cada caso de uso de la aplicación.

| Adapter | Reenvía a | Operaciones |
|---|---|---|
| `RouterManagementGenericAdapter` | `RouterManagementUseCase` | crear, conectar y desconectar routers; recuperar y persistir |
| `SwitchManagementGenericAdapter` | `SwitchManagementUseCase` | crear, conectar y desconectar switches |
| `NetworkManagementGenericAdapter` | `NetworkManagementUseCase` | crear redes y añadirlas/quitarlas de un switch |

## ¿Por qué se implementó así?

- **Para tener un punto de entrada desacoplado del protocolo.** Hoy son beans CDI;
  mañana el mismo adapter se expondrá por REST sin tocar el núcleo. El protocolo se
  enchufa por fuera.
- **Para reflejar el contrato de la aplicación uno a uno.** Cada adapter delega en
  su caso de uso con las mismas firmas: no añade reglas ni orquestación oculta.
- **Para mantener la persistencia fuera de la vista del adapter.** La recuperación
  y el guardado se delegan igual que el resto: el puerto de salida lo resuelve el
  application service por CDI, así que este adapter no sabe qué hay al otro lado.

## ¿Cómo se implementó?

- Cada adapter es un **bean `@ApplicationScoped`** que **recibe su caso de uso por
  `@Inject`** y **delega** cada método en él (sustituye al `new ...InputPort()` del
  constructor anterior).
- Los métodos llevan en su Javadoc el **endpoint REST** que representarán, como guía
  para el adapter web futuro.
- El **puerto de salida** (para `retrieveRouter`/`persistRouter`) lo resuelve el
  application service por CDI (`@Inject`): ambos métodos son operativos con el
  contenedor vivo.

## ¿Cuál es su responsabilidad?

**Recibir la petición e invocar el caso de uso.** Ser la puerta de entrada, nada
más.

| Sí es responsabilidad de `input/generic` | NO es responsabilidad de `input/generic` |
|---|---|
| Recibir la petición del exterior | Contener reglas de negocio (viven en `domain`) |
| Delegar en el caso de uso adecuado | Orquestar la persistencia (lo hace el application service) |
| Ser el punto que luego se expone por REST | Conocer la tecnología de salida (H2, JPA) |

## Estado actual

Implementado y gestionado por CDI. Todas las operaciones —**crear, conectar,
desconectar** y, para el router, **recuperar y persistir**— son operativas: cada
adapter recibe su caso de uso por `@Inject` y el application service resuelve el
puerto de salida por CDI cuando hace falta.

## ¿Cómo se relaciona con el proyecto?

Es la puerta de entrada del hexágono más externo:

```
   mundo exterior  ──▶  input/generic  ──▶  application (use cases)  ──▶  domain
```

- Reciben del exterior y **reenvían a los casos de uso** de `application`, que a su
  vez operan sobre el `domain`.

> Para el propósito y el estado del módulo completo, consulta el
> **README del módulo `framework`**.
