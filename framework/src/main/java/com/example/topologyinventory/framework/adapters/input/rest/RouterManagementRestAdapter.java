package com.example.topologyinventory.framework.adapters.input.rest;

import com.example.topologyinventory.application.usecases.RouterManagementUseCase;
import com.example.topologyinventory.domain.entity.CoreRouter;
import com.example.topologyinventory.domain.vo.IP;
import com.example.topologyinventory.domain.vo.Id;
import com.example.topologyinventory.framework.adapters.input.rest.request.AddRouterRequest;
import com.example.topologyinventory.framework.adapters.input.rest.request.CreateRouterRequest;
import com.example.topologyinventory.framework.adapters.input.rest.request.RemoveRouterRequest;
import com.example.topologyinventory.framework.adapters.input.rest.response.RouterResponse;
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
 * Adapter de entrada REST (<em>driving</em>) para la gestión de routers. Recibe la petición, la
 * traduce al vocabulario del dominio y delega en el caso de uso; no contiene lógica de negocio.
 *
 * <p><b>Reactivo de punta a punta (Fase 8).</b> Retirados los {@code @Blocking}: el caso de uso
 * llega ahora hasta Hibernate Reactive, así que cada endpoint <em>compone</em> sobre la
 * {@link Uni} del caso de uso en lugar de envolver trabajo bloqueante en un <em>worker thread</em>.
 * {@code retrieveRouter}/{@code persistRouter} son reactivos; {@code createRouter} (construcción
 * en memoria) es síncrono y se combina dentro de la cadena.
 *
 * <p><b>Rutas y desviaciones (sin cambios).</b> {@code POST /router/create} funde crear+persistir;
 * no hay {@code DELETE} (el núcleo no expone {@code removeRouter(id)}); una lectura sin resultado
 * responde {@code 404}; {@code add}/{@code remove} operan sobre el agregado recuperado en memoria
 * y no persisten la conexión (deuda del cascade, SC2).
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
    public Uni<Response> retrieveRouter(@PathParam("id") String id) {
        return routerManagementUseCase.retrieveRouter(Id.withId(id))
                .onItem().transform(router -> router != null
                        ? Response.ok(RouterResponse.from(router)).build()
                        : Response.status(Response.Status.NOT_FOUND).build());
    }

    /**
     * {@code POST /router/create} — crea un router y lo persiste (create + persist fundidos).
     *
     * @return {@code 200} con el {@link RouterResponse} del router persistido
     */
    @POST
    @Path("/create")
    @Operation(operationId = "createRouter", summary = "Crea y persiste un router")
    public Uni<Response> createRouter(CreateRouterRequest request) {
        var router = routerManagementUseCase.createRouter(
                request.getVendor(),
                request.getModel(),
                IP.fromAddress(request.getIp()),
                request.getLocation().toDomain(),
                request.getRouterType());
        return routerManagementUseCase.persistRouter(router)
                .onItem().transform(persisted ->
                        Response.ok(RouterResponse.from(persisted)).build());
    }

    /**
     * {@code POST /router/add} — conecta un router (por su id) a un core router (por su id).
     *
     * @return {@code 200} con el core resultante, o {@code 404} si algún id no resuelve
     */
    @POST
    @Path("/add")
    @Operation(operationId = "addRouterToCoreRouter", summary = "Conecta un router a un core router")
    public Uni<Response> addRouterToCoreRouter(AddRouterRequest request) {
        return routerManagementUseCase.retrieveRouter(Id.withId(request.getRouterId()))
                .flatMap(router -> routerManagementUseCase
                        .retrieveRouter(Id.withId(request.getCoreRouterId()))
                        .onItem().transform(core -> {
                            if (router == null || !(core instanceof CoreRouter coreRouter)) {
                                return Response.status(Response.Status.NOT_FOUND).build();
                            }
                            var result = routerManagementUseCase.addRouterToCoreRouter(router, coreRouter);
                            return Response.ok(RouterResponse.from(result)).build();
                        }));
    }

    /**
     * {@code POST /router/remove} — desconecta un router (por su id) de un core router.
     *
     * @return {@code 200} con el router desconectado, o {@code 404} si no estaba conectado
     */
    @POST
    @Path("/remove")
    @Operation(operationId = "removeRouterFromCoreRouter", summary = "Desconecta un router de un core router")
    public Uni<Response> removeRouterFromCoreRouter(RemoveRouterRequest request) {
        return routerManagementUseCase.retrieveRouter(Id.withId(request.getRouterId()))
                .flatMap(router -> routerManagementUseCase
                        .retrieveRouter(Id.withId(request.getCoreRouterId()))
                        .onItem().transform(core -> {
                            if (router == null || !(core instanceof CoreRouter coreRouter)) {
                                return Response.status(Response.Status.NOT_FOUND).build();
                            }
                            var removed = routerManagementUseCase.removeRouterFromCoreRouter(router, coreRouter);
                            return removed != null
                                    ? Response.ok(RouterResponse.from(removed)).build()
                                    : Response.status(Response.Status.NOT_FOUND).build();
                        }));
    }
}
