package com.example.topologyinventory.framework.adapters.input.generic;

import com.example.topologyinventory.application.usecases.RouterManagementUseCase;
import com.example.topologyinventory.domain.entity.CoreRouter;
import com.example.topologyinventory.domain.entity.Router;
import com.example.topologyinventory.domain.vo.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Adapter de entrada (<em>driving</em>) para la gestión de routers.
 *
 * Es el punto por el que el mundo exterior invoca al sistema: recibe la petición y la
 * reenvía al caso de uso, sin contener lógica propia. En esta fase es un bean CDI; será
 * la base del futuro adapter REST (los comentarios de cada método anticipan su endpoint).
 *
 * <p><b>Cableado (CDI).</b> Es un bean {@code @ApplicationScoped} y el caso de uso llega
 * por {@code @Inject}: Arc provee el bean que implementa {@link RouterManagementUseCase}
 * ({@code RouterManagementInputPort}). Sustituye al {@code new RouterManagementInputPort()}
 * del constructor anterior.
 */
@ApplicationScoped
public class RouterManagementGenericAdapter {

    @Inject
    RouterManagementUseCase routerManagementUseCase;

    /**
     * POST /router/create — crea un router del tipo indicado (sin persistir).
     *
     * @return el {@link Router} creado (un {@code CoreRouter} o un {@code EdgeRouter})
     */
    public Router createRouter(Vendor vendor, Model model, IP ip,
                               Location location, RouterType routerType) {
        return routerManagementUseCase.createRouter(vendor, model, ip, location, routerType);
    }

    /**
     * POST /router/add — conecta un router a un core router.
     *
     * @return el {@link CoreRouter} con el router ya incorporado
     */
    public CoreRouter addRouterToCoreRouter(Router router, CoreRouter coreRouter) {
        return routerManagementUseCase.addRouterToCoreRouter(router, coreRouter);
    }

    /**
     * POST /router/remove — desconecta un router de un core router.
     *
     * @return el {@link Router} desconectado, o {@code null} si no estaba presente
     */
    public Router removeRouterFromCoreRouter(Router router, CoreRouter coreRouter) {
        return routerManagementUseCase.removeRouterFromCoreRouter(router, coreRouter);
    }

    /**
     * GET /router/retrieve/{id} — recupera un router del almacenamiento.
     *
     * @return el {@link Router} recuperado
     */
    public Router retrieveRouter(Id id) {
        return routerManagementUseCase.retrieveRouter(id);
    }

    /**
     * POST /router/persist — persiste un router.
     *
     * @return el {@link Router} persistido
     */
    public Router persistRouter(Router router) {
        return routerManagementUseCase.persistRouter(router);
    }
}
