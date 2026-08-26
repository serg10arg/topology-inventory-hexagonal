package com.example.topologyinventory.domain.specification.shared;

import com.example.topologyinventory.domain.exception.GenericSpecificationException;

/**
 * Composite que representa el AND lógico de dos especificaciones: se satisface
 * solo cuando ambas hijas se satisfacen. Es el mecanismo que permite construir
 * reglas compuestas a partir de reglas simples.
 *
 * @param <T> tipo del objeto que la especificación evalúa
 */
public class AndSpecification<T> extends AbstractSpecification<T> {

    private final Specification<T> spec1;
    private final Specification<T> spec2;

    public AndSpecification(final Specification<T> spec1, final Specification<T> spec2) {
        this.spec1 = spec1;
        this.spec2 = spec2;
    }

    @Override
    public boolean isSatisfiedBy(final T t) {
        return spec1.isSatisfiedBy(t) && spec2.isSatisfiedBy(t);
    }

    /**
     * Intencionadamente vacío: la composición AND se consulta vía
     * {@link #isSatisfiedBy(Object)}. no dota al composite de una
     * validación imperativa propia.
     */
    @Override
    public void check(T t) throws GenericSpecificationException {
        // Sin comportamiento: usar isSatisfiedBy para evaluar la composición.
    }
}