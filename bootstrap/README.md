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
| **Punto de entrada Quarkus** | La clase `@QuarkusMain` que arranca el contenedor y ejecuta el flujo de punta a punta. | `Application` (`implements QuarkusApplication`) |
| **Descriptor del módulo** | Declara la dependencia con los tres hexágonos, la SPI de inyección y el runtime de Quarkus. | `module-info.java` (`requires domain, application, framework, jakarta.inject, quarkus.core`) |
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
  `framework`, `jakarta.inject` y `quarkus.core`. `framework` aporta transitivamente
  Hibernate ORM y H2 en runtime.
- La clase `Application`, anotada `@QuarkusMain` e implementando `QuarkusApplication`,
  ejecuta su lógica en `run(...)` **después** de que Quarkus arranque el contenedor.
  Se ejecuta en **command mode**: realiza el flujo (crea y persiste un router contra
  H2, lo recupera, y conecta un edge en memoria) y termina con código 0.
- El cableado es **CDI de punta a punta**: `Application` recibe el generic adapter
  por `@Inject` (como bean gestionado en command mode), que a su vez inyecta el caso
  de uso, que inyecta el output adapter, que recibe su `EntityManager` por `@Inject`.
  No queda ningún `new` de colaboradores ni `ServiceLoader` en la cadena.

| Tecnología | Rol en el módulo |
|---|---|
| Java 21 | Lenguaje base |
| JPMS (Java Modules) | Declara la dependencia con los tres hexágonos, `jakarta.inject` y `quarkus.core`; cierra el grafo de módulos |
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

Implementado y operativo bajo Quarkus. `Application` arranca el contenedor y, en
command mode, ejecuta el flujo de punta a punta contra H2: Arc inyecta la cadena de
beans (generic adapter → caso de uso → output adapter → `EntityManager`), el router
persistido se recupera con sus datos, y el proceso termina con código 0. La base H2
es en memoria y efímera (demostración del flujo, no un almacén persistente).

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
