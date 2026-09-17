package com.example.topologyinventory.framework.adapters.output.h2.mappers;

import com.example.topologyinventory.domain.entity.CoreRouter;
import com.example.topologyinventory.domain.entity.EdgeRouter;
import com.example.topologyinventory.domain.entity.Router;
import com.example.topologyinventory.domain.entity.Switch;
import com.example.topologyinventory.domain.vo.*;
import com.example.topologyinventory.framework.adapters.output.h2.data.*;

import java.util.*;

/**
 * Traductor entre el modelo de dominio y el modelo de persistencia (las clases {@code *Data}).
 * Es el "peaje" de la frontera: nada del mundo de la base entra al núcleo sin pasar por aquí.
 *
 * <p><b>Reconstrucción por profundidad (Fase 8).</b> Bajo Hibernate Reactive las colecciones
 * lazy se inician en el adapter con {@code session.fetch}; este mapper es puramente en memoria y
 * reconstruye a la profundidad que el adapter ya materializó:
 * <ul>
 *   <li>{@link #routerRootToDomain} — solo escalares, sin hijos (no navega ninguna colección);</li>
 *   <li>{@link #coreWithChildren} — un CORE con sus routers hijos ya fetchados, cada hijo
 *       superficial (basta su id para la respuesta);</li>
 *   <li>{@link #edgeWithSwitches} — un EDGE con sus switches ya fetchados, cada uno con sus redes
 *       ya fetchadas.</li>
 * </ul>
 *
 * <p><b>Escritura profunda (SC2).</b> En dominio → data, un router hijo de un core recibe ahora su
 * {@code routerParentCoreId} (el id del core): sin ese escalar self-FK, la fila del hijo se
 * persistiría suelta y {@code retrieveRouter} —que reencuentra los hijos leyendo
 * {@code router_parent_core_id}— nunca los volvería a ver. Las FK de switch ({@code routerId}) y
 * de red ({@code switchId}) ya se rellenaban desde SC1.
 */
public class RouterH2Mapper {

    // ---------------------------------------------------------------------
    // data -> domain (reconstrucción por profundidad)
    // ---------------------------------------------------------------------

    /** Reconstruye la raíz con solo sus escalares, sin hijos. No toca colecciones lazy. */
    public static Router routerRootToDomain(RouterData routerData) {
        var id = Id.withId(routerData.getRouterId());
        var vendor = Vendor.valueOf(routerData.getRouterVendor().toString());
        var model = Model.valueOf(routerData.getRouterModel().toString());
        var ip = IP.fromAddress(routerData.getIp().getAddress());
        var location = locationDataToLocation(routerData.getRouterLocation());
        var routerType = RouterType.valueOf(routerData.getRouterType().name());

        if (routerData.getRouterType() == RouterTypeData.CORE) {
            return CoreRouter.builder()
                    .id(id).vendor(vendor).model(model).ip(ip).location(location)
                    .routerType(routerType)
                    .routers(new HashMap<>())
                    .build();
        }
        return EdgeRouter.builder()
                .id(id).vendor(vendor).model(model).ip(ip).location(location)
                .routerType(routerType)
                .switches(new HashMap<>())
                .build();
    }

    /** Reconstruye un CORE con sus routers hijos (ya fetchados), cada hijo en forma superficial. */
    public static Router coreWithChildren(RouterData coreData, List<RouterData> fetchedChildren) {
        var core = (CoreRouter) routerRootToDomain(coreData);
        if (fetchedChildren != null) {
            for (RouterData child : fetchedChildren) {
                core.getRouters().put(
                        Id.withId(child.getRouterId()),
                        routerRootToDomain(child));
            }
        }
        return core;
    }

    /** Reconstruye un EDGE con sus switches (ya fetchados, con sus redes ya fetchadas). */
    public static Router edgeWithSwitches(RouterData edgeData, List<SwitchData> fetchedSwitches) {
        var edge = (EdgeRouter) routerRootToDomain(edgeData);
        if (fetchedSwitches != null) {
            for (SwitchData switchData : fetchedSwitches) {
                edge.getSwitches().put(
                        Id.withId(switchData.getSwitchId()),
                        switchDataToDomain(switchData));
            }
        }
        return edge;
    }

    private static Switch switchDataToDomain(SwitchData switchData) {
        return Switch.builder()
                .id(Id.withId(switchData.getSwitchId()))
                .vendor(Vendor.valueOf(switchData.getSwitchVendor().toString()))
                .model(Model.valueOf(switchData.getSwitchModel().toString()))
                .ip(IP.fromAddress(switchData.getIp().getAddress()))
                .location(locationDataToLocation(switchData.getSwitchLocation()))
                .switchType(SwitchType.valueOf(switchData.getSwitchType().toString()))
                .switchNetworks(getNetworksFromData(switchData.getNetworks()))
                .build();
    }

    // ---------------------------------------------------------------------
    // domain -> data (en memoria)
    // ---------------------------------------------------------------------

    /** Traduce un router (raíz del agregado) a su modelo de persistencia, con sus hijos. */
    public static RouterData routerDomainToData(Router router) {
        return routerDomainToData(router, null);
    }

    /**
     * Traduce un router a {@code *Data} asignándole {@code parentCoreId} como self-FK. Es
     * {@code null} para la raíz y el id del core para cada router hijo, de modo que la fila del
     * hijo enlace con su core al persistirse.
     */
    private static RouterData routerDomainToData(Router router, String parentCoreId) {
        var routerData = RouterData.builder()
                .routerId(router.getId().getId().toString())
                .routerParentCoreId(parentCoreId)
                .routerVendor(VendorData.valueOf(router.getVendor().toString()))
                .routerModel(ModelData.valueOf(router.getModel().toString()))
                .ip(IPData.fromAddress(router.getIp().getIpAddress()))
                .routerLocation(locationDomainToLocationData(router.getLocation()))
                .routerType(RouterTypeData.valueOf(router.getRouterType().toString()))
                .build();

        if (router.getRouterType().equals(RouterType.CORE)) {
            var coreRouter = (CoreRouter) router;
            routerData.setRouters(getRoutersFromDomain(coreRouter.getRouters(), routerData.getRouterId()));
        } else {
            var edgeRouter = (EdgeRouter) router;
            routerData.setSwitches(
                    getSwitchesFromDomain(edgeRouter.getSwitches(), router.getId().getId().toString()));
        }
        return routerData;
    }

    private static SwitchData switchDomainToData(Switch aSwitch, String routerId) {
        return SwitchData.builder()
                .switchId(aSwitch.getId().getId().toString())
                .routerId(routerId)
                .switchVendor(VendorData.valueOf(aSwitch.getVendor().toString()))
                .switchModel(ModelData.valueOf(aSwitch.getModel().toString()))
                .ip(IPData.fromAddress(aSwitch.getIp().getIpAddress()))
                .switchLocation(locationDomainToLocationData(aSwitch.getLocation()))
                .switchType(SwitchTypeData.valueOf(aSwitch.getSwitchType().toString()))
                .networks(getNetworksFromDomain(aSwitch.getSwitchNetworks(), aSwitch.getId().getId().toString()))
                .build();
    }

    // ---------------------------------------------------------------------
    // helpers de value objects
    // ---------------------------------------------------------------------

    public static Location locationDataToLocation(LocationData locationData) {
        return Location.builder()
                .address(locationData.getAddress())
                .city(locationData.getCity())
                .state(locationData.getState())
                .zipCode(locationData.getZipcode())
                .country(locationData.getCountry())
                .latitude(locationData.getLatitude())
                .longitude(locationData.getLongitude())
                .build();
    }

    public static LocationData locationDomainToLocationData(Location location) {
        return LocationData.builder()
                .address(location.getAddress())
                .city(location.getCity())
                .state(location.getState())
                .zipcode(location.getZipCode())
                .country(location.getCountry())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .build();
    }

    private static List<RouterData> getRoutersFromDomain(Map<Id, Router> routers, String parentCoreId) {
        List<RouterData> routerDataList = new ArrayList<>();
        if (routers != null) {
            routers.values().forEach(router -> routerDataList.add(routerDomainToData(router, parentCoreId)));
        }
        return routerDataList;
    }

    private static List<SwitchData> getSwitchesFromDomain(Map<Id, Switch> switches, String routerId) {
        List<SwitchData> switchDataList = new ArrayList<>();
        if (switches != null) {
            switches.values().forEach(aSwitch -> switchDataList.add(switchDomainToData(aSwitch, routerId)));
        }
        return switchDataList;
    }

    private static List<Network> getNetworksFromData(List<NetworkData> networkData) {
        List<Network> networks = new ArrayList<>();
        if (networkData != null) {
            networkData.forEach(data -> networks.add(new Network(
                    IP.fromAddress(data.getIp().getAddress()),
                    data.getName(),
                    data.getCidr())));
        }
        return networks;
    }

    private static List<NetworkData> getNetworksFromDomain(List<Network> networks, String switchId) {
        List<NetworkData> networkDataList = new ArrayList<>();
        if (networks != null) {
            networks.forEach(network -> networkDataList.add(new NetworkData(
                    switchId,
                    IPData.fromAddress(network.getNetworkAddress().getIpAddress()),
                    network.getNetworkName(),
                    network.getNetworkCidr())));
        }
        return networkDataList;
    }
}
