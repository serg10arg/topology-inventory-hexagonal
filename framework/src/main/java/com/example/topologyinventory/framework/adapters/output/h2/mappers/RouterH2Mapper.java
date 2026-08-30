package com.example.topologyinventory.framework.adapters.output.h2.mappers;

import com.example.topologyinventory.domain.entity.CoreRouter;
import com.example.topologyinventory.domain.entity.EdgeRouter;
import com.example.topologyinventory.domain.entity.Router;
import com.example.topologyinventory.domain.entity.Switch;
import com.example.topologyinventory.domain.vo.*;
import com.example.topologyinventory.framework.adapters.output.h2.data.*;

import java.util.*;

/**
 * Traductor entre el modelo de dominio y el modelo de persistencia (las clases
 * {@code *Data}). Es el "peaje" de la frontera: nada del mundo de la base de
 * datos entra al núcleo sin pasar por aquí, y ninguna entidad de dominio se
 * persiste sin convertirse antes en su espejo {@code *Data}.
 *
 * Reconstruye los routers con sus builders ({@code CoreRouter}/{@code EdgeRouter}),
 * respetando que las entidades del dominio son inmutables (sin setters). Como el
 * {@code Switch} del dominio no guarda el id de su router, al persistir se le
 * pasa el id del edge router padre (la raíz del agregado).
 */
public class RouterH2Mapper {

    public static Router routerDataToDomain(RouterData routerData) {
        var id = Id.withId(routerData.getRouterId().toString());
        var vendor = Vendor.valueOf(routerData.getRouterVendor().toString());
        var model = Model.valueOf(routerData.getRouterModel().toString());
        var ip = IP.fromAddress(routerData.getIp().getAddress());
        var location = locationDataToLocation(routerData.getRouterLocation());
        var routerType = RouterType.valueOf(routerData.getRouterType().name());

        if (routerData.getRouterType().equals(RouterTypeData.CORE)) {
            return CoreRouter.builder()
                    .id(id).vendor(vendor).model(model).ip(ip).location(location)
                    .routerType(routerType)
                    .routers(getRoutersFromData(routerData.getRouters()))
                    .build();
        }
        return EdgeRouter.builder()
                .id(id).vendor(vendor).model(model).ip(ip).location(location)
                .routerType(routerType)
                .switches(getSwitchesFromData(routerData.getSwitches()))
                .build();
    }

    public static RouterData routerDomainToData(Router router) {
        var routerData = RouterData.builder()
                .routerId(router.getId().getId())
                .routerVendor(VendorData.valueOf(router.getVendor().toString()))
                .routerModel(ModelData.valueOf(router.getModel().toString()))
                .ip(IPData.fromAddress(router.getIp().getIpAddress()))
                .routerLocation(locationDomainToLocationData(router.getLocation()))
                .routerType(RouterTypeData.valueOf(router.getRouterType().toString()))
                .build();

        if (router.getRouterType().equals(RouterType.CORE)) {
            var coreRouter = (CoreRouter) router;
            routerData.setRouters(getRoutersFromDomain(coreRouter.getRouters()));
        } else {
            var edgeRouter = (EdgeRouter) router;
            routerData.setSwitches(
                    getSwitchesFromDomain(edgeRouter.getSwitches(), router.getId().getId()));
        }
        return routerData;
    }

    private static Switch switchDataToDomain(SwitchData switchData) {
        return Switch.builder()
                .id(Id.withId(switchData.getSwitchId().toString()))
                .vendor(Vendor.valueOf(switchData.getSwitchVendor().toString()))
                .model(Model.valueOf(switchData.getSwitchModel().toString()))
                .ip(IP.fromAddress(switchData.getIp().getAddress()))
                .location(locationDataToLocation(switchData.getSwitchLocation()))
                .switchType(SwitchType.valueOf(switchData.getSwitchType().toString()))
                .switchNetworks(getNetworksFromData(switchData.getNetworks()))
                .build();
    }

    private static SwitchData switchDomainToData(Switch aSwitch, UUID routerId) {
        return SwitchData.builder()
                .switchId(aSwitch.getId().getId())
                .routerId(routerId)
                .switchVendor(VendorData.valueOf(aSwitch.getVendor().toString()))
                .switchModel(ModelData.valueOf(aSwitch.getModel().toString()))
                .ip(IPData.fromAddress(aSwitch.getIp().getIpAddress()))
                .switchLocation(locationDomainToLocationData(aSwitch.getLocation()))
                .switchType(SwitchTypeData.valueOf(aSwitch.getSwitchType().toString()))
                .networks(getNetworksFromDomain(aSwitch.getSwitchNetworks(), aSwitch.getId().getId()))
                .build();
    }

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

    private static Map<Id, Router> getRoutersFromData(List<RouterData> routerDataList) {
        Map<Id, Router> routerMap = new HashMap<>();
        if (routerDataList != null) {
            for (RouterData routerData : routerDataList) {
                routerMap.put(
                        Id.withId(routerData.getRouterId().toString()),
                        routerDataToDomain(routerData));
            }
        }
        return routerMap;
    }

    private static List<RouterData> getRoutersFromDomain(Map<Id, Router> routers) {
        List<RouterData> routerDataList = new ArrayList<>();
        if (routers != null) {
            routers.values().forEach(router -> routerDataList.add(routerDomainToData(router)));
        }
        return routerDataList;
    }

    private static Map<Id, Switch> getSwitchesFromData(List<SwitchData> switchDataList) {
        Map<Id, Switch> switchMap = new HashMap<>();
        if (switchDataList != null) {
            for (SwitchData switchData : switchDataList) {
                switchMap.put(
                        Id.withId(switchData.getSwitchId().toString()),
                        switchDataToDomain(switchData));
            }
        }
        return switchMap;
    }

    private static List<SwitchData> getSwitchesFromDomain(Map<Id, Switch> switches, UUID routerId) {
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

    private static List<NetworkData> getNetworksFromDomain(List<Network> networks, UUID switchId) {
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