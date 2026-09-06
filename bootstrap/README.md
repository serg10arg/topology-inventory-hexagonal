# Módulo `bootstrap` — Raíz de composición y arranque Quarkus

> La capa que **ensambla y arranca**. Es el único módulo que conoce a los tres
> hexágonos a la vez y los une en una aplicación ejecutable, y el único que enciende
> el motor cloud-native (Quarkus). No contiene lógica de negocio ni tecnología de
> persistencia: solo compone y da al botón de encendido.

Una analogía: si `domain` es el reglamento, `application` el gestor y `framework`
la sala de máquinas, este módulo es **el día de la inauguración**. Alguien enciende
las luces (arranca el contenedor), conecta la recepción con el almacén y procesa el
primer pedido real de principio a fin. Cuando ese primer pedido cruza el sistema
entero sin fricción, la empresa está abierta.

## ¿Qué se implementó?

El punto de entrada bajo Quarkus, el ensamblado de los hexágonos y la configuración
de runtime de la persistencia.

| Pieza | Qué es | Ejemplos |
|---|---|---|
| **Punto de entrada Quarkus** | La clase `@QuarkusMain` que arranca el contenedor y lo mantiene sirviendo HTTP. | `Application` (`main` → `Quarkus.run(args)`) |
| **Descriptor del módulo** | Declara la dependencia con los tres hexágonos y el runtime de Quarkus. | `module-info.java` (`requires domain, application, framework, quarkus.core`) |
| **Configuración de runtime** | Datasource H2 + Hibernate ORM y seed de la base. | `application.properties`, `import.sql` |

## ¿Por qué se implementó así?

- **Para tener una raíz de composición fuera del negocio.** Ensamblar exige conocer
  todas las piezas; si esa dependencia "hacia todo" viviera dentro de un hexágono,
  ese hexágono perdería su frontera. Aislarla aquí mantiene a cada uno con su
  responsabilidad única.
- **Para concentrar la costura con Quarkus en la capa más externa.** `bootstrap` es
  el único módulo que `requires quarkus.core`: al ser el que arranca el motor, es el
  único que conoce el framework de ejecución. Los tres hexágonos internos no dependen
  de Quarkus —solo de la SPI estándar de Jakarta—, preservando la dirección de
  dependencias hacia adentro.
- **Para demostrar el sistema de verdad, con el contenedor vivo.** El flujo recorre
  entrada → caso de uso → dominio → salida (persistencia gestionada por Quarkus),
  probando que el ensamblado funciona como aplicación real, no solo como piezas en
  un test.

## ¿Cómo se implementó?

- Como un **módulo Java** (JPMS) que `requires` a `domain`, `application`,
  `framework` y `quarkus.core`. `framework` aporta transitivamente Hibernate ORM, H2 y
  RESTEasy Reactive en runtime.
- La clase `Application`, anotada `@QuarkusMain`, arranca el contenedor con
  `Quarkus.run(args)` en su `main`: en **server mode**, Quarkus enciende Arc, el
  datasource, Hibernate, el seed y el servidor HTTP de RESTEasy Reactive, y **bloquea
  sirviendo** hasta el shutdown. La app dejó de ser un programa que corre y termina.
- El cableado es **CDI de punta a punta**, resuelto por Arc bajo demanda en cada
  petición: el REST adapter inyecta el caso de uso, que inyecta el output adapter, que
  recibe su `EntityManager` por `@Inject`. No queda ningún `new` de colaboradores ni
  `ServiceLoader` en la cadena; `Application` ya no inyecta nada (por eso `bootstrap`
  dejó de `requires jakarta.inject`).

| Tecnología | Rol en el módulo |
|---|---|
| Java 21 | Lenguaje base |
| JPMS (Java Modules) | Declara la dependencia con los tres hexágonos y `quarkus.core`; cierra el grafo de módulos |
| Quarkus 3.33 | Runtime cloud-native: arranca el contenedor (Arc), gestiona la inyección y la persistencia |
| Hibernate ORM (vía Quarkus) | Proveedor JPA configurado en `application.properties` |
| Maven | Construcción multi-módulo; `quarkus-maven-plugin` produce la app |

## ¿Cuál es su responsabilidad?

**Componer y arrancar.** Reunir los hexágonos ya construidos y ponerlos en marcha
sobre Quarkus, sin añadir reglas ni tecnología de negocio propias.

| Sí es responsabilidad del bootstrap | NO es responsabilidad del bootstrap |
|---|---|
| Ensamblar los adapters y arrancar la app bajo Quarkus | Contener reglas de negocio (viven en `domain`) |
| Conocer a los tres hexágonos a la vez | Orquestar casos de uso (lo hace `application`) |
| Ser el punto de entrada (`@QuarkusMain`) | Implementar tecnología concreta (lo hace `framework`) |
| Configurar el runtime (datasource, seed) y cerrar el grafo de módulos | Definir puertos o adapters |

## Estado actual

Implementado y operativo bajo Quarkus en **server mode**. `Application` arranca el
contenedor con `Quarkus.run` y se mantiene escuchando en el puerto 8080: Arc cablea la
cadena de beans (REST adapter → caso de uso → output adapter → `EntityManager`) bajo
demanda para atender cada petición. El contrato de la API está en `/q/openapi` y la UI
en `/q/swagger-ui/`. La base H2 es en memoria y vive mientras viva el proceso.

## ¿Cómo se relaciona con el proyecto?

Se sitúa por encima de los tres hexágonos; es el único que los conoce a todos y el
único que enciende el motor:

```
        bootstrap  ─▶  framework  ─▶  application  ─▶  domain
      (arranque       (tecnología)   (coordinación)    (núcleo)
       + Quarkus)
```

- `requires` a los tres hexágonos y a `quarkus.core`, y **ensambla** sus adapters.
- Al arrancar el contenedor, **Arc descubre e inyecta** la cadena de beans; el
  output adapter recibe su `EntityManager` gestionado por `@Inject`.

> Para la visión global del proyecto (arquitectura completa, stack y estado),
> consulta el **README raíz** del repositorio.
