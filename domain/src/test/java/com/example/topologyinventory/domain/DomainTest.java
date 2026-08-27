package com.example.topologyinventory.domain;

import com.example.topologyinventory.domain.entity.CoreRouter;
import com.example.topologyinventory.domain.entity.EdgeRouter;
import com.example.topologyinventory.domain.entity.Router;
import com.example.topologyinventory.domain.entity.Switch;
import com.example.topologyinventory.domain.exception.GenericSpecificationException;
import com.example.topologyinventory.domain.service.NetworkService;
import com.example.topologyinventory.domain.service.RouterService;
import com.example.topologyinventory.domain.service.SwitchService;
import com.example.topologyinventory.domain.vo.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests unitarios del Domain hexagon. Ejercitan las operaciones de negocio
 * (añadir, quitar, filtrar y buscar) usando únicamente componentes del dominio,
 * sin dependencias de otros hexágonos. Cada operación de mutación se prueba en su
 * camino feliz y, cuando aplica, en su camino de error para verificar que las
 * specifications rechazan lo que deben.
 */
public class DomainTest {

    // ---------- Alta de redes en un switch ----------

    /** Camino feliz: se añade una red nueva a un switch. */
    @Test
    public void addNetworkToSwitch() {
        var location = createLocation("US");
        var newNetwork = createTestNetwork("30.0.0.1", 8);
        var networkSwitch = createSwitch("30.0.0.0", 8, location);
        assertTrue(networkSwitch.addNetworkToSwitch(newNetwork));
    }

    /** Camino de error: añadir una red que ya existe debe lanzar excepción. */
    @Test
    public void addNetworkToSwitch_failBecauseSameNetworkAddress() {
        var location = createLocation("US");
        var newNetwork = createTestNetwork("30.0.0.0", 8);
        var networkSwitch = createSwitch("30.0.0.0", 8, location);
        assertThrows(GenericSpecificationException.class,
                () -> networkSwitch.addNetworkToSwitch(newNetwork));
    }

    // ---------- Conexión de switches a un edge router ----------

    /** Camino feliz: un switch del mismo país se conecta al edge router. */
    @Test
    public void addSwitchToEdgeRouter() {
        var location = createLocation("US");
        var networkSwitch = createSwitch("30.0.0.0", 8, location);
        var edgeRouter = createEdgeRouter(location, "30.0.0.1");

        edgeRouter.addSwitch(networkSwitch);

        assertEquals(1, edgeRouter.getSwitches().size());
    }

    /** Camino de error: un switch de otro país no puede conectarse. */
    @Test
    public void addSwitchToEdgeRouter_failBecauseEquipmentOfDifferentCountries() {
        var locationUS = createLocation("US");
        var locationJP = createLocation("JP");
        var networkSwitch = createSwitch("30.0.0.0", 8, locationUS);
        var edgeRouter = createEdgeRouter(locationJP, "30.0.0.1");

        assertThrows(GenericSpecificationException.class,
                () -> edgeRouter.addSwitch(networkSwitch));
    }

    // ---------- Conexión de routers a un core router ----------

    /** Camino feliz: un edge router del mismo país se conecta al core router. */
    @Test
    public void addEdgeToCoreRouter() {
        var location = createLocation("US");
        var edgeRouter = createEdgeRouter(location, "30.0.0.1");
        var coreRouter = createCoreRouter(location, "40.0.0.1");

        coreRouter.addRouter(edgeRouter);

        assertEquals(1, coreRouter.getRouters().size());
    }

    /** Camino de error: edge y core en países distintos no pueden conectarse. */
    @Test
    public void addEdgeToCoreRouter_failBecauseRoutersOfDifferentCountries() {
        var locationUS = createLocation("US");
        var locationJP = createLocation("JP");
        var edgeRouter = createEdgeRouter(locationUS, "30.0.0.1");
        var coreRouter = createCoreRouter(locationJP, "40.0.0.1");

        assertThrows(GenericSpecificationException.class,
                () -> coreRouter.addRouter(edgeRouter));
    }

    /** Camino feliz: dos core routers pueden interconectarse. */
    @Test
    public void addCoreToCoreRouter() {
        var location = createLocation("US");
        var coreRouter = createCoreRouter(location, "30.0.0.1");
        var newCoreRouter = createCoreRouter(location, "40.0.0.1");

        coreRouter.addRouter(newCoreRouter);

        assertEquals(1, coreRouter.getRouters().size());
    }

    /** Camino de error: dos routers con la misma IP no pueden conectarse. */
    @Test
    public void addCoreToCoreRouter_failBecauseRoutersOfSameIp() {
        var location = createLocation("US");
        var coreRouter = createCoreRouter(location, "30.0.0.1");
        var newCoreRouter = createCoreRouter(location, "30.0.0.1");

        assertThrows(GenericSpecificationException.class,
                () -> coreRouter.addRouter(newCoreRouter));
    }

    // ---------- Eliminación ----------

    /** Se elimina un edge router de un core router y se recupera su Id. */
    @Test
    public void removeRouter() {
        var location = createLocation("US");
        var coreRouter = createCoreRouter(location, "30.0.0.1");
        var edgeRouter = createEdgeRouter(location, "40.0.0.1");
        var expectedId = edgeRouter.getId();

        coreRouter.addRouter(edgeRouter);
        var actualId = coreRouter.removeRouter(edgeRouter).getId();

        assertEquals(expectedId, actualId);
    }

    /** Se elimina un switch (previamente vaciado de redes) de un edge router. */
    @Test
    public void removeSwitch() {
        var location = createLocation("US");
        var network = createTestNetwork("30.0.0.0", 8);
        var networkSwitch = createSwitch("30.0.0.0", 8, location);
        var edgeRouter = createEdgeRouter(location, "40.0.0.1");

        edgeRouter.addSwitch(networkSwitch);
        networkSwitch.removeNetworkFromSwitch(network);
        var expectedId = Id.withId("f8c3de3d-1fea-4d7c-a8b0-29f63c4c3490");
        var actualId = edgeRouter.removeSwitch(networkSwitch).getId();

        assertEquals(expectedId, actualId);
    }

    /** Se elimina una red de un switch; la colección queda vacía. */
    @Test
    public void removeNetwork() {
        var location = createLocation("US");
        var network = createTestNetwork("30.0.0.0", 8);
        var networkSwitch = createSwitch("30.0.0.0", 8, location);

        assertEquals(1, networkSwitch.getSwitchNetworks().size());
        assertTrue(networkSwitch.removeNetworkFromSwitch(network));
        assertEquals(0, networkSwitch.getSwitchNetworks().size());
    }

    // ---------- Filtrado y búsqueda (domain services + predicates) ----------

    /** Filtra routers por tipo (CORE / EDGE) vía RouterService. */
    @Test
    public void filterRouterByType() {
        List<Router> routers = new ArrayList<>();
        var location = createLocation("US");
        var coreRouter = createCoreRouter(location, "30.0.0.1");
        var edgeRouter = createEdgeRouter(location, "40.0.0.1");

        routers.add(coreRouter);
        routers.add(edgeRouter);

        var coreRouters = RouterService.filterAndRetrieveRouter(routers,
                Router.getRouterTypePredicate(RouterType.CORE));
        assertEquals(RouterType.CORE, coreRouters.get(0).getRouterType());

        var edgeRouters = RouterService.filterAndRetrieveRouter(routers,
                Router.getRouterTypePredicate(RouterType.EDGE));
        assertEquals(RouterType.EDGE, edgeRouters.get(0).getRouterType());
    }

    /** Filtra routers por fabricante (HP / CISCO). */
    @Test
    public void filterRouterByVendor() {
        List<Router> routers = new ArrayList<>();
        var location = createLocation("US");
        var coreRouter = createCoreRouter(location, "30.0.0.1");
        var edgeRouter = createEdgeRouter(location, "40.0.0.1");

        routers.add(coreRouter);
        routers.add(edgeRouter);

        var actualVendor = RouterService.filterAndRetrieveRouter(routers,
                Router.getVendorPredicate(Vendor.HP)).get(0).getVendor();
        assertEquals(Vendor.HP, actualVendor);

        actualVendor = RouterService.filterAndRetrieveRouter(routers,
                Router.getVendorPredicate(Vendor.CISCO)).get(0).getVendor();
        assertEquals(Vendor.CISCO, actualVendor);
    }

    /** Filtra routers por país. */
    @Test
    public void filterRouterByLocation() {
        List<Router> routers = new ArrayList<>();
        var location = createLocation("US");
        var coreRouter = createCoreRouter(location, "30.0.0.1");

        routers.add(coreRouter);

        var actualCountry = RouterService.filterAndRetrieveRouter(routers,
                Router.getCountryPredicate(location)).get(0).getLocation().getCountry();
        assertEquals(location.getCountry(), actualCountry);
    }

    /** Filtra routers por modelo. */
    @Test
    public void filterRouterByModel() {
        List<Router> routers = new ArrayList<>();
        var location = createLocation("US");
        var coreRouter = createCoreRouter(location, "30.0.0.1");
        var newCoreRouter = createCoreRouter(location, "40.0.0.1");

        coreRouter.addRouter(newCoreRouter);
        routers.add(coreRouter);

        var actualModel = RouterService.filterAndRetrieveRouter(routers,
                Router.getModelPredicate(Model.XYZ0001)).get(0).getModel();
        assertEquals(Model.XYZ0001, actualModel);
    }

    /** Filtra switches por tipo (LAYER3) vía SwitchService. */
    @Test
    public void filterSwitchByType() {
        List<Switch> switches = new ArrayList<>();
        var location = createLocation("US");
        var networkSwitch = createSwitch("30.0.0.0", 8, location);

        switches.add(networkSwitch);

        var actualSwitchType = SwitchService.filterAndRetrieveSwitch(switches,
                Switch.getSwitchTypePredicate(SwitchType.LAYER3)).get(0).getSwitchType();
        assertEquals(SwitchType.LAYER3, actualSwitchType);
    }

    /** Filtra redes por protocolo (IPv4) vía NetworkService. */
    @Test
    public void filterNetworkByProtocol() {
        var testNetwork = createTestNetwork("30.0.0.0", 8);
        var networks = createNetworks(testNetwork);

        var actualProtocol = NetworkService.filterAndRetrieveNetworks(networks,
                Switch.getNetworkProtocolPredicate(Protocol.IPV4)).get(0).getNetworkAddress().getProtocol();
        assertEquals(Protocol.IPV4, actualProtocol);
    }

    /** Busca un router por Id vía RouterService.findById. */
    @Test
    public void findRouterById() {
        Map<Id, Router> routersOfCoreRouter = new HashMap<>();
        var location = createLocation("US");
        var coreRouter = createCoreRouter(location, "30.0.0.1");
        var newCoreRouter = createCoreRouter(location, "40.0.0.1");

        coreRouter.addRouter(newCoreRouter);
        routersOfCoreRouter.put(newCoreRouter.getId(), newCoreRouter);

        var expectedId = newCoreRouter.getId();
        var actualId = RouterService.findById(routersOfCoreRouter, expectedId).getId();
        assertEquals(expectedId, actualId);
    }

    /** Busca un switch por Id vía SwitchService.findById. */
    @Test
    public void findSwitchById() {
        Map<Id, Switch> switchesOfEdgeRouter = new HashMap<>();
        var location = createLocation("US");
        var networkSwitch = createSwitch("30.0.0.0", 8, location);

        switchesOfEdgeRouter.put(networkSwitch.getId(), networkSwitch);

        var expectedId = Id.withId("f8c3de3d-1fea-4d7c-a8b0-29f63c4c3490");
        var actualId = SwitchService.findById(switchesOfEdgeRouter, expectedId).getId();
        assertEquals(expectedId, actualId);
    }

    // ================= Helpers de construcción de datos de prueba =================

    /** Crea una red de prueba con nombre fijo. */
    private Network createTestNetwork(String address, int cidr) {
        return Network.builder()
                .networkAddress(IP.fromAddress(address))
                .networkName("NewNetwork")
                .networkCidr(cidr)
                .build();
    }

    /** Crea una ubicación de prueba en el país indicado. */
    private Location createLocation(String country) {
        return Location.builder()
                .address("Test street")
                .city("Test City")
                .state("Test State")
                .country(country)
                .zipCode(0)
                .latitude(10F)
                .longitude(-10F)
                .build();
    }

    /** Envuelve una red en una lista mutable. */
    private List<Network> createNetworks(Network network) {
        List<Network> networks = new ArrayList<>();
        networks.add(network);
        return networks;
    }

    /** Crea un switch ya provisto con una red en la dirección/CIDR dados. */
    private Switch createSwitch(String address, int cidr, Location location) {
        var newNetwork = createTestNetwork(address, cidr);
        var networks = createNetworks(newNetwork);
        return createNetworkSwitch(location, networks);
    }

    /** Crea un switch LAYER3 con Id fijo y las redes indicadas. */
    private Switch createNetworkSwitch(Location location, List<Network> networks) {
        return Switch.builder()
                .id(Id.withId("f8c3de3d-1fea-4d7c-a8b0-29f63c4c3490"))
                .vendor(Vendor.CISCO)
                .model(Model.XYZ0004)
                .ip(IP.fromAddress("20.0.0.100"))
                .location(location)
                .switchType(SwitchType.LAYER3)
                .switchNetworks(networks)
                .build();
    }

    /** Crea un edge router (CISCO/XYZ0002) sin switches. */
    private EdgeRouter createEdgeRouter(Location location, String address) {
        Map<Id, Switch> switchesOfEdgeRouter = new HashMap<>();
        return EdgeRouter.builder()
                .id(Id.withoutId())
                .vendor(Vendor.CISCO)
                .model(Model.XYZ0002)
                .ip(IP.fromAddress(address))
                .location(location)
                .routerType(RouterType.EDGE)
                .switches(switchesOfEdgeRouter)
                .build();
    }

    /** Crea un core router (HP/XYZ0001) sin routers. */
    private CoreRouter createCoreRouter(Location location, String address) {
        Map<Id, Router> routersOfCoreRouter = new HashMap<>();
        return CoreRouter.builder()
                .id(Id.withoutId())
                .vendor(Vendor.HP)
                .model(Model.XYZ0001)
                .ip(IP.fromAddress(address))
                .location(location)
                .routerType(RouterType.CORE)
                .routers(routersOfCoreRouter)
                .build();
    }
}