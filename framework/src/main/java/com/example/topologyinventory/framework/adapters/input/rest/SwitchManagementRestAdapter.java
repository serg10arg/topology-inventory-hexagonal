package com.example.topologyinventory.framework.adapters.input.rest;

import com.example.topologyinventory.application.usecases.RouterManagementUseCase;
import com.example.topologyinventory.application.usecases.SwitchManagementUseCase;
import com.example.topologyinventory.domain.entity.EdgeRouter;
import com.example.topologyinventory.domain.entity.Router;
import com.example.topologyinventory.domain.entity.Switch;
import com.example.topologyinventory.domain.vo.IP;
import com.example.topologyinventory.domain.vo.Id;
import com.example.topologyinventory.framework.adapters.input.rest.request.AddSwitchRequest;
import com.example.topologyinventory.framework.adapters.input.rest.request.CreateSwitchRequest;
import com.example.topologyinventory.framework.adapters.input.rest.request.RemoveSwitchRequest;
import com.example.topologyinventory.framework.adapters.input.rest.response.RouterResponse;
import com.example.topologyinventory.framework.adapters.input.rest.response.SwitchResponse;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Adapter de entrada REST (<em>driving</em>) para la gestión de switches.
 *
 * <p><b>Divergencia (sin cambios de fondo).</b> En este núcleo un switch solo existe como hijo de
 * un router (no hay adapter de persistencia de switch). {@code create} opera en memoria;
 * {@code add}/{@code remove} recuperan el edge router y lo mutan en memoria, sin persistir.
 *
 * <p><b>Reactivo (Fase 8).</b> {@code retrieveRouter} es ahora reactivo, así que {@code add}/
 * {@code remove} <em>componen</em> sobre su {@link Uni} y ya no llevan {@code @Blocking}. El edge
 * recuperado trae sus switches materializados por el output adapter ({@code session.fetch}).
 */
@ApplicationScoped
@Path("/switch")
@Tag(name = "Switch Operations", description = "Alta, baja y conexión de switches a edge routers")
public class SwitchManagementRestAdapter {

    @Inject
    SwitchManagementUseCase switchManagementUseCase;

    @Inject
    RouterManagementUseCase routerManagementUseCase;

    /**
     * {@code POST /switch/create} — crea un switch (en memoria, sin persistir ni conectar).
     *
     * @return {@code 200} con el {@link SwitchResponse} del switch creado
     */
    @POST
    @Path("/create")
    @Operation(operationId = "createSwitch", summary = "Crea un switch (en memoria)")
    public Uni<Response> createSwitch(CreateSwitchRequest request) {
        var networkSwitch = switchManagementUseCase.createSwitch(
                request.getVendor(),
                request.getModel(),
                IP.fromAddress(request.getIp()),
                request.getLocation().toDomain(),
                request.getSwitchType());
        return Uni.createFrom().item(Response.ok(SwitchResponse.from(networkSwitch)).build());
    }

    /**
     * {@code POST /switch/add} — crea un switch y lo conecta a un edge router (por su id).
     *
     * @return {@code 200} con el edge resultante, o {@code 404} si el id no resuelve a un edge
     */
    @POST
    @Path("/add")
    @Operation(operationId = "addSwitchToEdgeRouter", summary = "Crea un switch y lo conecta a un edge router")
    public Uni<Response> addSwitchToEdgeRouter(AddSwitchRequest request) {
        return routerManagementUseCase.retrieveRouter(Id.withId(request.getEdgeRouterId()))
                .onItem().transform(router -> {
                    if (!(router instanceof EdgeRouter edgeRouter)) {
                        return Response.status(Response.Status.NOT_FOUND).build();
                    }
                    Switch networkSwitch = switchManagementUseCase.createSwitch(
                            request.getVendor(),
                            request.getModel(),
                            IP.fromAddress(request.getIp()),
                            request.getLocation().toDomain(),
                            request.getSwitchType());
                    Router edge = switchManagementUseCase.addSwitchToEdgeRouter(networkSwitch, edgeRouter);
                    return Response.ok(RouterResponse.from(edge)).build();
                });
    }

    /**
     * {@code POST /switch/remove} — desconecta un switch (por su id) de un edge router (por su id).
     *
     * @return {@code 200} con el edge resultante, o {@code 404} si el edge o el switch no resuelven
     */
    @POST
    @Path("/remove")
    @Operation(operationId = "removeSwitchFromEdgeRouter", summary = "Desconecta un switch de un edge router")
    public Uni<Response> removeSwitchFromEdgeRouter(RemoveSwitchRequest request) {
        return routerManagementUseCase.retrieveRouter(Id.withId(request.getEdgeRouterId()))
                .onItem().transform(router -> {
                    if (!(router instanceof EdgeRouter edgeRouter)) {
                        return Response.status(Response.Status.NOT_FOUND).build();
                    }
                    Switch networkSwitch = edgeRouter.getSwitches().get(Id.withId(request.getSwitchId()));
                    if (networkSwitch == null) {
                        return Response.status(Response.Status.NOT_FOUND).build();
                    }
                    Router edge = switchManagementUseCase.removeSwitchFromEdgeRouter(networkSwitch, edgeRouter);
                    return Response.ok(RouterResponse.from(edge)).build();
                });
    }
}
