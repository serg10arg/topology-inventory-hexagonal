# PROJECT_LOG

Bitácora de desarrollo incremental del proyecto. Cada fase deja un **bloque de
handoff** con lo entregado, las decisiones tomadas y el punto de partida de la
siguiente. Junto con la tabla *Estado del proyecto* del README raíz y el historial
de Git, es el portador autorizado del estado entre sesiones de trabajo.

---

## Fase 1 · Modelo de dominio (hexágono de dominio)

- **Estado:** ✅ completada
- **Entregado:** entities (`Equipment`, `Router`, `CoreRouter`, `EdgeRouter`,
  `Switch`), value objects (`Id`, `IP`, `Location`, `Network` y enums),
  `RouterFactory`, servicios de dominio y el patrón Specification (reglas de
  negocio + base común `shared`). `module-info` del módulo `domain` con sus
  `exports`. Pruebas unitarias del dominio.
- **Siguiente:** Fase 2 · Casos de uso y puertos.

---

## Fase 2 · Casos de uso y puertos (hexágono de aplicación)

- **Estado:** ✅ completada
- **Entregado:** casos de uso (`RouterManagementUseCase`,
  `SwitchManagementUseCase`, `NetworkManagementUseCase`) y sus application services
  (input ports); puerto de salida `RouterManagementOutputPort` (persistencia del
  agregado router); `module-info` del módulo `application` (`requires domain`);
  suite de aceptación con Cucumber sobre JUnit 5 Platform; READMEs de módulo.
- **Decisiones:** `RouterFactory` como única vía de construcción por tipo; el
  puerto de salida queda declarado como la costura que rellenará el framework;
  `cucumber-junit-platform-engine` sobre JUnit 5; versiones de JUnit centralizadas
  vía `junit-bom` en el POM padre.
- **Siguiente:** Fase 3 · Adapters y frontera tecnológica.

---

## Fase 3 · Adapters y frontera tecnológica (hexágono de framework)

- **Estado:** ✅ completada — implementada aquí; su verificación funcional se consolidó en la Fase 4.
- **Entregado:**
    - **Bootstrap** del módulo `framework` (Maven + JPMS): `requires domain,
    application` y la frontera tecnológica JPA/H2.
    - **Lado de salida (driven):** `RouterManagementH2Adapter` (implementa
      `RouterManagementOutputPort` con `retrieve`/`persist`); modelo de persistencia
      (`RouterData`, `SwitchData`, `NetworkData`, `LocationData`, `IPData`, los enums
      `*Data` y `UUIDTypeConverter`); `RouterH2Mapper`; `persistence.xml` (Jakarta
      Persistence 3.1, proveedor EclipseLink) e `inventory.sql`.
    - **Lado de entrada (driving):** adapters genéricos
      `RouterManagementGenericAdapter`, `SwitchManagementGenericAdapter` y
      `NetworkManagementGenericAdapter` (POJOs que delegan en los casos de uso).
    - **JPMS:** `module-info` del framework con las `requires` de persistencia y el
      `exports`/`opens` del paquete de entidades; `exports` de `usecases`,
      `ports.input` y `ports.output` añadidos al módulo `application`.
    - **Documentación:** READMEs del módulo `framework` y de los paquetes `data`,
      `mappers`, `input/generic`; además de los seis paquetes del módulo `domain`
      (`entity`, `factory`, `service`, `specification`, `shared`, `vo`).
- **Decisiones:**
    - Proveedor JPA: **EclipseLink** (no Hibernate; Hibernate llegará con la
      integración cloud-native), sobre **H2 2.x** en memoria.
    - **El framework se adapta a la API real del núcleo, no al revés:** `Id.getId()`;
      reconstrucción de agregados con sus builders (las entities del dominio son
      inmutables, sin setters); el `routerId` del switch lo aporta el edge router
      padre al persistir.
    - **Sin `SwitchManagementOutputPort`** ni recuperación independiente de switches:
      la persistencia fluye por el agregado router.
    - **Cableado del output port diferido a la Fase 4** (JPMS `provides/uses`): hasta
      entonces los generic adapters solo ejercitan crear, conectar y desconectar;
      `retrieve`/`persist` quedan declarados pero inactivos.
    - Modernizaciones: `persistence.xml` a Jakarta Persistence 3.1; limpieza de DDL
      para H2 2.x (sin PK nula, duplicada ni auto-referida); versión de Lombok
      centralizada en el POM padre.
- **Verificación:** compila en verde (`mvn -pl framework -am compile` →
  `BUILD SUCCESS`). **Aún sin pruebas propias del framework:** el test de
  integración del output adapter (round-trip contra H2) y los tests end-to-end de
  los generic adapters se realizan en la Fase 4, tras el cableado JPMS. Ahí se
  ejercitarán los riesgos de runtime pendientes (`@Entity`+`@MappedSuperclass`,
  *weaving* de EclipseLink bajo JPMS, transacciones en `persist`).
- **Siguiente:** Fase 4 · Inversión de dependencias entre módulos — cableado JPMS
  `provides/uses` que activa `retrieve`/`persist`, y con él la suite de pruebas del
  framework (integración del output adapter + end-to-end de los generic adapters).

---

## Fase 4 · Inversión de dependencias entre módulos (JPMS `provides`/`uses`)

- **Estado:** ✅ completada
- **Entregado:**
    - **Cableado del puerto de salida sin acoplar el núcleo:** `application`
      declara `uses RouterManagementOutputPort` y `RouterManagementInputPort` lo
      resuelve con `ServiceLoader` de forma **perezosa** (solo al persistir o
      recuperar); `framework` declara el `provides ... with RouterManagementH2Adapter`,
      cuyo método `provider()` preserva el singleton. `application` sigue **sin**
      `requires framework`: la flecha de dependencia nunca se invierte.
    - **Transacciones reales:** `persistRouter` envuelve `em.persist` en una
      transacción `RESOURCE_LOCAL` (begin/commit/rollback); sin ella no confirmaba.
    - **Módulo `bootstrap`** (raíz de composición): único módulo que conoce a los
      tres hexágonos y los ensambla; su `requires framework` es lo que mete al
      proveedor en el grafo de módulos en ejecución. `Application::main` recorre el
      flujo de punta a punta contra H2.
    - **Pruebas del framework (5):** integración del output adapter contra H2
      (lectura de un router semilla y escritura transaccional) y **end-to-end** por
      los tres generic adapters (jerarquía core → edge → switch → red, round-trip
      de persistencia y lectura del semilla por la puerta de entrada).
- **Decisiones y hallazgos:**
    - **Resolución perezosa, no inyección por constructor:** inyectar el puerto
      habría acoplado la construcción del application service al arranque de la base
      de datos; crear, conectar y desconectar no deben pagar ese coste.
    - **`Location` es un value object también en la persistencia** → `LocationData`
      pasó de entidad con PK y `@ManyToOne` a `@Embeddable` con `@AttributeOverrides`
      (desaparece la tabla `location`). Como entidad, un router con ubicación nueva
      no se podía persistir (*new object not marked cascade PERSIST*): el desajuste
      value-object/entidad se paga en la frontera.
    - **El proveedor JPA es dueño del DDL:** `inventory.sql` se dividió en
      `schema.sql` + `data.sql`, **una sentencia por línea** porque el lector de
      scripts de EclipseLink es orientado a línea; `persistence.xml` usa
      `create-source=script` + `sql-load-script-source`, sin `INIT/RUNSCRIPT` en la URL.
    - **Desviación de dominio (consistente con la ya existente):** el constructor de
      `Switch` dejaba `switchNetworks` a `null` cuando el use case crea el switch sin
      lista, de modo que `addNetworkToSwitch` lanzaba NPE. Se inicializa a lista
      vacía mutable, **igual que `CoreRouter`/`EdgeRouter` hacen con sus mapas**. No
      es un parche del test: un switch recién creado no debía reventar al añadirle
      una red.
    - **Falso positivo evitado en las pruebas:** el output adapter es un singleton
      con un único `EntityManager`, cuya caché de primer nivel puede servir una
      lectura sin tocar disco. Por eso la lectura *real* se verifica siempre contra
      un router **semilla**, que ningún test ha escrito.
- **Verificación:** `mvn test` en verde en todo el reactor — `domain` 19,
  `application` 12 (Cucumber), `framework` 5 — y `Application::main` ejecutado sobre
  el module path persiste y recupera un router desde H2.
- **Riesgos de la Fase 3 ya despejados:** `@Entity`+`@MappedSuperclass`, arranque de
  EclipseLink bajo JPMS y confirmación de transacciones en `persist`.
- **Deuda conocida que entra en la Fase 5:** el `@OneToMany` de `RouterData` no
  cascada, así que aún no se persiste el agregado con hijos (solo routers sueltos);
  el cableado sigue siendo manual (singleton + `new` en los adapters), y es
  justamente lo que vendrá a sustituir CDI.
- **Siguiente:** Fase 5 · Integración cloud-native con Quarkus.

---

## Fase 5 · Integración cloud-native con Quarkus

- **Estado:** ✅ completada
- **Entregado:**
    - **Quarkus 3.33 (LTS) en el reactor multi-módulo:** BOM `io.quarkus.platform:quarkus-bom`
      importado en el POM raíz; `quarkus-maven-plugin` (goal `build`) que hace de
      `bootstrap` el módulo que arranca el motor; `io.smallrye:jandex-maven-plugin`
      indexando los tres hexágonos para que la augmentation descubra las entidades,
      que viven en jars separados.
    - **Proveedor JPA sustituido:** fuera EclipseLink y `persistence.xml`; dentro
      **Hibernate ORM gestionado por Quarkus** (`quarkus-hibernate-orm`, `quarkus-jdbc-h2`,
      `quarkus-agroal`), configurado en `application.properties`. El DDL lo genera
      Hibernate desde las entidades (`schema-management.strategy=drop-and-create`) y el
      seed vive en `import.sql`.
    - **Output adapter reconectado (CDI-lite):** `RouterManagementH2Adapter` es ahora
      *stateless* y obtiene el `EntityManager` gestionado y una `UserTransaction` del
      contenedor por la SPI estándar `CDI.current()`, sin convertirse en bean CDI. El
      binding del `ServiceLoader` se porta a `META-INF/services` (constructor público),
      que es el camino que usa Quarkus en classpath; el `provides` del `module-info` se
      conserva para el module path.
    - **Arranque bajo Quarkus:** `Application` pasa a `@QuarkusMain` + `QuarkusApplication`
      (command mode): el contenedor arranca primero y luego ejecuta el flujo crear →
      persistir → recuperar, que vuelve a funcionar de punta a punta en la aplicación
      real, no solo en tests.
    - **Pruebas migradas a `@QuarkusTest`:** los cinco tests del framework corren bajo el
      contenedor vivo, ejercitando el round-trip real contra H2.
- **Decisiones y hallazgos:**
    - **CDI-lite en vez de CDI pleno:** mantener el adapter cableado por `ServiceLoader`
      y pedirle el `EntityManager` al contenedor (no `@Inject`) deja el hexágono de
      `framework` sin requerir ningún módulo de Quarkus en su descriptor. La gestión por
      contenedor de puertos y casos de uso se aborda en una fase posterior.
    - **Entidades hechas estrictas para Hibernate** (EclipseLink las toleraba): sin
      `@MappedSuperclass`; UUID nativo de H2 (eliminado `UUIDTypeConverter`); enums con
      `@Enumerated` (retirados `@Embeddable` sobre enums y `@Embedded` sobre campos enum);
      `@OneToMany` en solo lectura compartiendo la FK escalar ya mapeada, en vez del
      `@JoinTable` autorreferente que colisionaba con la tabla de la entidad; `NetworkData`
      con clave `IDENTITY`.
    - **La costura JPMS↔Quarkus existe, pero es del arnés de test, no del código.** El
      `module-info` de `framework` no conoce Quarkus; solo `bootstrap` `requires quarkus.core`.
      El fast-jar no sufre fricción porque no hay module path en runtime.
    - **`import.sql` no se ejecuta en perfil `prod` por defecto:** se fija
      `quarkus.hibernate-orm.sql-load-script=import.sql` explícito para que el seed cargue
      también fuera de dev/test.
- **Modernizaciones y desviaciones** (respecto al enfoque de referencia, con evidencia):

    | # | Decisión | Motivo | Evidencia |
    |---|----------|--------|-----------|
    | 1 | `quarkus-bom` (no `quarkus-universe-bom`); `io.smallrye` jandex; `fast-jar` por defecto | El universe BOM y el jandex de JBoss se discontinuaron en Quarkus 3.x | `mvn package` verde; feature `[cdi]` |
    | 2 | Swap de proveedor adelantado a esta fase: EclipseLink → Hibernate ORM gestionado por Quarkus | Objetivo de la fase: `persistence.xml` → `application.properties` | features `[hibernate-orm, jdbc-h2, agroal]` |
    | 3 | DDL propiedad de Hibernate (`drop-and-create`) + `import.sql`, retirando `schema.sql` a mano | Dueño único del esquema, idiomático en Quarkus | `create table routers/switches/networks` en el log de arranque |
    | 4 | UUID nativo, enums con `@Enumerated`, `@OneToMany` read-only sobre FK escalar | Hibernate rechaza combinaciones que EclipseLink toleraba | Augmentation sin errores de metamodelo; 3 FK y ninguna tabla de join |
    | 5 | EntityManager por SPI estándar `CDI.current()` + `UserTransaction`, no `@Inject`/`@Transactional` | Adapter no-bean; hexágono sin `requires quarkus.*` | `framework` no requiere Quarkus; round-trip verde |
    | 6 | `ServiceLoader` portado a `META-INF/services` (+ constructor público, adapter stateless) | Quarkus resuelve por classpath, donde `module-info provides` no aplica | El mismo binding lo usan `@QuarkusTest` y el fast-jar |
    | 7 | `framework`: `junit 6.0.3` y surefire `useModulePath=false` | `quarkus-junit5 3.33` corre sobre JUnit 6; surefire modular duplicaba el proveedor cruzando classloaders | Los 5 tests del framework en verde; Cucumber (JUnit 5) intacto |

- **Verificación:** `mvn clean package` en verde en todo el reactor — `domain` 19,
  `application` 12 (Cucumber), `framework` 5 (`@QuarkusTest`, 0 skipped) —; el
  `quarkus-run.jar` arranca en command mode, ejecuta crear → persistir → recuperar
  contra H2 y termina con código 0.
- **Deuda conocida que entra en la siguiente fase:** el cableado sigue siendo
  `ServiceLoader` + `new` en los adapters (la gestión por CDI de puertos y casos de uso
  es lo que viene); el `@OneToMany` de `RouterData` sigue sin cascade, así que aún no se
  persiste el agregado con hijos.
- **Siguiente:** Fase 6 · Gestión del ciclo de vida con CDI.
