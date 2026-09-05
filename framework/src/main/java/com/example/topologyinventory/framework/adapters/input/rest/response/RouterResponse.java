package com.example.topologyinventory.framework.adapters.input.rest.response;

import com.example.topologyinventory.domain.entity.CoreRouter;
import com.example.topologyinventory.domain.entity.EdgeRouter;
import com.example.topologyinventory.domain.entity.Router;
import com.example.topologyinventory.domain.vo.Id;
import com.example.topologyinventory.domain.vo.Model;
import com.example.topologyinventory.domain.vo.RouterType;
import com.example.topologyinventory.domain.vo.Vendor;
import lombok.Getter;

import java.util.List;

/**
 * DTO de salida <em>superficial</em> de un router. Expone solo lo que el cliente necesita
 * y proyecta los hijos del agregado como <b>listas de ids</b>, no como objetos anidados.
 *
 * <p>Esa superficialidad es deliberada: cortar el grafo en la frontera evita que la
 * serialización navegue el {@code Map<Id, Router>} / {@code Map<Id, Switch>} del agregado
 * y, con ello, esquiva cualquier interacción con las colecciones {@code @OneToMany}
 * perezosas fuera de transacción. El {@code from(...)} se invoca dentro del método
 * {@code @Blocking} del adapter, con el objeto de dominio ya materializado por el mapper
 * de persistencia, de modo que aquí solo se copian datos.
 *
 * <p>Desviación respecto al libro: el libro serializa la entidad de dominio directamente
 * en la {@code Response}; aquí se aísla con un DTO para no exponer el modelo interno.
 */
@Getter
public class RouterResponse {

    private String id;
    private RouterType routerType;
    private Vendor vendor;
    private Model model;
    private String ip;
    private LocationResponse location;

    /** Ids de los routers conectados (poblado cuando el router es un {@link CoreRouter}). */
    private List<String> routerIds = List.of();
    /** Ids de los switches conectados (poblado cuando el router es un {@link EdgeRouter}). */
    private List<String> switchIds = List.of();

    /**
     * Proyecta un {@link Router} del dominio a su DTO de salida. Según el subtipo concreto,
     * puebla la lista de ids de routers hijos (core) o de switches hijos (edge); la otra
     * queda vacía.
     */
    public static RouterResponse from(Router router) {
        var response = new RouterResponse();
        response.id = router.getId().getId().toString();
        response.routerType = router.getRouterType();
        response.vendor = router.getVendor();
        response.model = router.getModel();
        response.ip = router.getIp().getIpAddress();
        response.location = LocationResponse.from(router.getLocation());

        if (router instanceof CoreRouter coreRouter) {
            response.routerIds = coreRouter.getRouters().keySet().stream()
                    .map(Id::getId)
                    .map(java.util.UUID::toString)
                    .toList();
        } else if (router instanceof EdgeRouter edgeRouter) {
            response.switchIds = edgeRouter.getSwitches().keySet().stream()
                    .map(Id::getId)
                    .map(java.util.UUID::toString)
                    .toList();
        }
        return response;
    }
}
