package com.example.topologyinventory.application.ports.input;

import com.example.topologyinventory.application.ports.output.RouterManagementOutputPort;
import com.example.topologyinventory.application.usecases.RouterManagementUseCase;
import com.example.topologyinventory.domain.entity.CoreRouter;
import com.example.topologyinventory.domain.entity.Router;
import com.example.topologyinventory.domain.entity.factory.RouterFactory;
import com.example.topologyinventory.domain.vo.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;

/**
 * Application service que implementa {@link RouterManagementUseCase}. Su papel es
 * <em>orquestar</em>: delega la creación en {@link RouterFactory}, las conexiones en
 * el agregado {@link CoreRouter} y la persistencia en el puerto de salida. No contiene
 * reglas de negocio.
 *
 * <p><b>Cableado (CDI).</b> Es un bean {@code @ApplicationScoped}: Arc lo comparte como
 * única instancia y lo crea de forma perezosa (en la primera llamada). El puerto de
 * salida llega por {@code @Inject}, no por {@code ServiceLoader}: Arc localiza el bean
 * que implementa {@link RouterManagementOutputPort} (el output adapter de H2) sin que
 * este hexágono conozca la clase concreta. Como esa dependencia es a su vez
 * {@code @ApplicationScoped}, lo que se recibe es un <em>client proxy</em>; la instancia
 * real —y con ella el arranque de la persistencia— no se materializa hasta la primera
 * operación de persistir o recuperar. Así se conserva, sin código propio, la laziness
 * que antes daba la resolución perezosa por {@code ServiceLoader}.
 */
@NoArgsConstructor
@ApplicationScoped
public class RouterManagementInputPort implements RouterManagementUseCase {

    /**
     * Puerto de salida hacia la persistencia, provisto por el contenedor. Arc inyecta
     * el bean que implementa {@link RouterManagementOutputPort}; su naturaleza de client
     * proxy difiere el coste real (la conexión con H2) a la primera llamada.
     */
    @Inject
    RouterManagementOutputPort routerManagementOutputPort;

    /** {@inheritDoc} Delega la construcción del tipo concreto en {@link RouterFactory}. */
    @Override
    public Router createRouter(Vendor vendor, Model model, IP ip, Location location, RouterType routerType) {
        return RouterFactory.getRouter(null, vendor, model, ip, location, routerType);
    }

    /** {@inheritDoc} Delega en el puerto de salida inyectado. */
    @Override
    public Router retrieveRouter(Id id) {
        return routerManagementOutputPort.retrieveRouter(id);
    }

    /** {@inheritDoc} Delega en el puerto de salida inyectado. */
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
