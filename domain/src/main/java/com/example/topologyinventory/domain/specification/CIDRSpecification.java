package com.example.topologyinventory.domain.specification;

import com.example.topologyinventory.domain.exception.GenericSpecificationException;
import com.example.topologyinventory.domain.specification.shared.AbstractSpecification;

/**
 * Regla: el prefijo CIDR de una red debe ser igual o mayor que el mínimo
 * permitido ({@value #MINIMUM_ALLOWED_CIDR}). Un CIDR demasiado bajo implicaría
 * una red desproporcionadamente grande.
 */
public class CIDRSpecification extends AbstractSpecification<Integer> {

    /** Prefijo CIDR mínimo aceptado. */
    public static final int MINIMUM_ALLOWED_CIDR = 8;

    @Override
    public boolean isSatisfiedBy(Integer cidr) {
        return cidr >= MINIMUM_ALLOWED_CIDR;
    }

    @Override
    public void check(Integer cidr) throws GenericSpecificationException {
        if (!isSatisfiedBy(cidr))
            throw new GenericSpecificationException("CIDR is below " + CIDRSpecification.MINIMUM_ALLOWED_CIDR);
    }
}