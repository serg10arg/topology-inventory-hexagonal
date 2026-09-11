package com.example.topologyinventory.application.usecases;

import com.example.topologyinventory.domain.entity.CoreRouter;
import com.example.topologyinventory.domain.entity.Router;
import com.example.topologyinventory.domain.vo.*;
import io.smallrye.mutiny.Uni;

/**
 * Puerto de entrada del hexágono de aplicación para la gestión de routers. Expresa, en términos
 * del dominio, las operaciones que el sistema ofrece sobre routers.
 *
 * <p><b>Reactivo acotado (Fase 8, decisión D3).</b> Solo {@link #retrieveRouter} y
 * {@link #persistRouter} devuelven {@link Uni}, porque son las únicas que tocan el puerto de
 * salida reactivo. {@link #createRouter}, {@link #addRouterToCoreRouter} y
 * {@link #removeRouterFromCoreRouter} son trabajo <em>en memoria</em> (construcción y mutación
 * del agregado, sin persistencia), así que permanecen síncronas: envolverlas en {@code Uni}
 * sería ruido sin beneficio.
 */
public interface RouterManagementUseCase {

    /**
     * Crea un router del tipo indicado sin persistirlo (en memoria).
     *
     * @return el {@link Router} recién creado (un CoreRouter o un EdgeRouter)
     */
    Router createRouter(Vendor vendor, Model model, IP ip, Location location, RouterType routerType);

    /**
     * Conecta un router a un core router aplicando las reglas del agregado (en memoria).
     *
     * @return el {@link CoreRouter} con el router ya incorporado
     */
    CoreRouter addRouterToCoreRouter(Router router, CoreRouter coreRouter);

    /**
     * Desconecta un router de un core router (en memoria).
     *
     * @return el router desconectado, o {@code null} si no estaba presente
     */
    Router removeRouterFromCoreRouter(Router router, CoreRouter coreRouter);

    /**
     * Recupera un router desde el almacenamiento a través del puerto de salida reactivo.
     *
     * @return una {@link Uni} con el {@link Router} recuperado, o {@code null} si no existe
     */
    Uni<Router> retrieveRouter(Id id);

    /**
     * Persiste un router a través del puerto de salida reactivo.
     *
     * @return una {@link Uni} con el {@link Router} persistido
     */
    Uni<Router> persistRouter(Router router);
}
