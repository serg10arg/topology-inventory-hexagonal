package com.example.topologyinventory.framework.adapters.input.generic;

import com.example.topologyinventory.application.ports.input.RouterManagementInputPort;
import com.example.topologyinventory.application.usecases.RouterManagementUseCase;
import com.example.topologyinventory.domain.entity.CoreRouter;
import com.example.topologyinventory.domain.entity.Router;
import com.example.topologyinventory.domain.vo.*;

/**
 * Adapter de entrada (<em>driving</em>) para la gestión de routers.
 *
 * Es el punto por el que el mundo exterior invoca al sistema: recibe la petición
 * y la reenvía al caso de uso, sin contener lógica propia. En esta fase es un
 * POJO; será la base del futuro adapter REST (los comentarios de cada método
 * anticipan su endpoint).
 *
 * El caso de uso se instancia con el constructor sin argumentos de
 * {@link RouterManagementInputPort}. El puerto de salida (persistencia) se
 * enchufará en la fase de inyección de dependencias (JPMS {@code provides/uses});
 * hasta entonces solo son operativos crear, conectar y desconectar.
 */
public class RouterManagementGenericAdapter {

    private final RouterManagementUseCase routerManagementUseCase;

    public RouterManagementGenericAdapter() {
        this.routerManagementUseCase = new RouterManagementInputPort();
    }

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
     * <p>Operativo una vez que el puerto de salida quede cableado en la fase de
     * inyección de dependencias.
     *
     * @return el {@link Router} recuperado
     */
    public Router retrieveRouter(Id id) {
        return routerManagementUseCase.retrieveRouter(id);
    }

    /**
     * POST /router/persist — persiste un router.
     *
     * <p>Operativo una vez que el puerto de salida quede cableado en la fase de
     * inyección de dependencias.
     *
     * @return el {@link Router} persistido
     */
    public Router persistRouter(Router router) {
        return routerManagementUseCase.persistRouter(router);
    }
}