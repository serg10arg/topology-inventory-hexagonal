package com.example.topologyinventory.domain.specification;

import com.example.topologyinventory.domain.entity.EdgeRouter;
import com.example.topologyinventory.domain.exception.GenericSpecificationException;
import com.example.topologyinventory.domain.specification.shared.AbstractSpecification;

/**
 * Regla: un edge router solo puede eliminarse si no tiene switches conectados.
 * Análoga a {@link EmptyRouterSpec}, pero para los edge routers, que son los
 * únicos que pueden contener switches.
 */
public class EmptySwitchSpec extends AbstractSpecification<EdgeRouter> {

    @Override
    public boolean isSatisfiedBy(EdgeRouter edgeRouter) {
        return edgeRouter.getSwitches() == null || edgeRouter.getSwitches().isEmpty();
    }

    @Override
    public void check(EdgeRouter edgeRouter) {
        if (!isSatisfiedBy(edgeRouter))
            throw new GenericSpecificationException("It isn't allowed to remove an edge router with a switch attached to it");
    }
}