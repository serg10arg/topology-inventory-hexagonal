/**
 * Descriptor del módulo del Framework hexagon.
 *
 * Es el hexágono más externo: aquí se ensamblan los adapters que conectan el
 * sistema con el mundo real. Requiere 'domain' y 'application' porque consume
 * las entities y value objects del dominio y los use cases y ports de la
 * aplicación. Las flechas de dependencia apuntan siempre hacia dentro.
 *
 * Cláusulas de persistencia (lado de salida):
 * - 'requires jakarta.persistence' y 'org.eclipse.persistence.core': la API JPA
 *   y el proveedor EclipseLink usados por los output adapters y las entidades.
 * - 'requires java.sql': lo necesita el UUIDTypeConverter (java.sql.Types).
 * - 'opens ...output.h2.data': EclipseLink accede por reflexión a los campos
 *   privados de las entidades; sin 'opens' fallaría en tiempo de ejecución.
 * - 'exports ...output.h2.data': hace visibles los tipos de persistencia a
 *   otros módulos del reactor (p. ej. los tests de integración).
 *
 * Nota: los 'requires' de Jackson y los adapters de entrada se añadirán cuando
 * esas piezas existan, para no arrastrar dependencias sin uso.
 */
module framework {
    requires domain;
    requires application;
    requires static lombok;
    requires jakarta.persistence;
    requires org.eclipse.persistence.core;
    requires java.sql;

    exports com.example.topologyinventory.framework.adapters.output.h2.data;
    opens com.example.topologyinventory.framework.adapters.output.h2.data;
}