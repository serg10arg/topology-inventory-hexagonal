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
 */
module bootstrap {
    requires domain;
    requires application;
    requires framework;
}
