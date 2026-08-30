# Paquete `mappers` — Traducción dominio ↔ persistencia

> La **aduana** de la frontera de salida. Convierte las entidades del dominio en
> su espejo de persistencia (`*Data`) y viceversa, de modo que ningún tipo de base
> de datos entre al núcleo y ninguna entidad de dominio se guarde sin traducirse
> antes.

Una analogía: en un puesto fronterizo, un **intérprete** traduce lo que se dice a
un lado al idioma del otro, en ambos sentidos. Aquí, el dominio habla el idioma de
las reglas de negocio y la base de datos habla el idioma de las filas y columnas;
el mapper es el único que entiende ambos y hace de puente. Si desapareciera, cada
lado tendría que aprender el idioma del otro —y esa es justo la mezcla que la
arquitectura quiere evitar.

## ¿Qué se implementó?

Un traductor que cubre los dos sentidos de la conversión para el agregado del router.

| Método | Dirección | Qué hace |
|---|---|---|
| `routerDomainToData` | dominio → data | Convierte un `Router` (y su agregado) en filas listas para persistir. |
| `routerDataToDomain` | data → dominio | Reconstruye el `Router` de dominio a partir de sus filas. |
| Ayudantes de `switch`, `network`, `location` | ambos sentidos | Traducen las piezas internas del agregado (switches, redes, ubicación). |

## ¿Por qué se implementó así?

- **Para concentrar la traducción en un único punto.** Una sola aduana es más fácil
  de mantener y auditar que reglas de conversión repartidas por los adapters.
- **Para respetar la inmutabilidad del dominio.** Las entidades del dominio no
  tienen *setters*; por eso la reconstrucción se hace con sus **builders**
  (`CoreRouter`/`EdgeRouter`/`Switch`), no mutando objetos ya creados.
- **Para salvar un desajuste del modelo con elegancia.** El `Switch` del dominio no
  guarda el id de su router; al persistir, el mapper toma ese id del **edge router
  padre** (la raíz del agregado) y lo escribe en la fila del switch. Así la base de
  datos mantiene su clave foránea sin obligar al dominio a cargar con un dato que no
  necesita.

## ¿Cómo se implementó?

- Como métodos **estáticos** que reciben una entidad (o una fila) y devuelven su
  contraparte.
- Usa los **builders** del dominio y de las clases `*Data`, con **guardas de nulos**
  en las colecciones para tolerar agregados sin hijos.
- Solo depende de `domain` (entidades y value objects) y del paquete `data`; no
  conoce la base de datos ni JPA directamente.

## ¿Cuál es su responsabilidad?

**Traducir, y solo traducir.** Ser el puente fiel entre dos representaciones de la
misma información.

| Sí es responsabilidad de `mappers` | NO es responsabilidad de `mappers` |
|---|---|
| Convertir dominio → `data` y `data` → dominio | Persistir o leer en la base de datos (lo hace el adapter) |
| Reconstruir el agregado respetando sus invariantes | Contener reglas de negocio (viven en `domain`) |
| Aislar al dominio de los tipos de persistencia | Conocer H2 o la API de JPA |

## Estado actual

Implementado y verificado contra la API real del dominio. Lo usa el
`RouterManagementH2Adapter` en cada operación de recuperar y persistir.

## ¿Cómo se relaciona con el proyecto?

Es el eslabón central de la frontera de salida:

```
   output adapter  ──▶  mappers (este paquete)  ──▶  data  ──▶  H2
                              │
                              ▼
                     domain (entidades)
```

- El **output adapter** llama al mapper en cada lectura y escritura.
- El mapper traduce entre las **entidades de `domain`** y las clases de **`data`**.

> Para el propósito y el estado del módulo completo, consulta el
> **README del módulo `framework`**.