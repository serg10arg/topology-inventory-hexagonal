/**
 * Hexágono de aplicación: orquesta el dominio a través de casos de uso (puertos
 * de entrada) y define los puertos de salida que el framework implementará.
 * Depende de {@code domain} y no expone nada al exterior todavía; los exports se
 * afinarán cuando el hexágono de framework consuma estos puertos.
 */
module application {
    requires domain;
    requires static lombok;
}