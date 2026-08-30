# Paquete `input/generic` — Adapters de entrada genéricos

> El **lado de entrada** (*driving*) del hexágono de framework. Son los puntos por
> los que el mundo exterior invoca al sistema: reciben la petición y la reenvían
> al caso de uso correspondiente, sin lógica propia. Hoy son POJOs; serán la base
> de los futuros adapters REST.

Una analogía: son los **camareros**. Toman la comanda en la mesa y la reenvían a
la cocina (los casos de uso); no cocinan ni deciden la receta. Ahora mismo toman
los pedidos "a mano" (como POJOs); más adelante se pondrán el uniforme REST, pero
su trabajo —recibir y reenviar— será el mismo.

## ¿Qué se implementó?

Un adapter por cada caso de uso de la aplicación.

| Adapter | Reenvía a | Operaciones |
|---|---|---|
| `RouterManagementGenericAdapter` | `RouterManagementUseCase` | crear, conectar y desconectar routers (y recuperar/persistir, cuando se cablee la salida) |
| `SwitchManagementGenericAdapter` | `SwitchManagementUseCase` | crear, conectar y desconectar switches |
| `NetworkManagementGenericAdapter` | `NetworkManagementUseCase` | crear redes y añadirlas/quitarlas de un switch |

## ¿Por qué se implementó así?

- **Para tener un punto de entrada desacoplado del protocolo.** Hoy son POJOs;
  mañana el mismo adapter se expondrá por REST sin tocar el núcleo. El protocolo se
  enchufa por fuera.
- **Para reflejar el contrato de la aplicación uno a uno.** Cada adapter delega en
  su caso de uso con las mismas firmas: no añade reglas ni orquestación oculta.
- **Para preparar el terreno de la persistencia sin adelantarla.** La recuperación
  y el guardado dependen del puerto de salida, que se cablea en la fase de
  inyección de dependencias; por eso aquí quedan declarados pero inactivos.

## ¿Cómo se implementó?

- Cada adapter **instancia su input port** con el constructor sin argumentos
  (`new RouterManagementInputPort()`, etc.) y **delega** cada método en el caso de
  uso.
- Los métodos llevan en su Javadoc el **endpoint REST** que representarán, como guía
  para el adapter web futuro.
- El cableado del **puerto de salida** (para `retrieveRouter`/`persistRouter`) se
  hará mediante JPMS `provides/uses` en la fase de inyección de dependencias.

## ¿Cuál es su responsabilidad?

**Recibir la petición e invocar el caso de uso.** Ser la puerta de entrada, nada
más.

| Sí es responsabilidad de `input/generic` | NO es responsabilidad de `input/generic` |
|---|---|
| Recibir la petición del exterior | Contener reglas de negocio (viven en `domain`) |
| Delegar en el caso de uso adecuado | Orquestar la persistencia (aún no cableada) |
| Ser el punto que luego se expone por REST | Conocer la tecnología de salida (H2, JPA) |

## Estado actual

Implementado. Las operaciones de **crear, conectar y desconectar** son operativas.
Las de **recuperar y persistir** están declaradas y se activarán al cablear el
puerto de salida en la fase de inyección de dependencias.

## ¿Cómo se relaciona con el proyecto?

Es la puerta de entrada del hexágono más externo:

```
   mundo exterior  ──▶  input/generic  ──▶  application (use cases)  ──▶  domain
```

- Reciben del exterior y **reenvían a los casos de uso** de `application`, que a su
  vez operan sobre el `domain`.

> Para el propósito y el estado del módulo completo, consulta el
> **README del módulo `framework`**.