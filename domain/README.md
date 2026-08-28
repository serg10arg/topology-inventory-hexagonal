# Módulo `domain` — Hexágono de Dominio

> El **núcleo** del sistema. Contiene el modelo de la red y las reglas de negocio
> que la gobiernan, sin depender de ninguna tecnología externa (ni bases de datos,
> ni web, ni frameworks).

Si tuvieras que quedarte con una sola idea: este módulo es **el reglamento y el
inventario** de la red. Sabe qué es un router, qué es un switch, qué condiciones
deben cumplirse para conectarlos… pero no sabe nada de cómo se guardan los datos
ni de cómo llega una petición desde fuera. Esa ignorancia es intencionada y es su
mayor virtud.

## ¿Qué se implementó?

Una representación fiel del mundo real de una red y de las normas que la ordenan.
Se organiza en cuatro tipos de piezas:

| Pieza | Qué representa | Ejemplos |
|---|---|---|
| **Objetos de valor** | Datos sin identidad propia: valen por su contenido. | `IP`, `Network`, `Location`, `Id`, `Vendor`, `Model` |
| **Entidades** | Cosas con identidad y ciclo de vida: nacen, cambian y pueden desaparecer. | `Router` (core y edge), `Switch`, `Equipment` |
| **Reglas de negocio** | Condiciones que deben cumplirse para permitir una operación. | «mismo país», «IP única», «cupo de redes», «no borrar algo con dependencias» |
| **Servicios de dominio** | Operaciones que no pertenecen a una sola entidad. | `RouterService`, `SwitchService`, `NetworkService` |

## ¿Por qué se implementó así?

- **Para proteger lo importante del ruido técnico.** Las reglas de la red (qué se
  puede conectar y qué no) son lo que de verdad define el negocio. Si mañana se
  cambia la base de datos o el framework, este reglamento no debería tener que
  tocarse.
- **Para que sea fácil de leer y de probar.** Al no depender de ninguna
  tecnología externa, cualquier persona puede entender las reglas leyendo el
  código, y se pueden verificar de forma aislada, sin arrancar servidores ni
  bases de datos.
- **Porque es la parte más estable.** Lo que significa "una red bien formada"
  cambia mucho menos que la tecnología que la rodea.

## ¿Cómo se implementó?

- Como un **módulo Java independiente** (usando el sistema de módulos de Java,
  JPMS) que **no depende de ningún otro módulo** del proyecto.
- Siguiendo bloques de **Diseño Guiado por el Dominio (DDD)**: la distinción
  entre objetos de valor, entidades y servicios no es capricho, sino el
  vocabulario estándar para modelar un negocio con claridad.
- Cada regla se modela como una **especificación**: una clase pequeña e
  independiente que responde a una pregunta de sí/no («¿se cumple esta
  condición?») y que, cuando no se cumple, **impide** la operación. Es como el
  portero de un local: comprueba una condición concreta y, si no se cumple, no
  deja pasar.

| Tecnología | Rol en el módulo |
|---|---|
| Java 21 | Lenguaje base |
| JPMS (Java Modules) | Aísla el dominio del resto del sistema |
| Lombok | Reduce código repetitivo (constructores, getters) |

## ¿Cuál es su responsabilidad?

Ser la **única fuente de verdad** del negocio: qué existe, cómo se relaciona y
qué está permitido. Deliberadamente **no** se ocupa de la parte técnica.

| Sí es responsabilidad del dominio | NO es responsabilidad del dominio |
|---|---|
| Definir qué es un router, un switch o una red | Guardar o leer datos de una base de datos |
| Hacer cumplir las reglas de conexión y borrado | Atender peticiones web o exponer una API |
| Modelar la identidad y el ciclo de vida de los equipos | Dibujar interfaces de usuario |
| — | Depender de un framework o de configuración de infraestructura |

## Estado actual

El modelo del negocio está implementado y el módulo **compila y se prueba de
forma aislada**, sin depender de ningún otro módulo: los equipos (routers y
switches), las redes y todas las reglas de conexión y borrado están en su sitio.
Al ser el núcleo estable del sistema, es el módulo que menos cambiará a medida
que el proyecto crezca.

## ¿Cómo se relaciona con el proyecto?

Es el **hexágono interior**. La regla de oro de esta arquitectura es que las
dependencias apuntan **hacia dentro**: todo depende del dominio, y el dominio no
depende de nada.

```
        framework  ─▶  application  ─▶  domain
      (mundo exterior)  (orquestación)   (núcleo)   ◀── este módulo
```

- El módulo **`application`** usa este dominio para llevar a cabo las operaciones
  que pide el usuario (crear un router, conectar un switch…).
- El futuro módulo **`framework`** conectará el dominio con el mundo exterior
  (base de datos, API), sin que el dominio se entere de esos detalles.

> Para la visión global del proyecto (arquitectura completa, stack y estado),
> consulta el **README raíz** del repositorio.