/**
 * Descriptor del módulo del Framework hexagon.
 *
 * Es el hexágono más externo: aquí se ensamblan los adapters que conectan el
 * sistema con el mundo real. Requiere 'domain' y 'application' porque consume
 * las entities y value objects del dominio y los use cases y ports de la
 * aplicación. Las flechas de dependencia apuntan siempre hacia dentro.
 *
 * Cláusulas de persistencia (lado de salida):
 * - 'requires jakarta.persistence': la API JPA que anotan las entidades *Data.
 *   El proveedor (Hibernate ORM) lo aporta Quarkus en tiempo de ejecución; no se
 *   declara como módulo requerido porque no se referencian sus tipos en código.
 * - 'opens ...output.h2.data': Hibernate accede por reflexión a los campos
 *   privados de las entidades y embeddables; sin 'opens' fallaría en runtime.
 * - 'exports ...output.h2.data': hace visibles los tipos de persistencia a otros
 *   módulos del reactor (p. ej. los tests de integración).
 *
 * - 'requires jakarta.cdi' y 'requires jakarta.transaction': la SPI estandar con
 *   la que el output adapter obtiene, del contenedor gestionado por Quarkus, el
 *   EntityManager (CDI.current()) y la UserTransaction. Se usan las APIs estandar
 *   de Jakarta -modulos JPMS estables- en lugar de las de Quarkus (Arc,
 *   QuarkusTransaction), de modo que este hexagono no 'requires' ningun modulo
 *   Quarkus: el acoplamiento a Quarkus queda solo en la resolucion de beans en
 *   runtime, no en el descriptor.
 *
 * Provisión de servicio (inversión de dependencias vía JPMS):
 * - 'provides ...RouterManagementOutputPort with ...RouterManagementH2Adapter':
 *   la interfaz de salida definida por application queda implementada por el
 *   output adapter de H2. La mitad "demanda" ('uses') vive en application.
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

    provides com.example.topologyinventory.application.ports.output.RouterManagementOutputPort
            with com.example.topologyinventory.framework.adapters.output.h2.RouterManagementH2Adapter;
}
