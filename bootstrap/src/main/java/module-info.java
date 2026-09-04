/**
 * Descriptor del módulo de arranque (bootstrap).
 *
 * Es la raíz de composición: el único módulo que conoce a los tres hexágonos y los
 * ensambla en una aplicación ejecutable. Por eso 'requires' a los tres.
 *
 * 'requires jakarta.inject': la clase Application recibe el generic adapter por '@Inject'
 * (command mode), así que su descriptor necesita ver la anotación. El contenedor (Arc)
 * descubre e inyecta los beans de los hexágonos internos por el índice Jandex; bootstrap
 * solo declara la anotación que usa.
 *
 * 'requires quarkus.core': bootstrap es el ÚNICO módulo que conoce el framework de arranque.
 * Al implementar QuarkusApplication y anotarse con @QuarkusMain (tipos de io.quarkus.runtime),
 * su descriptor debe requerir el módulo de Quarkus. Es la costura JPMS <-> Quarkus, ubicada a
 * propósito en la capa más externa: los tres hexágonos internos (domain, application,
 * framework) no requieren ningún módulo de Quarkus, preservando la dirección de dependencias
 * hacia dentro.
 */
module bootstrap {
    requires domain;
    requires application;
    requires framework;
    requires jakarta.inject;
    requires quarkus.core;
}
