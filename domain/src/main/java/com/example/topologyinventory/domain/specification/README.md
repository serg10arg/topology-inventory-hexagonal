# Paquete `specification` — Reglas de negocio como especificaciones

> Cada regla que decide si una conexión o una eliminación está permitida,
> encapsulada como un objeto con nombre que sabe responder "esto se cumple / esto
> no". Reglas explícitas, testeables y combinables.

Una analogía: el **portero de la discoteca**. Cada portero aplica una norma
concreta —"¿está en el mismo país?", "¿esta IP ya está en uso?", "¿queda hueco
para otra red?"— y decide si se pasa o no. Una *specification* es exactamente eso:
un portero con una norma clara.

## ¿Qué se implementó?

Ocho reglas del dominio, agrupables por lo que protegen.

| Grupo | Especificaciones | Qué protegen |
|---|---|---|
| **Conexión** | `SameCountrySpec`, `SameIpSpec`, `CIDRSpecification`, `NetworkAmountSpec`, `NetworkAvailabilitySpec` | Que un equipo o una red solo se conecten cuando cumplen las condiciones (mismo país, IP no repetida, CIDR válido, cupo y disponibilidad de red). |
| **Eliminación segura** | `EmptyRouterSpec`, `EmptySwitchSpec`, `EmptyNetworkSpec` | Que no se elimine un nodo que aún tiene elementos conectados (evitar huérfanos). |

## ¿Por qué se implementó así?

- **Para sacar las reglas de dentro de los `if`.** Una regla escondida en una
  condición es difícil de nombrar y de probar. Como objeto, tiene nombre, se testea
  aislada y se reutiliza.
- **Para poder combinarlas.** Reglas simples se encadenan en reglas compuestas sin
  reescribir nada (ver el paquete `shared`).
- **Para aplicarlas como guarda.** Una entity ejecuta la regla *antes* de mutar: si
  no se cumple, la operación no ocurre.

## ¿Cómo se implementó?

- Cada regla **extiende `AbstractSpecification`** (paquete `shared`) y ofrece dos
  formas de uso: `isSatisfiedBy(...)` (devuelve un booleano) y `check(...)` (lanza
  una excepción si la regla no se cumple).
- Las entities llaman a `check(...)` como guarda antes de conectar o eliminar.

## ¿Cuál es su responsabilidad?

**Expresar y evaluar una regla de negocio.** Decir si algo cumple, y señalar cuándo
no.

| Sí es responsabilidad de `specification` | NO es responsabilidad de `specification` |
|---|---|
| Encapsular una regla de conexión/eliminación | Mutar entities (solo las evalúa) |
| Evaluarla y reportar el incumplimiento | Orquestar operaciones (eso es `application`) |
| Ser testeable de forma aislada | Conocer la persistencia |

## Estado actual

Las ocho especificaciones están implementadas y cubiertas por pruebas.

## ¿Cómo se relaciona con el proyecto?

Es el guardián de las reglas del núcleo:

```
   entity  ──(check antes de mutar)──▶  specification  ──▶  shared (base común)
```

- Las **entities** las usan como guardas; todas se apoyan en la base del paquete
  **`shared`**.

> Para el propósito y el estado del módulo completo, consulta el
> **README del módulo `domain`**.