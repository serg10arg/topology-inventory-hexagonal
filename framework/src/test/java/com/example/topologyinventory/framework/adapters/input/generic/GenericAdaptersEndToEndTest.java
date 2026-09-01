package com.example.topologyinventory.framework.adapters.input.generic;

import com.example.topologyinventory.domain.entity.CoreRouter;
import com.example.topologyinventory.domain.entity.EdgeRouter;
import com.example.topologyinventory.domain.vo.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test end-to-end del lado <em>driving</em>: entra por los generic adapters (la
 * "puerta de entrada" del sistema, base del futuro adapter REST) y recorre la
 * jerarquía completa del agregado —core → edge → switch → red— más el camino de
 * persistencia, que atraviesa los tres hexágonos: adapter de entrada → use case →
 * puerto de salida resuelto por {@code ServiceLoader} → output adapter de H2.
 *
 * <p>Los fixtures respetan las reglas de dominio (specifications): misma
 * ubicación (mismo país) en toda la cadena, IPs distintas entre core, edge y
 * switch, y un CIDR válido (&gt;= 8) para la red. Un dato que violara una regla
 * haría fallar el test por el motivo equivocado.
 *
 * <p>Sobre el paso de persistencia: no se hace round-trip de un router recién
 * persistido, por la misma razón documentada en {@code RouterManagementH2AdapterTest}
 * —el mapper no asigna {@code locationId}, así que releerlo fallaría al resolver la
 * ubicación—. Escritura y lectura se ejercitan por separado: se persiste un router
 * nuevo y se recupera uno semilla.
 */
class GenericAdaptersEndToEndTest {

    /** EDGE router semilla insertado por inventory.sql. */
    private static final String SEEDED_EDGE_ROUTER_ID = "b832ef4f-f894-4194-8feb-a99c2cd4be0a";

    private static final Location LOCATION = Location.builder()
            .address("Amos Ln").city("Tully").state("NY").zipCode(13159)
            .country("United States").latitude(42.79731f).longitude(-76.13075f)
            .build();

    @Test
    @DisplayName("e2e: crear y conectar la jerarquía core → edge → switch → red")
    void createAndConnectHierarchy() {
        var routerAdapter = new RouterManagementGenericAdapter();
        var switchAdapter = new SwitchManagementGenericAdapter();
        var networkAdapter = new NetworkManagementGenericAdapter();

        // core ← edge
        var core = (CoreRouter) routerAdapter.createRouter(
                Vendor.CISCO, Model.XYZ0001, IP.fromAddress("1.0.0.1"),
                LOCATION, RouterType.CORE);
        var edge = (EdgeRouter) routerAdapter.createRouter(
                Vendor.JUNIPER, Model.XYZ0001, IP.fromAddress("5.0.0.5"),
                LOCATION, RouterType.EDGE);
        var coreWithEdge = routerAdapter.addRouterToCoreRouter(edge, core);
        assertEquals(1, coreWithEdge.getRouters().size(),
                "el core debería tener 1 router conectado");

        // edge ← switch
        var networkSwitch = switchAdapter.createSwitch(
                Vendor.CISCO, Model.XYZ0002, IP.fromAddress("9.0.0.9"),
                LOCATION, SwitchType.LAYER3);
        var edgeWithSwitch = switchAdapter.addSwitchToEdgeRouter(networkSwitch, edge);
        assertEquals(1, edgeWithSwitch.getSwitches().size(),
                "el edge debería tener 1 switch conectado");

        // switch ← red
        var network = networkAdapter.createNetwork(IP.fromAddress("10.0.0.0"), "HR", 8);
        var switchWithNetwork = networkAdapter.addNetworkToSwitch(network, networkSwitch);
        assertEquals(1, switchWithNetwork.getSwitchNetworks().size(),
                "el switch debería tener 1 red conectada");
    }

    @Test
    @DisplayName("e2e: el adapter de entrada persiste vía el puerto de salida (ServiceLoader)")
    void persistRouterThroughOutputPort() {
        var routerAdapter = new RouterManagementGenericAdapter();

        var core = routerAdapter.createRouter(
                Vendor.CISCO, Model.XYZ0001, IP.fromAddress("3.0.0.3"),
                LOCATION, RouterType.CORE);

        assertDoesNotThrow(() -> routerAdapter.persistRouter(core),
                "persistir por el adapter debería resolver el puerto de salida y confirmar");
    }

    @Test
    @DisplayName("e2e: el adapter de entrada recupera un router semilla vía el puerto de salida")
    void retrieveSeededRouterThroughOutputPort() {
        var routerAdapter = new RouterManagementGenericAdapter();

        var retrieved = routerAdapter.retrieveRouter(Id.withId(SEEDED_EDGE_ROUTER_ID));

        assertNotNull(retrieved, "el router semilla debería recuperarse por el adapter");
        assertEquals(RouterType.EDGE, retrieved.getRouterType());
        assertEquals("Tully", retrieved.getLocation().getCity());
    }
}
