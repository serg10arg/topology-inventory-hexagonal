package com.example.topologyinventory.application.ports.input;

import com.example.topologyinventory.application.ports.output.RouterManagementOutputPort;
import com.example.topologyinventory.application.usecases.RouterManagementUseCase;
import com.example.topologyinventory.domain.entity.CoreRouter;
import com.example.topologyinventory.domain.entity.Router;
import com.example.topologyinventory.domain.entity.factory.RouterFactory;
import com.example.topologyinventory.domain.vo.*;
import lombok.NoArgsConstructor;

/**
 * Application service que implementa {@link RouterManagementUseCase}. Su papel es
 * <em>orquestar</em>: delega la creación en {@link RouterFactory}, las conexiones
 * en el agregado {@link CoreRouter} y la persistencia en el puerto de salida. No
 * contiene reglas de negocio.
 */
@NoArgsConstructor
public class RouterManagementInputPort implements RouterManagementUseCase {

    /**
     * Puerto de salida hacia la persistencia. Se declara aquí como la costura por
     * la que el hexágono de framework inyectará su adaptador en fases posteriores
     * (cableado con JPMS {@code provides/uses} y DI de Quarkus). Mientras no se
     * cablee, {@link #retrieveRouter(Id)} y {@link #persistRouter(Router)} no son
     * operativos: por eso en esta fase solo se ejercitan crear, conectar y quitar.
     */
    RouterManagementOutputPort routerManagementOutputPort;

    /** {@inheritDoc} Delega la construcción del tipo concreto en {@link RouterFactory}. */
    @Override
    public Router createRouter(Vendor vendor, Model model, IP ip, Location location, RouterType routerType) {
        return RouterFactory.getRouter(null, vendor, model, ip, location, routerType);
    }

    /** {@inheritDoc} Delega en el puerto de salida. */
    @Override
    public Router retrieveRouter(Id id) {
        return routerManagementOutputPort.retrieveRouter(id);
    }

    /** {@inheritDoc} Delega en el puerto de salida. */
    @Override
    public Router persistRouter(Router router) {
        return routerManagementOutputPort.persistRouter(router);
    }

    /** {@inheritDoc} La validación de la conexión ocurre dentro del agregado. */
    @Override
    public CoreRouter addRouterToCoreRouter(Router router, CoreRouter coreRouter) {
        return coreRouter.addRouter(router);
    }

    /** {@inheritDoc} La validación de la desconexión ocurre dentro del agregado. */
    @Override
    public Router removeRouterFromCoreRouter(Router router, CoreRouter coreRouter) {
        return coreRouter.removeRouter(router);
    }
}