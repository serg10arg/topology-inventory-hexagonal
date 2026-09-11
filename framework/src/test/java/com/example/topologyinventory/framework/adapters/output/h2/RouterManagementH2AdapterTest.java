package com.example.topologyinventory.framework.adapters.output.h2;

import com.example.topologyinventory.domain.entity.Router;
import com.example.topologyinventory.domain.entity.factory.RouterFactory;
import com.example.topologyinventory.domain.vo.*;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test de integración del output adapter de persistencia REACTIVA. Arranca el contenedor de
 * Quarkus ({@code @QuarkusTest}) —Arc + Hibernate Reactive sobre MySQL vía Dev Services, con el
 * esquema generado desde las entidades y el seed de {@code import.sql}— y ejercita el adapter como
 * bean CDI.
 *
 * <p><b>Arnés reactivo (decisión D5).</b> Cada test corre {@code @RunOnVertxContext} y afirma sobre
 * la {@link io.smallrye.mutiny.Uni} con un {@link UniAsserter}, sin bloquear el hilo. Los dos
 * caminos se prueban por separado: lectura de un router semilla (fuerza una lectura real y el
 * mapper de vuelta) y escritura de un router nuevo (confirma que la transacción reactiva no lanza).
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
        var newRouter = newCoreRouterFixture(
                Id.withId("11111111-1111-1111-1111-111111111111"));

        asserter.assertThat(
                () -> adapter.persistRouter(newRouter),
                persisted -> assertNotNull(persisted,
                        "Persistir un router nuevo debería emitir el router sin error"));
    }

    /**
     * Construye un {@code CoreRouter} sin hijos con un id conocido, usando {@link RouterFactory}
     * (la misma vía que el caso de uso) para no acoplar el test a las clases concretas del dominio.
     */
    private Router newCoreRouterFixture(Id id) {
        var location = Location.builder()
                .address("Amos Ln").city("Tully").state("NY").zipCode(13159)
                .country("United States").latitude(42.79731f).longitude(-76.13075f)
                .build();

        return RouterFactory.getRouter(
                id, Vendor.CISCO, Model.XYZ0001, IP.fromAddress("1.0.0.1"),
                location, RouterType.CORE);
    }
}
