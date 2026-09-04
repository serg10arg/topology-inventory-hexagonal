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
 */
module framework {
    requires domain;
    requires application;
    requires static lombok;
    requires jakarta.persistence;
    requires jakarta.cdi;
    requires jakarta.transaction;

    exports com.example.topologyinventory.framework.adapters.output.h2.data;
    opens com.example.topologyinventory.framework.adapters.output.h2.data;

    exports com.example.topologyinventory.framework.adapters.input.generic;
}
