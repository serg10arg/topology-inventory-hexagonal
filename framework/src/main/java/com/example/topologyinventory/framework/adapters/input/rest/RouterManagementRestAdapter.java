package com.example.topologyinventory.framework.adapters.input.rest;

import com.example.topologyinventory.application.usecases.RouterManagementUseCase;
import com.example.topologyinventory.domain.entity.CoreRouter;
import com.example.topologyinventory.domain.entity.Router;
import com.example.topologyinventory.domain.vo.IP;
import com.example.topologyinventory.domain.vo.Id;
import com.example.topologyinventory.framework.adapters.input.rest.request.AddRouterRequest;
import com.example.topologyinventory.framework.adapters.input.rest.request.CreateRouterRequest;
import com.example.topologyinventory.framework.adapters.input.rest.request.RemoveRouterRequest;
import com.example.topologyinventory.framework.adapters.input.rest.response.RouterResponse;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Adapter de entrada REST (<em>driving</em>) para la gestión de routers. Es la puerta HTTP
 * del sistema: recibe la petición, la traduce al vocabulario del dominio (DTOs → value
 * objects / entidades) y delega en el caso de uso; no contiene lógica de negocio.
 *
 * <p><b>Cableado (CDI).</b> Es un bean {@code @ApplicationScoped} y recibe el caso de uso por
 * {@code @Inject}. Sustituye al antiguo {@code RouterManagementGenericAdapter}: el libro no
 * monta REST <em>encima</em> del generic adapter, sino que <em>convierte</em> la puerta de
 * entrada en un recurso JAX-RS, evitando una capa de <em>passthrough</em> sin valor.
 *
 * <p><b>Reactivo + persistencia bloqueante.</b> Cada endpoint devuelve {@code Uni<Response>}
 * (modelo reactivo de RESTEasy Reactive). Como el caso de uso llega hasta Hibernate ORM
 * clásico —bloqueante—, los métodos se anotan {@code @Blocking} para que RESTEasy Reactive
 * los despache a un <em>worker thread</em>: ejecutar E/S bloqueante en el <em>event loop</em>
 * lanzaría {@code BlockingOperationNotAllowedException}. El trabajo se envuelve en un
 * <em>supplier</em> diferido ({@code .item(() -> ...)}) para que corra en ese hilo, no en el
 * que arma la {@code Uni}. La demarcación transaccional vive en el output adapter
 * ({@code @Transactional}); la proyección a DTO se hace aquí, con el dominio ya materializado.
 *
 * <p><b>Rutas.</b> Se respetan las anticipadas en el código previo: {@code POST /router/create},
 * {@code GET /router/retrieve/{id}}, {@code POST /router/add}, {@code POST /router/remove}.
 * <b>Desviaciones respecto al libro / al mapeo 1:1 de casos de uso:</b>
 * <ul>
 *   <li>{@code create} funde crear + persistir: un alta HTTP que no guardara devolvería un
 *       recurso inexistente, y un {@code persist} separado exigiría recibir la entidad completa
 *       en el cuerpo (fuga del modelo). El caso de uso mantiene ambas operaciones separadas;
 *       la fusión es solo de la frontera REST.</li>
 *   <li>No hay {@code DELETE /router/{id}} (borrado de router): este núcleo no expone
 *       {@code removeRouter(id)} en su caso de uso.</li>
 *   <li>Una lectura sin resultado responde {@code 404}, no {@code 200} con cuerpo nulo como
 *       en el libro.</li>
 *   <li>{@code add}/{@code remove} operan sobre el agregado recuperado <em>en memoria</em> y
 *       no persisten la conexión: persistir el agregado con hijos depende de la decisión
 *       pendiente del {@code @OneToMany} sin cascade (deuda abierta de fases previas).</li>
 * </ul>
 */
@ApplicationScoped
@Path("/router")
@Tag(name = "Router Operations", description = "Alta, baja, conexión y consulta de routers")
public class RouterManagementRestAdapter {

    @Inject
    RouterManagementUseCase routerManagementUseCase;

    /**
     * {@code GET /router/retrieve/{id}} — recupera un router del almacenamiento.
     *
     * @return {@code 200} con el {@link RouterResponse}, o {@code 404} si no existe
     */
    @GET
    @Path("/retrieve/{id}")
    @Operation(operationId = "retrieveRouter", summary = "Recupera un router por su id")
    @Blocking
    public Uni<Response> retrieveRouter(@PathParam("id") String id) {
        return Uni.createFrom()
                .item(() -> routerManagementUseCase.retrieveRouter(Id.withId(id)))
                .onItem().transform(router -> router != null
                        ? Response.ok(RouterResponse.from(router))
                        : Response.status(Response.Status.NOT_FOUND))
                .onItem().transform(Response.ResponseBuilder::build);
    }

    /**
     * {@code POST /router/create} — crea un router y lo persiste (create + persist fundidos).
     *
     * @return {@code 200} con el {@link RouterResponse} del router persistido
     */
    @POST
    @Path("/create")
    @Operation(operationId = "createRouter", summary = "Crea y persiste un router")
    @Blocking
    public Uni<Response> createRouter(CreateRouterRequest request) {
        return Uni.createFrom()
                .item(() -> {
                    var router = routerManagementUseCase.createRouter(
                            request.getVendor(),
                            request.getModel(),
                            IP.fromAddress(request.getIp()),
                            request.getLocation().toDomain(),
                            request.getRouterType());
                    return routerManagementUseCase.persistRouter(router);
                })
                .onItem().transform(router -> Response.ok(RouterResponse.from(router)))
                .onItem().transform(Response.ResponseBuilder::build);
    }

    /**
     * {@code POST /router/add} — conecta un router (por su id) a un core router (por su id).
     *
     * @return {@code 200} con el core resultante, o {@code 404} si algún id no resuelve
     *         a los tipos esperados
     */
    @POST
    @Path("/add")
    @Operation(operationId = "addRouterToCoreRouter", summary = "Conecta un router a un core router")
    @Blocking
    public Uni<Response> addRouterToCoreRouter(AddRouterRequest request) {
        return Uni.createFrom()
                .item(() -> {
                    Router router = routerManagementUseCase.retrieveRouter(
                            Id.withId(request.getRouterId()));
                    Router core = routerManagementUseCase.retrieveRouter(
                            Id.withId(request.getCoreRouterId()));
                    if (router == null || !(core instanceof CoreRouter coreRouter)) {
                        return null;
                    }
                    return routerManagementUseCase.addRouterToCoreRouter(router, coreRouter);
                })
                .onItem().transform(core -> core != null
                        ? Response.ok(RouterResponse.from(core))
                        : Response.status(Response.Status.NOT_FOUND))
                .onItem().transform(Response.ResponseBuilder::build);
    }

    /**
     * {@code POST /router/remove} — desconecta un router (por su id) de un core router.
     *
     * @return {@code 200} con el router desconectado, o {@code 404} si no estaba conectado
     *         o algún id no resuelve
     */
    @POST
    @Path("/remove")
    @Operation(operationId = "removeRouterFromCoreRouter", summary = "Desconecta un router de un core router")
    @Blocking
    public Uni<Response> removeRouterFromCoreRouter(RemoveRouterRequest request) {
        return Uni.createFrom()
                .item(() -> {
                    Router router = routerManagementUseCase.retrieveRouter(
                            Id.withId(request.getRouterId()));
                    Router core = routerManagementUseCase.retrieveRouter(
                            Id.withId(request.getCoreRouterId()));
                    if (router == null || !(core instanceof CoreRouter coreRouter)) {
                        return null;
                    }
                    return routerManagementUseCase.removeRouterFromCoreRouter(router, coreRouter);
                })
                .onItem().transform(removed -> removed != null
                        ? Response.ok(RouterResponse.from(removed))
                        : Response.status(Response.Status.NOT_FOUND))
                .onItem().transform(Response.ResponseBuilder::build);
    }
}
