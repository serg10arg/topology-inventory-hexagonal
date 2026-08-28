package com.example.topologyinventory.application.usecases;

import com.example.topologyinventory.domain.entity.CoreRouter;
import com.example.topologyinventory.domain.entity.Router;
import com.example.topologyinventory.domain.vo.*;

/**
 * Puerto de entrada del hexágono de aplicación para la gestión de routers.
 * Expresa, en términos del dominio, las operaciones que el sistema ofrece sobre
 * routers: crearlos, conectarlos y desconectarlos de un core router, y
 * recuperarlos o persistirlos. Es únicamente el contrato; la orquestación vive
 * en la clase que lo implementa ({@code RouterManagementInputPort}) y las reglas
 * de negocio permanecen en el dominio.
 */
public interface RouterManagementUseCase {

    /**
     * Crea un router del tipo indicado sin persistirlo.
     *
     * @param vendor     fabricante del router
     * @param model      modelo del router
     * @param ip         dirección IP asignada
     * @param location   ubicación física
     * @param routerType tipo de router (CORE o EDGE), que determina la subclase creada
     * @return el {@link Router} recién creado (un CoreRouter o un EdgeRouter)
     */
    Router createRouter(Vendor vendor, Model model, IP ip, Location location, RouterType routerType);

    /**
     * Conecta un router a un core router aplicando las reglas del agregado.
     *
     * @param router     router a conectar
     * @param coreRouter core router que actúa como raíz del agregado
     * @return el {@link CoreRouter} con el router ya incorporado
     */
    CoreRouter addRouterToCoreRouter(Router router, CoreRouter coreRouter);

    /**
     * Desconecta un router de un core router.
     *
     * @param router     router a desconectar
     * @param coreRouter core router del que se elimina
     * @return el router desconectado, o {@code null} si no estaba presente
     */
    Router removeRouterFromCoreRouter(Router router, CoreRouter coreRouter);

    /**
     * Recupera un router desde el almacenamiento a través del puerto de salida.
     *
     * @param id identidad del router buscado
     * @return el {@link Router} recuperado
     */
    Router retrieveRouter(Id id);

    /**
     * Persiste un router a través del puerto de salida.
     *
     * @param router router a persistir
     * @return el {@link Router} persistido
     */
    Router persistRouter(Router router);
}