/**
 * Descriptor del módulo de arranque (bootstrap).
 *
 * Es la raíz de composición: el único módulo que conoce a los tres hexágonos y
 * los ensambla en una aplicación ejecutable. Por eso 'requires' a los tres.
 *
 * 'requires framework' es, además, el que mete al proveedor del output port en el
 * grafo de módulos en ejecución: gracias a él, el 'ServiceLoader.load(...)' que
 * vive dentro de RouterManagementInputPort (módulo application) encuentra a
 * RouterManagementH2Adapter en tiempo de ejecución. No hace falta declarar 'uses'
 * aquí: esa declaración vive en application, que es quien consume el servicio.
 *
 * 'requires quarkus.core': bootstrap es el ÚNICO módulo que conoce el framework de
 * arranque. Al implementar QuarkusApplication y anotarse con @QuarkusMain
 * (tipos de io.quarkus.runtime), su descriptor debe requerir el módulo de Quarkus.
 * Es la costura JPMS <-> Quarkus, ubicada a propósito en la capa más externa: los
 * tres hexágonos internos (domain, application, framework) no requieren ningún
 * módulo de Quarkus, preservando la dirección de dependencias hacia dentro.
 */
module bootstrap {
    requires domain;
    requires application;
    requires framework;
    requires quarkus.core;
}
