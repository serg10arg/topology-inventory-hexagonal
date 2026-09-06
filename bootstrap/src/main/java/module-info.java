/**
 * Descriptor del módulo de arranque (bootstrap).
 *
 * Es la raíz de composición: el único módulo que conoce a los tres hexágonos y los
 * ensambla en una aplicación ejecutable. Por eso 'requires' a los tres, aunque la clase
 * Application ya no referencie sus tipos directamente: el ensamblaje lo realiza Quarkus a
 * partir de las dependencias del reactor, y estos 'requires' documentan qué compone bootstrap.
 *
 * 'requires quarkus.core': bootstrap es el ÚNICO módulo que conoce el framework de arranque.
 * La clase Application se anota con @QuarkusMain y arranca el contenedor con Quarkus.run
 * (tipos de io.quarkus.runtime), así que su descriptor debe requerir el módulo de Quarkus. Es
 * la costura JPMS <-> Quarkus, ubicada a propósito en la capa más externa: los tres hexágonos
 * internos (domain, application, framework) no requieren ningún módulo de Quarkus, preservando
 * la dirección de dependencias hacia dentro.
 *
 * (Se retiró 'requires jakarta.inject': la aplicación pasó a server mode y Application dejó de
 * recibir colaboradores por @Inject; en server mode Arc cablea el grafo bajo demanda, sin que
 * la raíz de composición inyecte nada.)
 */
module bootstrap {
    requires domain;
    requires application;
    requires framework;
    requires quarkus.core;
}
