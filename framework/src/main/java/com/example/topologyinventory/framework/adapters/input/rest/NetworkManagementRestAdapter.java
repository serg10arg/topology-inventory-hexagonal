package com.example.topologyinventory.framework.adapters.input.rest;

import com.example.topologyinventory.application.usecases.NetworkManagementUseCase;
import com.example.topologyinventory.application.usecases.RouterManagementUseCase;
import com.example.topologyinventory.domain.entity.EdgeRouter;
import com.example.topologyinventory.domain.entity.Router;
import com.example.topologyinventory.domain.entity.Switch;
import com.example.topologyinventory.domain.vo.IP;
import com.example.topologyinventory.domain.vo.Id;
import com.example.topologyinventory.domain.vo.Network;
import com.example.topologyinventory.framework.adapters.input.rest.request.AddNetworkRequest;
import com.example.topologyinventory.framework.adapters.input.rest.request.CreateNetworkRequest;
import com.example.topologyinventory.framework.adapters.input.rest.request.RemoveNetworkRequest;
import com.example.topologyinventory.framework.adapters.input.rest.response.NetworkResponse;
import com.example.topologyinventory.framework.adapters.input.rest.response.SwitchResponse;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

/**
 * Adapter de entrada REST (<em>driving</em>) para la gestión de redes.
 *
 * <p><b>Divergencia respecto al libro / al camino de router.</b> Una red es un objeto de valor
 * que vive dentro de un switch, y el switch dentro de un edge router; ninguno se persiste ni se
 * recupera de forma independiente en este núcleo. Por eso:
 * <ul>
 *   <li>{@code create} opera <em>en memoria</em> y devuelve una red efímera; no toca la base,
 *       así que no necesita {@code @Blocking}.</li>
 *   <li>{@code add}/{@code remove} recuperan el edge router por su id, localizan el switch entre
 *       sus hijos (clave compuesta edge + switch), mutan <em>en memoria</em> y devuelven el
 *       switch, sin persistir. Tocan la base al recuperar, así que van {@code @Blocking}.</li>
 * </ul>
 * Inyecta el caso de uso de redes y el de routers (para recuperar el edge). Localizar el switch
 * (por id) y la red (por nombre) dentro del agregado recuperado son búsquedas, no reglas de
 * negocio: son consecuencia de que switch y red no sean recuperables por sí mismos.
 */
@ApplicationScoped
@Path("/network")
public class NetworkManagementRestAdapter {

    @Inject
    NetworkManagementUseCase networkManagementUseCase;

    @Inject
    RouterManagementUseCase routerManagementUseCase;

    /**
     * {@code POST /network/create} — crea una red (en memoria, sin asociarla a un switch).
     *
     * @return {@code 200} con el {@link NetworkResponse} de la red creada
     */
    @POST
    @Path("/create")
    public Uni<Response> createNetwork(CreateNetworkRequest request) {
        return Uni.createFrom()
                .item(() -> networkManagementUseCase.createNetwork(
                        IP.fromAddress(request.getNetworkAddress()),
                        request.getNetworkName(),
                        request.getNetworkCidr()))
                .onItem().transform(network -> Response.ok(NetworkResponse.from(network)))
                .onItem().transform(Response.ResponseBuilder::build);
    }

    /**
     * {@code POST /network/add} — crea una red y la añade a un switch (alcanzado por edge + switch id).
     *
     * @return {@code 200} con el switch resultante, o {@code 404} si el edge o el switch no resuelven
     */
    @POST
    @Path("/add")
    @Blocking
    public Uni<Response> addNetworkToSwitch(AddNetworkRequest request) {
        return Uni.createFrom()
                .item(() -> {
                    Switch networkSwitch = locateSwitch(
                            request.getEdgeRouterId(), request.getSwitchId());
                    if (networkSwitch == null) {
                        return null;
                    }
                    Network network = networkManagementUseCase.createNetwork(
                            IP.fromAddress(request.getNetworkAddress()),
                            request.getNetworkName(),
                            request.getNetworkCidr());
                    return networkManagementUseCase.addNetworkToSwitch(network, networkSwitch);
                })
                .onItem().transform(networkSwitch -> networkSwitch != null
                        ? Response.ok(SwitchResponse.from(networkSwitch))
                        : Response.status(Response.Status.NOT_FOUND))
                .onItem().transform(Response.ResponseBuilder::build);
    }

    /**
     * {@code POST /network/remove} — quita una red (por nombre) de un switch (alcanzado por edge + switch id).
     *
     * @return {@code 200} con el switch resultante, o {@code 404} si el edge, el switch o la red no resuelven
     */
    @POST
    @Path("/remove")
    @Blocking
    public Uni<Response> removeNetworkFromSwitch(RemoveNetworkRequest request) {
        return Uni.createFrom()
                .item(() -> {
                    Switch networkSwitch = locateSwitch(
                            request.getEdgeRouterId(), request.getSwitchId());
                    if (networkSwitch == null) {
                        return null;
                    }
                    Network network = networkSwitch.getSwitchNetworks().stream()
                            .filter(Network.getNetworkNamePredicate(request.getNetworkName()))
                            .findFirst()
                            .orElse(null);
                    if (network == null) {
                        return null;
                    }
                    return networkManagementUseCase.removeNetworkFromSwitch(network, networkSwitch);
                })
                .onItem().transform(networkSwitch -> networkSwitch != null
                        ? Response.ok(SwitchResponse.from(networkSwitch))
                        : Response.status(Response.Status.NOT_FOUND))
                .onItem().transform(Response.ResponseBuilder::build);
    }

    /**
     * Localiza un switch dentro de un edge router recuperado por su id. Devuelve {@code null}
     * si el router no existe, no es un edge, o no contiene ese switch. Es una búsqueda en el
     * agregado ya materializado, no lógica de dominio.
     */
    private Switch locateSwitch(String edgeRouterId, String switchId) {
        Router router = routerManagementUseCase.retrieveRouter(Id.withId(edgeRouterId));
        if (!(router instanceof EdgeRouter edgeRouter)) {
            return null;
        }
        return edgeRouter.getSwitches().get(Id.withId(switchId));
    }
}
