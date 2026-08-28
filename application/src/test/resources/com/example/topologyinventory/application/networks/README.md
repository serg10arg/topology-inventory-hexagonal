# `resources/.../application/networks` — Escenarios de redes

> Los **escenarios en lenguaje natural** que describen qué se puede hacer con las
> redes: crearlas, y añadirlas o quitarlas de un switch.

Igual que las otras dos áreas, estos ficheros son la especificación legible del
comportamiento, no código. Una red vive dentro de un switch, y estos escenarios
describen cómo se crea una red y cómo se asocia o se retira de un switch. Cada uno
tiene su clase gemela en la carpeta `java`, que lo ejecuta.

## ¿Qué se implementó?

| Fichero de escenarios | Qué describe |
|---|---|
| `NetworkCreate.feature` | Crear una red. |
| `NetworkAdd.feature` | Añadir una red a un switch. |
| `NetworkRemove.feature` | Quitar una red de un switch. |

## ¿Por qué se implementó así?

- **Para fijar el comportamiento esperado en lenguaje claro**, al margen de la
  implementación.
- **Para documentar de forma viva** la relación entre redes y switches.

## ¿Cómo se implementó?

- En formato **Gherkin** (`.feature`), con la estructura "Dado / Cuando / Entonces".
- Cada fichero se empareja con una clase de step definitions del mismo nombre en la
  carpeta `java`, que traduce sus frases a llamadas reales al sistema.

## ¿Cuál es su responsabilidad?

| Sí es responsabilidad de esta carpeta | NO es responsabilidad de esta carpeta |
|---|---|
| Describir, en lenguaje natural, las operaciones sobre redes | Contener código o comprobaciones técnicas (eso vive en `java`) |
| Ser la fuente de verdad legible del comportamiento esperado | Definir las reglas de negocio (viven en `domain`) |

## Estado actual

Los **3 escenarios** de redes (crear, añadir y quitar) se ejecutan en verde.

## ¿Cómo se relaciona con el proyecto?

Cada `.feature` de aquí tiene su clase gemela en `java/.../application`, que lo
ejecuta contra los casos de uso de redes del hexágono de aplicación. Es una de las
tres áreas de la suite de comportamiento, junto a `routers` y `switches`.

> Para la visión global del proyecto (arquitectura completa, stack y estado),
> consulta el **README raíz** del repositorio.