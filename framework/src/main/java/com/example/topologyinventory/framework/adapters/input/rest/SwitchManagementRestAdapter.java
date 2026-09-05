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
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

/**
 * Adapter de entrada REST (<em>driving</em>) para la gestión de switches.
 *
 * <p><b>Divergencia respecto al libro / al camino de router.</b> En este núcleo un switch no
 * se persiste ni se recupera de forma independiente: solo existe como hijo del agregado de un
 * router (no hay {@code SwitchManagementH2Adapter}). Por eso:
 * <ul>
 *   <li>{@code create} opera <em>en memoria</em> y devuelve un switch efímero (no persistido);
 *       no toca la base, así que no necesita {@code @Blocking}.</li>
 *   <li>{@code add}/{@code remove} recuperan el edge router por su id (a través de
 *       {@link RouterManagementUseCase}), lo mutan <em>en memoria</em> y lo devuelven, sin
 *       persistir la conexión —igual que {@code /router/add} de la rebanada anterior—. Tocan
 *       la base al recuperar, así que van {@code @Blocking}. Persistir el agregado con hijos
 *       depende de la decisión pendiente del {@code @OneToMany} sin cascade.</li>
 * </ul>
 * Inyecta dos casos de uso: el de switches (crear/conectar/desconectar) y el de routers
 * (recuperar el edge destino). No contiene lógica de negocio: localizar un switch dentro del
 * edge recuperado es una búsqueda por id, consecuencia de que el switch no sea recuperable
 * por sí mismo.
 */
@ApplicationScoped
@Path("/switch")
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
    public Uni<Response> createSwitch(CreateSwitchRequest request) {
        return Uni.createFrom()
                .item(() -> switchManagementUseCase.createSwitch(
                        request.getVendor(),
                        request.getModel(),
                        IP.fromAddress(request.getIp()),
                        request.getLocation().toDomain(),
                        request.getSwitchType()))
                .onItem().transform(networkSwitch ->
                        Response.ok(SwitchResponse.from(networkSwitch)))
                .onItem().transform(Response.ResponseBuilder::build);
    }

    /**
     * {@code POST /switch/add} — crea un switch y lo conecta a un edge router (por su id).
     *
     * @return {@code 200} con el edge resultante, o {@code 404} si el id no resuelve a un edge
     */
    @POST
    @Path("/add")
    @Blocking
    public Uni<Response> addSwitchToEdgeRouter(AddSwitchRequest request) {
        return Uni.createFrom()
                .item(() -> {
                    Router router = routerManagementUseCase.retrieveRouter(
                            Id.withId(request.getEdgeRouterId()));
                    if (!(router instanceof EdgeRouter edgeRouter)) {
                        return null;
                    }
                    Switch networkSwitch = switchManagementUseCase.createSwitch(
                            request.getVendor(),
                            request.getModel(),
                            IP.fromAddress(request.getIp()),
                            request.getLocation().toDomain(),
                            request.getSwitchType());
                    return switchManagementUseCase.addSwitchToEdgeRouter(networkSwitch, edgeRouter);
                })
                .onItem().transform(edge -> edge != null
                        ? Response.ok(RouterResponse.from(edge))
                        : Response.status(Response.Status.NOT_FOUND))
                .onItem().transform(Response.ResponseBuilder::build);
    }

    /**
     * {@code POST /switch/remove} — desconecta un switch (por su id) de un edge router (por su id).
     *
     * @return {@code 200} con el edge resultante, o {@code 404} si el edge o el switch no resuelven
     */
    @POST
    @Path("/remove")
    @Blocking
    public Uni<Response> removeSwitchFromEdgeRouter(RemoveSwitchRequest request) {
        return Uni.createFrom()
                .item(() -> {
                    Router router = routerManagementUseCase.retrieveRouter(
                            Id.withId(request.getEdgeRouterId()));
                    if (!(router instanceof EdgeRouter edgeRouter)) {
                        return null;
                    }
                    Switch networkSwitch = edgeRouter.getSwitches()
                            .get(Id.withId(request.getSwitchId()));
                    if (networkSwitch == null) {
                        return null;
                    }
                    return switchManagementUseCase.removeSwitchFromEdgeRouter(networkSwitch, edgeRouter);
                })
                .onItem().transform(edge -> edge != null
                        ? Response.ok(RouterResponse.from(edge))
                        : Response.status(Response.Status.NOT_FOUND))
                .onItem().transform(Response.ResponseBuilder::build);
    }
}
