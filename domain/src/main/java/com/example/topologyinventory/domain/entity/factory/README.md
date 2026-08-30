# Paquete `factory` — Fábrica de routers

> El **único punto de construcción** de routers por tipo. Decide si lo que hay que
> crear es un `CoreRouter` o un `EdgeRouter` y oculta esa decisión al resto del
> sistema.

Una analogía: una fábrica de verdad. Haces un pedido ("quiero un router de tipo
CORE") y la cadena de montaje te devuelve el modelo correcto, ya ensamblado. Tú no
juntas las piezas ni necesitas saber qué línea de producción se usó: pides por
tipo y recibes el producto terminado.

## ¿Qué se implementó?

| Pieza | Qué es |
|---|---|
| `RouterFactory` | Un método de creación que, según el `RouterType`, devuelve el subtipo de router correcto (`CoreRouter` o `EdgeRouter`) ya construido. |

## ¿Por qué se implementó así?

- **Para tener una sola vía de creación.** En lugar de instanciar routers a mano en
  distintos sitios (con el riesgo de olvidar algún paso), toda creación pasa por
  aquí.
- **Para ocultar qué subclase corresponde a cada tipo.** Quien pide un router no
  necesita conocer la jerarquía `CoreRouter`/`EdgeRouter`: pide por `RouterType` y
  la fábrica resuelve.
- **Para que el caso de uso `createRouter` tenga un punto claro donde apoyarse.**

## ¿Cómo se implementó?

- Como un método **estático** que, en función del `RouterType`, usa el *builder* de
  `CoreRouter` o de `EdgeRouter`.
- Vive **dentro del dominio** (subpaquete de `entity`) porque decidir la forma del
  agregado es una responsabilidad del núcleo, no de la infraestructura.

## ¿Cuál es su responsabilidad?

**Crear el router correcto y entregarlo listo para usar.**

| Sí es responsabilidad de `factory` | NO es responsabilidad de `factory` |
|---|---|
| Construir el subtipo de router según su tipo | Persistir el router (lo hace `framework`) |
| Devolverlo con su estado inicial coherente | Conectarlo a otros (son métodos de la entity) |
| — | Aplicar las reglas de conexión (eso es `specification`) |

## Estado actual

Implementado. Lo usa el caso de uso `createRouter` de la capa de aplicación como
punto de entrada para materializar un router.

## ¿Cómo se relaciona con el proyecto?

Es la puerta de creación del agregado:

```
   application (createRouter)  ──▶  factory  ──▶  entity (CoreRouter / EdgeRouter)
```

- La **aplicación** pide un router por tipo; la **fábrica** devuelve la entity
  concreta, que a partir de ahí aplica sus propias reglas.

> Para el propósito y el estado del módulo completo, consulta el
> **README del módulo `domain`**.