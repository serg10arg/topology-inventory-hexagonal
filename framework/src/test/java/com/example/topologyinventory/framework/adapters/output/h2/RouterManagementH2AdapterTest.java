package com.example.topologyinventory.framework.adapters.output.h2;

import com.example.topologyinventory.domain.entity.CoreRouter;
import com.example.topologyinventory.domain.entity.EdgeRouter;
import com.example.topologyinventory.domain.entity.Router;
import com.example.topologyinventory.domain.entity.Switch;
import com.example.topologyinventory.domain.entity.factory.RouterFactory;
import com.example.topologyinventory.domain.vo.*;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test de integración del output adapter de persistencia REACTIVA. Arranca el contenedor de
 * Quarkus ({@code @QuarkusTest}) —Arc + Hibernate Reactive sobre MySQL vía Dev Services, con el
 * esquema generado desde las entidades y el seed de {@code import.sql}— y ejercita el adapter como
 * bean CDI.
 *
 * <p><b>Arnés reactivo (decisión D5).</b> Cada test corre {@code @RunOnVertxContext} y afirma sobre
 * la {@link io.smallrye.mutiny.Uni} con un {@link UniAsserter}, sin bloquear el hilo.
 *
 * <p><b>Round-trip del agregado (SC2).</b> Los dos últimos tests son el único "caller" real de la
 * escritura profunda (la superficie REST no persiste hijos): construyen un agregado con hijos, lo
 * persisten y lo recuperan en una sesión nueva, afirmando que los hijos sobrevivieron el viaje a
 * MySQL y de vuelta. Como {@code drop-and-create} corre una vez al arrancar el contenedor (no por
 * test), los fixtures usan ids nuevos y únicos para no chocar con la semilla ni entre sí.
 */
@QuarkusTest
class RouterManagementH2AdapterTest {

    /** EDGE router semilla insertado por import.sql (JUNIPER/XYZ0001, sin switches, ubicación en Tully). */
    private static final String SEEDED_EDGE_ROUTER_ID = "b832ef4f-f894-4194-8feb-a99c2cd4be0a";

    @Inject
    RouterManagementH2Adapter adapter;

    @Test
    @RunOnVertxContext
    @DisplayName("retrieveRouter recupera y mapea un router semilla desde MySQL reactivo")
    void retrieveSeededRouter_shouldMapFromDatabase(UniAsserter asserter) {
        asserter.assertThat(
                () -> adapter.retrieveRouter(Id.withId(SEEDED_EDGE_ROUTER_ID)),
                router -> {
                    assertNotNull(router, "El router semilla debería recuperarse desde la base de datos");
                    assertEquals(Id.withId(SEEDED_EDGE_ROUTER_ID), router.getId());
                    assertEquals(RouterType.EDGE, router.getRouterType());
                    assertEquals(Vendor.JUNIPER, router.getVendor());
                    assertEquals(Model.XYZ0001, router.getModel());
                    // Prueba de que las columnas @Embedded de Location se leyeron de la fila semilla.
                    assertEquals("Tully", router.getLocation().getCity());
                });
    }

    @Test
    @RunOnVertxContext
    @DisplayName("persistRouter confirma la transacción reactiva sin lanzar")
    void persistNewCoreRouter_shouldCommitWithoutError(UniAsserter asserter) {
        var newRouter = RouterFactory.getRouter(
                Id.withId("11111111-1111-1111-1111-111111111111"),
                Vendor.CISCO, Model.XYZ0001, IP.fromAddress("1.0.0.1"),
                tullyLocation(), RouterType.CORE);

        asserter.assertThat(
                () -> adapter.persistRouter(newRouter),
                persisted -> assertNotNull(persisted,
                        "Persistir un router nuevo debería emitir el router sin error"));
    }

    @Test
    @RunOnVertxContext
    @DisplayName("round-trip: un EDGE con switch y red sobrevive persistir y recuperar")
    void persistAndRetrieveEdgeWithChildren_shouldSurvive(UniAsserter asserter) {
        var edgeId = Id.withId("22222222-2222-2222-2222-222222222222");
        var switchId = Id.withId("22222222-2222-2222-2222-2222222222aa");
        var edge = edgeWithSwitchAndNetwork(edgeId, switchId);

        asserter.assertThat(
                () -> adapter.persistRouter(edge)
                        .flatMap(persisted -> adapter.retrieveRouter(edgeId)),
                retrieved -> {
                    assertNotNull(retrieved, "El edge persistido debería recuperarse");
                    var edgeRouter = (EdgeRouter) retrieved;
                    assertEquals(1, edgeRouter.getSwitches().size(), "El switch debería persistir con el edge");
                    Switch networkSwitch = edgeRouter.getSwitches().get(switchId);
                    assertNotNull(networkSwitch, "El switch debería recuperarse por su id");
                    assertEquals(1, networkSwitch.getSwitchNetworks().size(), "La red debería persistir con el switch");
                    assertEquals("test-net", networkSwitch.getSwitchNetworks().get(0).getNetworkName());
                });
    }

    @Test
    @RunOnVertxContext
    @DisplayName("round-trip: un CORE con un edge hijo conserva el hijo tras recuperar")
    void persistAndRetrieveCoreWithChild_shouldKeepChild(UniAsserter asserter) {
        var coreId = Id.withId("33333333-3333-3333-3333-333333333333");
        var childEdgeId = Id.withId("33333333-3333-3333-3333-3333333333aa");
        var core = coreWithChildEdge(coreId, childEdgeId);

        asserter.assertThat(
                () -> adapter.persistRouter(core)
                        .flatMap(persisted -> adapter.retrieveRouter(coreId)),
                retrieved -> {
                    assertNotNull(retrieved, "El core persistido debería recuperarse");
                    var coreRouter = (CoreRouter) retrieved;
                    assertTrue(coreRouter.getRouters().containsKey(childEdgeId),
                            "El edge hijo debería reencontrarse por su self-FK (router_parent_core_id)");
                });
    }

    // -----------------------------------------------------------------
    // fixtures — ensamblados con builders (como reconstruye el mapper),
    // sin pasar por addSwitch/addRouter para no disparar specs de dominio
    // -----------------------------------------------------------------

    private EdgeRouter edgeWithSwitchAndNetwork(Id edgeId, Id switchId) {
        var network = new Network(IP.fromAddress("10.0.0.0"), "test-net", 24);
        var networkSwitch = Switch.builder()
                .id(switchId)
                .vendor(Vendor.CISCO).model(Model.XYZ0001)
                .ip(IP.fromAddress("2.0.0.1"))
                .location(tullyLocation())
                .switchType(SwitchType.LAYER2)
                .switchNetworks(List.of(network))
                .build();
        return EdgeRouter.builder()
                .id(edgeId)
                .vendor(Vendor.CISCO).model(Model.XYZ0001)
                .ip(IP.fromAddress("2.0.0.2"))
                .location(tullyLocation())
                .routerType(RouterType.EDGE)
                .switches(new HashMap<>(Map.of(switchId, networkSwitch)))
                .build();
    }

    private CoreRouter coreWithChildEdge(Id coreId, Id childEdgeId) {
        Router childEdge = RouterFactory.getRouter(
                childEdgeId, Vendor.CISCO, Model.XYZ0001, IP.fromAddress("3.0.0.1"),
                tullyLocation(), RouterType.EDGE);
        return CoreRouter.builder()
                .id(coreId)
                .vendor(Vendor.CISCO).model(Model.XYZ0001)
                .ip(IP.fromAddress("3.0.0.2"))
                .location(tullyLocation())
                .routerType(RouterType.CORE)
                .routers(new HashMap<>(Map.of(childEdgeId, childEdge)))
                .build();
    }

    private Location tullyLocation() {
        return Location.builder()
                .address("Amos Ln").city("Tully").state("NY").zipCode(13159)
                .country("United States").latitude(42.79731f).longitude(-76.13075f)
                .build();
    }
}
