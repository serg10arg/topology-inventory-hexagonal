package com.example.topologyinventory.domain.specification;

import com.example.topologyinventory.domain.entity.CoreRouter;
import com.example.topologyinventory.domain.exception.GenericSpecificationException;
import com.example.topologyinventory.domain.specification.shared.AbstractSpecification;

/**
 * Regla: un core router solo puede eliminarse si no tiene otros routers
 * conectados. Evita dejar routers huérfanos al desconectar un nodo troncal.
 */
public class EmptyRouterSpec extends AbstractSpecification<CoreRouter> {

    @Override
    public boolean isSatisfiedBy(CoreRouter coreRouter) {
        return coreRouter.getRouters() == null || coreRouter.getRouters().isEmpty();
    }

    @Override
    public void check(CoreRouter coreRouter) {
        if (!isSatisfiedBy(coreRouter))
            throw new GenericSpecificationException("It isn't allowed to remove a core router with other routers attached to it");
    }
}