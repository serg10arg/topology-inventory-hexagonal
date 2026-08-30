# Paquete `specification/shared` — Base del patrón Specification

> La **infraestructura común** del patrón Specification: el contrato, la plantilla
> abstracta y el combinador lógico. Es detalle interno del dominio y **no se
> exporta** fuera del módulo.

Una analogía: si cada especificación concreta es un portero con su norma, este
paquete es el **manual y las herramientas del portero**: define qué es una norma,
da la plantilla común para escribir normas nuevas, y explica cómo encadenar dos
normas en una sola.

## ¿Qué se implementó?

Las tres piezas que sostienen a todas las especificaciones concretas.

| Pieza | Qué es |
|---|---|
| `Specification<T>` | El **contrato**: `isSatisfiedBy(t)` y la combinación `and(...)`. |
| `AbstractSpecification<T>` | La **base**: aporta `and(...)` y `check(t)` (la forma imperativa que lanza excepción). Las subclases solo definen su regla. |
| `AndSpecification<T>` | El **composite AND**: se satisface solo si sus dos reglas hijas se satisfacen. |

## ¿Por qué se implementó así?

- **Para no repetir lo común.** La combinación y la validación imperativa (`check`)
  se escriben una vez aquí; cada regla concreta solo aporta su lógica propia.
- **Para poder componer.** El `AndSpecification` permite construir reglas compuestas
  a partir de reglas simples sin tocar ninguna de ellas.
- **Para mantener el patrón cerrado.** Al no exportarse, este andamiaje es un
  detalle de implementación del dominio, no parte de su API pública.

## ¿Cómo se implementó?

- Como una **interfaz** (`Specification`), una **clase abstracta**
  (`AbstractSpecification`) y un **composite** (`AndSpecification`).
- Con **genéricos `<T>`** para que el mismo patrón sirva a cualquier tipo del
  dominio (equipos, routers, redes…).

## ¿Cuál es su responsabilidad?

**Definir el contrato y la mecánica del patrón.** Ofrecer combinación y validación
imperativa a las reglas concretas.

| Sí es responsabilidad de `shared` | NO es responsabilidad de `shared` |
|---|---|
| Definir el contrato `Specification` | Contener reglas de negocio concretas (esas viven en `specification`) |
| Aportar la combinación `and` y el `check` | Conocer entities específicas |
| Ser la base de la que heredan las reglas | Formar parte de la API pública del módulo |

## Estado actual

Implementado. Es la base de las ocho especificaciones concretas y permanece
**sin exportar** en el `module-info` del dominio (uso interno).

## ¿Cómo se relaciona con el proyecto?

Es el cimiento del que cuelgan todas las reglas:

```
   specification (8 reglas)  ──extiende──▶  shared (este paquete)
```

- De él **heredan** todas las especificaciones concretas; nada fuera del dominio lo
  ve.

> Para el propósito y el estado del módulo completo, consulta el
> **README del módulo `domain`**.