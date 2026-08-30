# Paquete `vo` — Value Objects

> Las piezas del dominio que se definen **por su valor, no por su identidad**.
> Encapsulan un dato y las reglas que lo hacen válido, y son inmutables: una vez
> creadas, no cambian.

Una analogía: un billete de 10 € es un *value object*. No importa qué billete
concreto tengas en la mano; dos billetes de 10 € valen exactamente lo mismo y son
intercambiables. Lo que importa es el valor, no la pieza. (La excepción es `Id`,
que precisamente **es** la identidad de una entity.)

## ¿Qué se implementó?

Los valores con los que se construye todo el dominio.

| Pieza | Qué es | Ejemplos |
|---|---|---|
| **Identidad** | Envuelve un `UUID` como identidad de una entity. | `Id` |
| **Valores de red y ubicación** | Datos compuestos con sus reglas de formato. | `IP`, `Network`, `Location` |
| **Enumeraciones** | Conjuntos cerrados de valores permitidos. | `Vendor`, `Model`, `Protocol`, `RouterType`, `SwitchType` |

## ¿Por qué se implementó así?

- **Para que un valor inválido no pueda existir.** La validación ocurre al
  construir (una `IP` mal formada o una `Network` con CIDR fuera de rango no llegan
  a crearse), así que el resto del dominio confía en que todo valor es correcto.
- **Para poder compartirlos sin miedo.** Al ser inmutables, se pueden pasar entre
  entities y servicios sin riesgo de que alguien los altere por sorpresa.
- **Para razonar por valor.** Dos value objects con los mismos datos son
  equivalentes; eso simplifica comparaciones y evita identidades accidentales.

## ¿Cómo se implementó?

- Como clases y enums **inmutables**, con `@Getter`/`@Builder` de Lombok para
  reducir código repetitivo.
- Con **fábricas estáticas** que expresan la intención (`Id.withId(...)`,
  `Id.withoutId()`, `IP.fromAddress(...)`).
- Con **validación en el constructor**: las reglas de formato viven junto al dato
  que protegen.

## ¿Cuál es su responsabilidad?

**Representar valores con sus reglas.** Ser datos correctos por construcción.

| Sí es responsabilidad de `vo` | NO es responsabilidad de `vo` |
|---|---|
| Encapsular un valor y validar su formato | Tener ciclo de vida o identidad mutable (eso son las entities) |
| Ofrecer igualdad por valor | Orquestar operaciones del sistema |
| Exponer fábricas claras de creación | Saber de persistencia o infraestructura |

## Estado actual

Implementado y con pruebas. Es la base sobre la que se construyen las entities, los
servicios y las especificaciones.

## ¿Cómo se relaciona con el proyecto?

Son los ladrillos más básicos del núcleo:

```
        vo  ──▶  entity / service / specification
   (este paquete)         (lo usan todos)
```

- Las **entities** se componen de value objects (un router tiene una `IP`, una
  `Location`, un `Id`).
- El **framework** los traduce a sus espejos de persistencia (`IPData`, etc.).

> Para el propósito y el estado del módulo completo, consulta el
> **README del módulo `domain`**.