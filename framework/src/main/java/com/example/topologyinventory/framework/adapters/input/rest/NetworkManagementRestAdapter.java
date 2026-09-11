package com.example.topologyinventory.framework.adapters.input.rest;

import com.example.topologyinventory.application.usecases.NetworkManagementUseCase;
import com.example.topologyinventory.application.usecases.RouterManagementUseCase;
import com.example.topologyinventory.domain.entity.EdgeRouter;
import com.example.topologyinventory.domain.entity.Switch;
import com.example.topologyinventory.domain.vo.IP;
import com.example.topologyinventory.domain.vo.Id;
import com.example.topologyinventory.domain.vo.Network;
import com.example.topologyinventory.framework.adapters.input.rest.request.AddNetworkRequest;
import com.example.topologyinventory.framework.adapters.input.rest.request.CreateNetworkRequest;
import com.example.topologyinventory.framework.adapters.input.rest.request.RemoveNetworkRequest;
import com.example.topologyinventory.framework.adapters.input.rest.response.NetworkResponse;
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
 * Adapter de entrada REST (<em>driving</em>) para la gestión de redes.
 *
 * <p><b>Divergencia (sin cambios de fondo).</b> Una red vive dentro de un switch, y el switch
 * dentro de un edge router; ninguno se recupera de forma independiente. {@code create} opera en
 * memoria; {@code add}/{@code remove} recuperan el edge, localizan el switch y mutan en memoria.
 *
 * <p><b>Reactivo (Fase 8).</b> La localización del switch pasa por el {@code retrieveRouter}
 * reactivo, así que {@code add}/{@code remove} componen sobre su {@link Uni} sin {@code @Blocking}.
 * El switch recuperado trae sus redes materializadas (fetch profundo del output adapter), por lo
 * que la búsqueda de red por nombre en {@code remove} opera sobre datos ya cargados.
 */
@ApplicationScoped
@Path("/network")
@Tag(name = "Network Operations", description = "Alta, baja y conexión de redes a switches")
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
    @Operation(operationId = "createNetwork", summary = "Crea una red (en memoria)")
    public Uni<Response> createNetwork(CreateNetworkRequest request) {
        var network = networkManagementUseCase.createNetwork(
                IP.fromAddress(request.getNetworkAddress()),
                request.getNetworkName(),
                request.getNetworkCidr());
        return Uni.createFrom().item(Response.ok(NetworkResponse.from(network)).build());
    }

    /**
     * {@code POST /network/add} — crea una red y la añade a un switch (edge + switch id).
     *
     * @return {@code 200} con el switch resultante, o {@code 404} si el edge o el switch no resuelven
     */
    @POST
    @Path("/add")
    @Operation(operationId = "addNetworkToSwitch", summary = "Crea una red y la añade a un switch")
    public Uni<Response> addNetworkToSwitch(AddNetworkRequest request) {
        return locateSwitch(request.getEdgeRouterId(), request.getSwitchId())
                .onItem().transform(networkSwitch -> {
                    if (networkSwitch == null) {
                        return Response.status(Response.Status.NOT_FOUND).build();
                    }
                    Network network = networkManagementUseCase.createNetwork(
                            IP.fromAddress(request.getNetworkAddress()),
                            request.getNetworkName(),
                            request.getNetworkCidr());
                    Switch result = networkManagementUseCase.addNetworkToSwitch(network, networkSwitch);
                    return Response.ok(SwitchResponse.from(result)).build();
                });
    }

    /**
     * {@code POST /network/remove} — quita una red (por nombre) de un switch (edge + switch id).
     *
     * @return {@code 200} con el switch resultante, o {@code 404} si el edge, el switch o la red no resuelven
     */
    @POST
    @Path("/remove")
    @Operation(operationId = "removeNetworkFromSwitch", summary = "Quita una red de un switch")
    public Uni<Response> removeNetworkFromSwitch(RemoveNetworkRequest request) {
        return locateSwitch(request.getEdgeRouterId(), request.getSwitchId())
                .onItem().transform(networkSwitch -> {
                    if (networkSwitch == null) {
                        return Response.status(Response.Status.NOT_FOUND).build();
                    }
                    Network network = networkSwitch.getSwitchNetworks().stream()
                            .filter(Network.getNetworkNamePredicate(request.getNetworkName()))
                            .findFirst()
                            .orElse(null);
                    if (network == null) {
                        return Response.status(Response.Status.NOT_FOUND).build();
                    }
                    Switch result = networkManagementUseCase.removeNetworkFromSwitch(network, networkSwitch);
                    return Response.ok(SwitchResponse.from(result)).build();
                });
    }

    /**
     * Localiza un switch dentro de un edge router recuperado por su id, de forma reactiva. Emite
     * {@code null} si el router no existe, no es un edge, o no contiene ese switch. Es una búsqueda
     * en el agregado ya materializado, no lógica de dominio.
     */
    private Uni<Switch> locateSwitch(String edgeRouterId, String switchId) {
        return routerManagementUseCase.retrieveRouter(Id.withId(edgeRouterId))
                .onItem().transform(router -> {
                    if (!(router instanceof EdgeRouter edgeRouter)) {
                        return null;
                    }
                    return edgeRouter.getSwitches().get(Id.withId(switchId));
                });
    }
}
