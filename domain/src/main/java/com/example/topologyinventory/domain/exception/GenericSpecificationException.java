package com.example.topologyinventory.domain.exception;

/**
 * Excepción de dominio que se lanza cuando una {@code Specification} no se cumple.
 * Es no comprobada (extiende {@link RuntimeException}) porque representa la
 * violación de una regla de negocio en tiempo de ejecución, no un error del que
 * el llamador deba encargarse obligatoriamente.
 */
public class GenericSpecificationException extends RuntimeException {

    /** @param message descripción de la regla de negocio incumplida */
    public GenericSpecificationException(String message) {
        super(message);
    }
}