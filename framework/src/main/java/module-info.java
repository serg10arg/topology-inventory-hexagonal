/**
 * Descriptor del módulo del Framework hexagon.
 *
 * Es el hexágono más externo: aquí se ensamblan los adapters que conectan el sistema con
 * el mundo real. Requiere 'domain' y 'application' porque consume las entities y value
 * objects del dominio y los use cases y ports de la aplicación. Las flechas de
 * dependencia apuntan siempre hacia dentro.
 *
 * Cláusulas de persistencia (lado de salida):
 * - 'requires jakarta.persistence': la API JPA que anotan las entidades *Data y el tipo
 *   EntityManager que el output adapter recibe por '@Inject'. El proveedor (Hibernate ORM)
 *   lo aporta Quarkus en tiempo de ejecución.
 * - 'opens ...output.h2.data': Hibernate accede por reflexión a los campos privados de las
 *   entidades; sin 'opens' fallaría en runtime.
 * - 'exports ...output.h2.data': hace visibles los tipos de persistencia a otros módulos
 *   del reactor (p. ej. los tests de integración).
 *
 * Anotaciones CDI (SPI estándar de Jakarta, no APIs de Quarkus):
 * - 'requires jakarta.cdi': '@ApplicationScoped' e '@Inject', con las que los adapters y el
 *   output adapter pasan a ser beans gestionados por Arc.
 * - 'requires jakarta.transaction': '@Transactional', con el que el output adapter delega la
 *   demarcación de la transacción en el contenedor.
 * Al usar módulos JPMS estables de Jakarta (no Arc ni QuarkusTransaction), este descriptor
 * no 'requires' ningún módulo Quarkus: el acoplamiento a Quarkus vive solo en la resolución
 * de beans en runtime, no en el descriptor.
 *
 * Inyección de dependencias (CDI): el output adapter (RouterManagementH2Adapter) es el bean
 * que implementa RouterManagementOutputPort; Arc lo descubre por Jandex y lo inyecta donde
 * application declara '@Inject'. Se retiró la cláusula 'provides ... with ...' (y su binding
 * gemelo META-INF/services): la resolución del puerto de salida ya no la hace ServiceLoader,
 * sino el contenedor CDI.
 *
 * Costura JAX-RS (lado de entrada):
 * - 'requires jakarta.ws.rs': las anotaciones del recurso REST ('@Path', '@GET', '@POST',
 *   '@PathParam') y el tipo Response. Es de nuevo SPI estándar de Jakarta, no una API de
 *   Quarkus: el descriptor sigue sin 'requires' de ningún módulo Quarkus.
 * - 'requires io.smallrye.mutiny': el tipo Uni que devuelven los endpoints (modelo reactivo
 *   de RESTEasy Reactive).
 * - 'requires io.smallrye.common.annotation': '@Blocking', con el que los endpoints se
 *   despachan a un worker thread en lugar del event loop, porque la persistencia por debajo
 *   (Hibernate ORM clásico) es bloqueante.
 * Los dos últimos son módulos automáticos de SmallRye: no son API de Jakarta, pero tampoco
 * de Quarkus, y son la frontera mínima para expresar "reactivo con trabajo bloqueante".
 * - 'requires org.eclipse.microprofile.openapi': '@Tag' y '@Operation', con las que los adapters
 *   publican su contrato (tags y operationId) en el documento OpenAPI. Es API de MicroProfile,
 *   estándar como las anteriores: el descriptor sigue sin conocer Quarkus.
 *
 * No se abren los paquetes de DTOs ('input.rest.request'/'response') a la reflexión de
 * Jackson: en runtime la aplicación corre en classpath plano (fast-jar y '@QuarkusTest' con
 * useModulePath=false), donde el encapsulamiento de JPMS no aplica. Es el mismo motivo por el
 * que Arc no necesitó 'opens' para sus proxies en la Fase 6.
 */
module framework {
    requires domain;
    requires application;
    requires static lombok;
    requires jakarta.persistence;
    requires jakarta.cdi;
    requires jakarta.transaction;
    requires jakarta.ws.rs;
    requires io.smallrye.mutiny;
    requires io.smallrye.common.annotation;
    requires org.eclipse.microprofile.openapi;

    exports com.example.topologyinventory.framework.adapters.output.h2.data;
    opens com.example.topologyinventory.framework.adapters.output.h2.data;

    exports com.example.topologyinventory.framework.adapters.input.generic;
}
