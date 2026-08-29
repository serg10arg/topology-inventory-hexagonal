/**
 * Descriptor del módulo del Framework hexagon.
 *
 * Es el hexágono más externo: aquí se ensamblan los adapters que conectan el
 * sistema con el mundo real. Requiere 'domain' y 'application' porque consume
 * las entities y value objects del dominio y los use cases y ports de la
 * aplicación. Las flechas de dependencia apuntan siempre hacia dentro: el
 * framework conoce el núcleo, nunca al revés.
 */
module framework {
    requires domain;
    requires application;
}