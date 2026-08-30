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

- **Estado:** 🚧 implementada — la verificación funcional se consolida en la Fase 4.
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