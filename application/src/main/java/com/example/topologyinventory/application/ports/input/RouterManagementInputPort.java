package com.example.topologyinventory.application.ports.input;

import com.example.topologyinventory.application.ports.output.RouterManagementOutputPort;
import com.example.topologyinventory.application.usecases.RouterManagementUseCase;
import com.example.topologyinventory.domain.entity.CoreRouter;
import com.example.topologyinventory.domain.entity.Router;
import com.example.topologyinventory.domain.entity.factory.RouterFactory;
import com.example.topologyinventory.domain.vo.*;
import lombok.NoArgsConstructor;

import java.util.ServiceLoader;

/**
 * Application service que implementa {@link RouterManagementUseCase}. Su papel es
 * <em>orquestar</em>: delega la creación en {@link RouterFactory}, las conexiones
 * en el agregado {@link CoreRouter} y la persistencia en el puerto de salida. No
 * contiene reglas de negocio.
 */
@NoArgsConstructor
public class RouterManagementInputPort implements RouterManagementUseCase {

    /**
     * Puerto de salida hacia la persistencia. Se resuelve de forma perezosa a
     * través de {@link #outputPort()}: no se inyecta por constructor (eso acoplaría
     * la creación de este application service al arranque de la base de datos) sino
     * que se obtiene vía {@link ServiceLoader} la primera vez que hace falta.
     */
    RouterManagementOutputPort routerManagementOutputPort;

    /**
     * Resuelve el puerto de salida bajo demanda y lo memoriza. El módulo application
     * declara {@code uses RouterManagementOutputPort}; el módulo framework lo
     * {@code provides} con su output adapter. ServiceLoader localiza esa
     * implementación en el grafo de módulos sin que este hexágono conozca la clase
     * concreta. Se resuelve solo al persistir o recuperar, de modo que crear,
     * conectar y desconectar routers no arrastran el coste de la persistencia.
     *
     * @return la implementación del puerto de salida
     * @throws IllegalStateException si no hay ningún proveedor en el module path
     */
    private RouterManagementOutputPort outputPort() {
        if (routerManagementOutputPort == null) {
            routerManagementOutputPort = ServiceLoader
                    .load(RouterManagementOutputPort.class)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "No hay ninguna implementación de RouterManagementOutputPort "
                                    + "en el module path (¿falta el módulo framework?)"));
        }
        return routerManagementOutputPort;
    }

    /** {@inheritDoc} Delega la construcción del tipo concreto en {@link RouterFactory}. */
    @Override
    public Router createRouter(Vendor vendor, Model model, IP ip, Location location, RouterType routerType) {
        return RouterFactory.getRouter(null, vendor, model, ip, location, routerType);
    }

    /** {@inheritDoc} Delega en el puerto de salida, resuelto de forma perezosa. */
    @Override
    public Router retrieveRouter(Id id) {
        return outputPort().retrieveRouter(id);
    }

    /** {@inheritDoc} Delega en el puerto de salida, resuelto de forma perezosa. */
    @Override
    public Router persistRouter(Router router) {
        return outputPort().persistRouter(router);
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