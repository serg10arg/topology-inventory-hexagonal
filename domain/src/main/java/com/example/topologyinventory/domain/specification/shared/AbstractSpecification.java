package com.example.topologyinventory.domain.specification.shared;

import com.example.topologyinventory.domain.exception.GenericSpecificationException;

/**
 * Base de las especificaciones concretas. Aporta la combinación
 * {@link #and(Specification)} y el método {@link #check(Object)}, que ejecuta la
 * regla lanzando una excepción cuando no se cumple (en vez de limitarse a
 * devolver un booleano). Las subclases solo definen qué significa satisfacer la
 * regla y qué error reportar.
 *
 * @param <T> tipo del objeto que la especificación evalúa
 */
public abstract class AbstractSpecification<T> implements Specification<T> {

    @Override
    public abstract boolean isSatisfiedBy(T t);

    /**
     * Ejecuta la regla y lanza una excepción si no se cumple. Es la forma
     * imperativa de aplicar la especificación: las entities la usan como guarda
     * antes de mutar su estado.
     *
     * @param t objeto a validar
     * @throws GenericSpecificationException si la regla no se satisface
     */
    public abstract void check(T t) throws GenericSpecificationException;

    @Override
    public Specification<T> and(final Specification<T> specification) {
        return new AndSpecification<>(this, specification);
    }
}