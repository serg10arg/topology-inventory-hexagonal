/**
 * Descriptor del módulo del Framework hexagon. Es el hexágono más externo: aquí se ensamblan los
 * adapters que conectan el sistema con el mundo real. Requiere 'domain' y 'application'; las
 * flechas de dependencia apuntan siempre hacia dentro.
 *
 * Cláusulas de persistencia REACTIVA (lado de salida, Fase 8):
 * - 'requires jakarta.persistence': la API JPA que anotan las entidades *Data (incluido el
 *   AttributeConverter del UUID). El proveedor (Hibernate Reactive) lo aporta Quarkus en runtime.
 * - 'requires hibernate.reactive.core': el tipo Mutiny.SessionFactory que el output adapter recibe
 *   por '@Inject' y con el que abre sesión/transacción reactivas. (Se retiró 'jakarta.transaction':
 *   @Transactional desaparece en favor de withTransaction.) OJO: a diferencia de la costura REST
 *   de la Fase 7 (mutiny, smallrye-common-annotation, microprofile-openapi, todos con
 *   module-info.class propio), hibernate-reactive-core NO trae descriptor ni declara
 *   'Automatic-Module-Name': su nombre es el DERIVADO del nombre del jar. Es el eslabón frágil de
 *   la costura: si el artefacto se renombrara, este 'requires' dejaría de resolver.
 * - 'opens ...output.h2.data': Hibernate accede por reflexión a los campos privados de las
 *   entidades; sin 'opens' fallaría en runtime.
 * - 'exports ...output.h2.data': hace visibles los tipos de persistencia a los tests de integración.
 *
 * Anotaciones CDI (SPI estándar de Jakarta): 'requires jakarta.cdi' para '@ApplicationScoped' e
 * '@Inject'. Este descriptor no 'requires' ningún módulo Quarkus: el acoplamiento a Quarkus vive
 * solo en la resolución de beans en runtime.
 *
 * Costura JAX-RS (lado de entrada):
 * - 'requires jakarta.ws.rs': '@Path', '@GET', '@POST', '@PathParam', Response.
 * - 'requires io.smallrye.mutiny': el tipo Uni que devuelven los endpoints y el output port, y los
 *   operadores de composición reactiva. (Se retiró 'io.smallrye.common.annotation': al hacerse
 *   reactiva la salida ya no hay '@Blocking' en ningún endpoint.)
 * - 'requires org.eclipse.microprofile.openapi': '@Tag' y '@Operation' para el contrato OpenAPI.
 *
 * No se abren los paquetes de DTOs a la reflexión de Jackson: en runtime la app corre en classpath
 * plano (fast-jar y '@QuarkusTest' con useModulePath=false), donde el encapsulamiento JPMS no aplica.
 */
module framework {
    requires domain;
    requires application;
    requires static lombok;
    requires jakarta.persistence;
    requires jakarta.cdi;
    requires jakarta.ws.rs;
    requires io.smallrye.mutiny;
    requires hibernate.reactive.core;
    requires org.eclipse.microprofile.openapi;

    exports com.example.topologyinventory.framework.adapters.output.h2.data;
    opens com.example.topologyinventory.framework.adapters.output.h2.data;
}
