# Paquete `service` — Servicios de dominio

> La lógica de dominio que **no pertenece a una sola entity**: consultas
> transversales como filtrar una colección o buscar por identidad. Son *stateless*:
> no guardan estado, solo operan sobre lo que reciben.

Una analogía: un **colador**. Le pasas una colección de elementos y un criterio (un
`Predicate`), y te quedas solo con los que cumplen. El colador no es dueño de nada:
recibe, filtra y devuelve.

## ¿Qué se implementó?

Servicios de consulta para cada tipo de elemento de la red.

| Pieza | Qué hace |
|---|---|
| `RouterService` | Filtra routers por un predicado y busca un router por su `Id`. |
| `SwitchService` | Localiza switches dentro del agregado. |
| `NetworkService` | Busca redes (p. ej. por nombre) dentro de un switch. |

## ¿Por qué se implementó así?

- **Porque hay lógica que no es de nadie en concreto.** Filtrar una lista de routers
  o buscar por id no es responsabilidad natural de *un* router; colocarla en una
  entity la ensuciaría. Un servicio *stateless* es el sitio adecuado.
- **Para mantener las entities enfocadas.** Cada entity se ocupa de su estado y sus
  reglas; las consultas transversales se delegan aquí.
- **Para reutilizar.** Los mismos filtros sirven a la aplicación y a las propias
  entities.

## ¿Cómo se implementó?

- Como clases con **métodos estáticos** y **sin estado**.
- Usando **`Stream` + `Predicate`**; los predicados suelen provenir de las fábricas
  del dominio (por tipo, modelo, país, fabricante o nombre de red).

## ¿Cuál es su responsabilidad?

**Consultar, no decidir ni mutar.** Filtrar y localizar elementos del agregado.

| Sí es responsabilidad de `service` | NO es responsabilidad de `service` |
|---|---|
| Filtrar colecciones por un criterio | Mantener estado propio |
| Buscar por identidad o por nombre | Contener las reglas de conexión (eso es `specification`) |
| Ofrecer utilidades de consulta reutilizables | Orquestar casos de uso (eso es `application`) |

## Estado actual

Implementado, incluidos los métodos de apoyo a la suite de aceptación (por ejemplo,
la búsqueda de redes por nombre).

## ¿Cómo se relaciona con el proyecto?

Es el ayudante de consulta del núcleo:

```
   entity / application  ──▶  service  ──▶  (filtra colecciones de vo/entity)
```

- Lo usan las **entities** y la **aplicación** para localizar y filtrar elementos
  sin duplicar esa lógica.

> Para el propósito y el estado del módulo completo, consulta el
> **README del módulo `domain`**.