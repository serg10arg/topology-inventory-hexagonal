package com.example.topologyinventory.application.ports.input;

import com.example.topologyinventory.application.ports.output.RouterManagementOutputPort;
import com.example.topologyinventory.application.usecases.RouterManagementUseCase;
import com.example.topologyinventory.domain.entity.CoreRouter;
import com.example.topologyinventory.domain.entity.Router;
import com.example.topologyinventory.domain.entity.factory.RouterFactory;
import com.example.topologyinventory.domain.vo.*;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;

/**
 * Application service que implementa {@link RouterManagementUseCase}. Su papel es
 * <em>orquestar</em>: delega la creación en {@link RouterFactory}, las conexiones en el agregado
 * {@link CoreRouter} y la persistencia en el puerto de salida. No contiene reglas de negocio.
 *
 * <p><b>Cableado (CDI).</b> Es un bean {@code @ApplicationScoped}: Arc lo comparte como única
 * instancia. El puerto de salida llega por {@code @Inject} (Arc localiza el output adapter que
 * implementa {@link RouterManagementOutputPort} sin que este hexágono conozca la clase concreta).
 *
 * <p><b>Reactivo (Fase 8).</b> {@code retrieveRouter}/{@code persistRouter} ya no devuelven un
 * {@link Router} directo, sino la {@link Uni} del puerto de salida: la orquestación es aquí un
 * <em>passthrough</em> reactivo, sin bloquear. Las operaciones en memoria se delegan tal cual.
 */
@NoArgsConstructor
@ApplicationScoped
public class RouterManagementInputPort implements RouterManagementUseCase {

    /** Puerto de salida hacia la persistencia reactiva, provisto por el contenedor. */
    @Inject
    RouterManagementOutputPort routerManagementOutputPort;

    /** {@inheritDoc} Delega la construcción del tipo concreto en {@link RouterFactory}. */
    @Override
    public Router createRouter(Vendor vendor, Model model, IP ip, Location location, RouterType routerType) {
        return RouterFactory.getRouter(null, vendor, model, ip, location, routerType);
    }

    /** {@inheritDoc} Devuelve la {@link Uni} del puerto de salida sin bloquear. */
    @Override
    public Uni<Router> retrieveRouter(Id id) {
        return routerManagementOutputPort.retrieveRouter(id);
    }

    /** {@inheritDoc} Devuelve la {@link Uni} del puerto de salida sin bloquear. */
    @Override
    public Uni<Router> persistRouter(Router router) {
        return routerManagementOutputPort.persistRouter(router);
    }

    /** {@inheritDoc} La validación de la conexión ocurre dentro del agregado (en memoria). */
    @Override
    public CoreRouter addRouterToCoreRouter(Router router, CoreRouter coreRouter) {
        return coreRouter.addRouter(router);
    }

    /** {@inheritDoc} La validación de la desconexión ocurre dentro del agregado (en memoria). */
    @Override
    public Router removeRouterFromCoreRouter(Router router, CoreRouter coreRouter) {
        return coreRouter.removeRouter(router);
    }
}
