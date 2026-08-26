package com.example.topologyinventory.domain.specification.shared;

/**
 * Contrato del patrón Specification: encapsula una regla de negocio como un
 * predicado reutilizable y combinable sobre objetos de tipo {@code T}.
 *
 * @param <T> tipo del objeto que la especificación evalúa
 */
public interface Specification<T> {

    /**
     * Evalúa si el objeto satisface la regla.
     *
     * @param t objeto a evaluar
     * @return {@code true} si la regla se cumple
     */
    boolean isSatisfiedBy(T t);

    /**
     * Combina esta especificación con otra mediante un AND lógico.
     *
     * @param specification especificación a combinar
     * @return una especificación que representa {@code this AND specification}
     */
    Specification<T> and(Specification<T> specification);
}