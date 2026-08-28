# `resources/.../application/routers` — Escenarios de routers

> Los **escenarios en lenguaje natural** que describen qué se puede hacer con los
> routers: crearlos, conectarlos y desconectarlos.

Estos ficheros no son código: son la especificación del comportamiento esperado,
escrita para que cualquiera —técnico o no— pueda leerla y entender qué hace el
sistema con los routers. Cada frase de un escenario tiene su contraparte en una
clase de la carpeta gemela en `java`, que es la que lo ejecuta de verdad.

## ¿Qué se implementó?

| Fichero de escenarios | Qué describe |
|---|---|
| `RouterCreate.feature` | Crear un core router y crear un edge router. |
| `RouterAdd.feature` | Conectar un edge router a un core router, y un core router a otro core router. |
| `RouterRemove.feature` | Desconectar un edge router de un core router, y un core router de otro. |

## ¿Por qué se implementó así?

- **Para fijar el comportamiento esperado en lenguaje claro**, antes y por encima
  de cualquier detalle de implementación.
- **Para servir de documentación viva**: describen las reglas de conexión entre
  routers de una forma que no envejece cuando cambia el código.

## ¿Cómo se implementó?

- En formato **Gherkin** (`.feature`), con la estructura "Dado / Cuando / Entonces"
  que da a cada escenario una preparación, una acción y una comprobación.
- Cada fichero se empareja con una clase de step definitions del mismo nombre en la
  carpeta `java`, que traduce sus frases a llamadas reales al sistema.

## ¿Cuál es su responsabilidad?

| Sí es responsabilidad de esta carpeta | NO es responsabilidad de esta carpeta |
|---|---|
| Describir, en lenguaje natural, las operaciones sobre routers | Contener código o comprobaciones técnicas (eso vive en `java`) |
| Ser la fuente de verdad legible del comportamiento esperado | Definir las reglas de negocio (viven en `domain`) |

## Estado actual

Los **6 escenarios** de routers (2 de crear, 2 de conectar, 2 de desconectar) se
ejecutan en verde.

## ¿Cómo se relaciona con el proyecto?

Cada `.feature` de aquí tiene su clase gemela en `java/.../application`, que lo
ejecuta contra los casos de uso de routers del hexágono de aplicación. Es una de
las tres áreas de la suite de comportamiento, junto a `switches` y `networks`.

> Para la visión global del proyecto (arquitectura completa, stack y estado),
> consulta el **README raíz** del repositorio.