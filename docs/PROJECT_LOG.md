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
