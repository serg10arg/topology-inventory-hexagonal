# `src/test/java/.../application` — Suite de pruebas de comportamiento

> Aquí vive el **código de pruebas** del hexágono de aplicación: las piezas que
> conectan escenarios escritos en lenguaje natural con llamadas reales al sistema.

Piensa en esta carpeta como el **traductor** entre dos mundos: por un lado hay
escenarios escritos en un inglés casi cotidiano ("dado un router de borde… cuando
lo conecto… entonces…"); por otro, el código real de la aplicación. Las clases de
aquí enganchan cada frase de esos escenarios con la operación que le corresponde,
de modo que las pruebas se leen como documentación y a la vez se ejecutan.

## ¿Qué se implementó?

| Pieza | Qué es |
|---|---|
| **Runner** (`ApplicationTest`) | El punto de arranque: descubre los escenarios y los ejecuta. |
| **Fixture compartido** (`ApplicationTestData`) | El estado de partida común (routers, switches y una red de prueba ya montados) del que parte cada escenario. |
| **Step definitions** (`RouterCreate`, `RouterAdd`, `RouterRemove`, `SwitchCreate`, `SwitchAdd`, `SwitchRemove`, `NetworkCreate`, `NetworkAdd`, `NetworkRemove`) | Las clases que dan cuerpo a cada frase de los escenarios llamando a los casos de uso. |

## ¿Por qué se implementó así?

- **Para que las pruebas documenten el comportamiento.** Cada escenario describe
  una operación del sistema en lenguaje que cualquiera puede leer, y sirve a la vez
  de especificación viva y de prueba automática.
- **Para verificar el sistema "por fuera".** Se ejercita cada operación a través de
  sus casos de uso (la puerta de entrada pública), no espiando detalles internos.
  Así las pruebas siguen siendo válidas aunque cambie la implementación interna.
- **Para partir siempre del mismo estado.** El fixture compartido evita repetir la
  preparación en cada escenario y hace las pruebas predecibles.

## ¿Cómo se implementó?

- Con **Cucumber** (escenarios en lenguaje natural) ejecutado sobre **JUnit 5**.
- Cada clase de esta carpeta se corresponde con un fichero de escenarios de la
  carpeta gemela en `resources` (misma ruta de paquete).
- El estado de partida se comparte por **herencia**: cada clase extiende el fixture
  y lo carga en su arranque.

| Tecnología | Rol en la carpeta |
|---|---|
| Cucumber | Ejecuta los escenarios escritos en lenguaje natural |
| JUnit 5 Platform | Motor sobre el que corre la suite |
| Java 21 | Lenguaje de las clases de step definitions |

## ¿Cuál es su responsabilidad?

| Sí es responsabilidad de esta carpeta | NO es responsabilidad de esta carpeta |
|---|---|
| Traducir cada escenario a llamadas reales al sistema | Contener los escenarios en sí (están en `resources`) |
| Verificar las operaciones a través de los casos de uso | Probar los detalles internos del dominio (eso es del módulo `domain`) |
| Preparar un estado de partida común y fiable | Probar la persistencia (todavía no está conectada) |

## Estado actual

Las **12 comprobaciones** (crear, conectar y desconectar routers, switches y redes)
se ejecutan en verde. Las operaciones que dependen de guardar o recuperar datos aún
no se prueban, porque esa conexión con el exterior llegará en una etapa posterior.

## ¿Cómo se relaciona con el proyecto?

Esta carpeta es la **contraparte en código** de los escenarios en lenguaje natural:

```
   resources/.../application/*.feature   ─┐  (qué debe pasar, en lenguaje natural)
                                          ├─►  casos de uso del hexágono de aplicación
   java/.../application/*.java  (steps)  ─┘  (cómo se comprueba, en código)
```

Ejercita el módulo `application` apoyándose en el módulo `domain`, verificando que
las operaciones se comportan como describen los escenarios.

> Para la visión global del proyecto (arquitectura completa, stack y estado),
> consulta el **README raíz** del repositorio.