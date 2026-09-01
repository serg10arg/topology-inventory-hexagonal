# Módulo `bootstrap` — Raíz de composición y arranque

> La capa que **ensambla y arranca**. Es el único módulo que conoce a los tres
> hexágonos a la vez y los une en una aplicación ejecutable. No contiene lógica de
> negocio ni tecnología: solo compone y da al botón de encendido.

Una analogía: si `domain` es el reglamento, `application` el gestor y `framework`
la sala de máquinas, este módulo es **el día de la inauguración**. Alguien contrata
a la recepción, la conecta con el almacén y procesa el primer pedido real de
principio a fin. Cuando ese primer pedido cruza el sistema entero sin fricción, la
empresa está abierta.

## ¿Qué se implementó?

El punto de entrada de la aplicación y el ensamblado de los hexágonos.

| Pieza | Qué es | Ejemplos |
|---|---|---|
| **Clase de arranque** | El `main` que construye los adapters y ejercita el flujo de punta a punta. | `Application` |
| **Descriptor del módulo** | Declara la dependencia con los tres hexágonos. | `module-info.java` (`requires domain, application, framework`) |

## ¿Por qué se implementó así?

- **Para tener una raíz de composición fuera del negocio.** Ensamblar exige conocer
  todas las piezas; si esa dependencia "hacia todo" viviera dentro de un hexágono,
  ese hexágono perdería su frontera. Aislarla aquí mantiene a cada uno con su
  responsabilidad única.
- **Para cerrar el circuito de la inversión de dependencias.** El cableado JPMS
  `provides/uses` solo **resuelve** cuando el módulo `framework` está en un grafo de
  módulos en ejecución. `requires framework` lo pone ahí, y el `ServiceLoader` del
  input port encuentra al output adapter de H2 sin que el `main` mencione jamás JPA.
- **Para demostrar el sistema de verdad.** El `main` recorre entrada → caso de uso →
  dominio → salida (persistencia), probando que el ensamblado funciona como
  aplicación, no solo como piezas en un test.

## ¿Cómo se implementó?

- Como un **módulo Java** (JPMS) que `requires` a `domain`, `application` y
  `framework`. `framework` aporta transitivamente EclipseLink y H2 en runtime.
- La clase `Application` construye los generic adapters (que se **auto-cablean** con
  su constructor sin argumentos) y dispara un flujo demostrativo: crea y persiste un
  router contra H2, lo recupera, y conecta un edge en memoria.
- Es un `main` **plano**, a propósito: el cableado se hace por `ServiceLoader`. La
  gestión del ciclo de vida con un contenedor (CDI/Quarkus) llega en una fase
  posterior.

| Tecnología | Rol en el módulo |
|---|---|
| Java 21 | Lenguaje base |
| JPMS (Java Modules) | Declara la dependencia con los tres hexágonos y cierra el grafo de módulos en ejecución |
| Maven | Construcción multi-módulo |

## ¿Cuál es su responsabilidad?

**Componer y arrancar.** Reunir los hexágonos ya construidos y ponerlos en marcha,
sin añadir reglas ni tecnología propias.

| Sí es responsabilidad del bootstrap | NO es responsabilidad del bootstrap |
|---|---|
| Ensamblar los adapters y arrancar la app | Contener reglas de negocio (viven en `domain`) |
| Conocer a los tres hexágonos a la vez | Orquestar casos de uso (lo hace `application`) |
| Ser el punto de entrada (`main`) | Implementar tecnología concreta (lo hace `framework`) |
| Cerrar el grafo de módulos en runtime | Definir puertos o adapters |

## Estado actual

Implementado y operativo. `Application::main` arranca sobre el module path y
ejecuta el flujo de punta a punta contra H2: el `ServiceLoader` resuelve el output
port fuera de un test y el router persistido se recupera con sus datos. La base H2
es en memoria y efímera (demostración del flujo, no un almacén persistente).

## ¿Cómo se relaciona con el proyecto?

Se sitúa por encima de los tres hexágonos; es el único que los conoce a todos:

```
        bootstrap  ─▶  framework  ─▶  application  ─▶  domain
      (este módulo)    (tecnología)   (coordinación)    (núcleo)
```

- `requires` a los tres hexágonos y **ensambla** sus adapters.
- Al meter `framework` en el grafo, **activa la resolución** del cableado
  `provides/uses` definido en la Fase 4.

> Para la visión global del proyecto (arquitectura completa, stack y estado),
> consulta el **README raíz** del repositorio.