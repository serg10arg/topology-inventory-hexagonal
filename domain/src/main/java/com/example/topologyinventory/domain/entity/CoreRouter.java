package com.example.topologyinventory.domain.entity;

import com.example.topologyinventory.domain.specification.EmptyRouterSpec;
import com.example.topologyinventory.domain.specification.EmptySwitchSpec;
import com.example.topologyinventory.domain.specification.SameCountrySpec;
import com.example.topologyinventory.domain.specification.SameIpSpec;
import com.example.topologyinventory.domain.vo.IP;
import com.example.topologyinventory.domain.vo.Id;
import com.example.topologyinventory.domain.vo.Location;
import com.example.topologyinventory.domain.vo.Model;
import com.example.topologyinventory.domain.vo.RouterType;
import com.example.topologyinventory.domain.vo.Vendor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;

/**
 * Router troncal (core). Actúa como nodo de interconexión: agrupa otros routers
 * (core o edge), nunca switches directamente. Es la raíz del agregado de
 * topología, por lo que centraliza las reglas para conectar y desconectar routers.
 */
@Getter
@ToString
public class CoreRouter extends Router {

    /** Routers (core o edge) conectados a este core router, indexados por su Id. */
    private Map<Id, Router> routers;

    @Builder
    public CoreRouter(Id id, Vendor vendor, Model model, IP ip, Location location,
                      RouterType routerType, Map<Id, Router> routers) {
        super(id, vendor, model, ip, location, routerType);
        this.routers = routers;
    }

    /**
     * Conecta un router a este core router, previa comprobación de dos reglas: el
     * router debe estar en el mismo país ({@link SameCountrySpec}) y no puede
     * compartir IP con este core router ({@link SameIpSpec}).
     *
     * @param anyRouter router a conectar
     * @return este mismo {@link CoreRouter} (raíz del agregado) con el router ya incorporado
     */
    public CoreRouter addRouter(Router anyRouter) {
        var sameCountryRouterSpec = new SameCountrySpec(this);
        var sameIpSpec = new SameIpSpec(this);

        sameCountryRouterSpec.check(anyRouter);
        sameIpSpec.check(anyRouter);

        this.routers.put(anyRouter.id, anyRouter);
        return this;                    // antes devolvía el valor previo de put(...) (Router)
    }

    /**
     * Desconecta un router. Antes de eliminarlo comprueba que no arrastre
     * dependencias: si es core, no debe tener routers conectados
     * ({@link EmptyRouterSpec}); si es edge, no debe tener switches
     * ({@link EmptySwitchSpec}).
     *
     * @param anyRouter router a desconectar
     * @return el router eliminado, o {@code null} si no estaba presente
     */
    public Router removeRouter(Router anyRouter) {
        var emptyRoutersSpec = new EmptyRouterSpec();
        var emptySwitchSpec = new EmptySwitchSpec();

        switch (anyRouter.routerType) {
            case CORE:
                var coreRouter = (CoreRouter) anyRouter;
                emptyRoutersSpec.check(coreRouter);
                break;
            case EDGE:
                var edgeRouter = (EdgeRouter) anyRouter;
                emptySwitchSpec.check(edgeRouter);
        }
        return this.routers.remove(anyRouter.id);
    }
}