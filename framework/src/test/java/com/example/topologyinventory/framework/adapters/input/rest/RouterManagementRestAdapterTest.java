package com.example.topologyinventory.framework.adapters.input.rest;

import com.example.topologyinventory.domain.vo.Model;
import com.example.topologyinventory.domain.vo.RouterType;
import com.example.topologyinventory.domain.vo.Vendor;
import com.example.topologyinventory.framework.adapters.input.rest.request.AddRouterRequest;
import com.example.topologyinventory.framework.adapters.input.rest.request.CreateRouterRequest;
import com.example.topologyinventory.framework.adapters.input.rest.request.LocationRequest;
import com.example.topologyinventory.framework.adapters.input.rest.request.RemoveRouterRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Test de los input adapters REST reactivos de router ({@code @QuarkusTest} + REST Assured).
 * Arranca el contenedor de Quarkus con el servidor HTTP y ejercita los endpoints por HTTP
 * real contra H2 sembrado por {@code import.sql}. Además de la lógica de cada endpoint,
 * prueba de extremo a extremo la cadena driving completa: adapter REST → caso de uso →
 * puerto de salida → output adapter H2. Sustituye la cobertura del antiguo
 * {@code GenericAdaptersEndToEndTest} para el camino de router, ahora por HTTP.
 *
 * <p>Los fixtures respetan las specifications del dominio: misma ubicación (mismo país) e
 * IPs distintas entre core y edge. Los endpoints {@code add}/{@code remove} no persisten la
 * conexión, así que el test de baja desconecta un edge <em>semilla</em> (presente en el core
 * recuperado desde la base), no uno añadido en memoria por un test previo.
 */
@QuarkusTest
class RouterManagementRestAdapterTest {

    /** CORE router semilla (import.sql), raíz del agregado; sus hijos edge cuelgan de él. */
    private static final String SEEDED_CORE_ID = "b832ef4f-f894-4194-8feb-a99c2cd4be0c";
    /** EDGE router semilla conectado al core, sin switches (baja segura). */
    private static final String SEEDED_EDGE_ID = "b832ef4f-f894-4194-8feb-a99c2cd4be0a";

    @Test
    @DisplayName("GET /router/retrieve/{id}: recupera un router semilla desde H2")
    void retrieveSeededRouter() {
        given()
                .when().get("/router/retrieve/{id}", SEEDED_CORE_ID)
                .then().statusCode(200)
                .body("id", is(SEEDED_CORE_ID))
                .body("routerType", is("CORE"))
                .body("location.city", is("Tully"));
    }

    @Test
    @DisplayName("POST /router/create: crea y persiste un router, y se recupera después")
    void createAndRetrieveRouter() {
        var request = new CreateRouterRequest();
        request.setVendor(Vendor.CISCO);
        request.setModel(Model.XYZ0001);
        request.setIp("3.0.0.3");
        request.setLocation(tully());
        request.setRouterType(RouterType.CORE);

        String createdId = given()
                .contentType(ContentType.JSON).body(request)
                .when().post("/router/create")
                .then().statusCode(200)
                .body("id", notNullValue())
                .body("routerType", is("CORE"))
                .body("location.city", is("Tully"))
                .extract().path("id");

        // Round-trip: lo persistido debe recuperarse por su id.
        given()
                .when().get("/router/retrieve/{id}", createdId)
                .then().statusCode(200)
                .body("id", is(createdId));
    }

    @Test
    @DisplayName("POST /router/add: conecta un edge nuevo al core semilla")
    void addRouterToCoreRouter() {
        // Un edge nuevo, persistido para poder recuperarlo por id en el endpoint de alta.
        var edge = new CreateRouterRequest();
        edge.setVendor(Vendor.JUNIPER);
        edge.setModel(Model.XYZ0001);
        edge.setIp("7.0.0.7");
        edge.setLocation(tully());
        edge.setRouterType(RouterType.EDGE);

        String edgeId = given()
                .contentType(ContentType.JSON).body(edge)
                .when().post("/router/create")
                .then().statusCode(200)
                .extract().path("id");

        var addRequest = new AddRouterRequest();
        addRequest.setRouterId(edgeId);
        addRequest.setCoreRouterId(SEEDED_CORE_ID);

        given()
                .contentType(ContentType.JSON).body(addRequest)
                .when().post("/router/add")
                .then().statusCode(200)
                .body("id", is(SEEDED_CORE_ID))
                .body("routerType", is("CORE"))
                .body("routerIds", hasItem(edgeId));
    }

    @Test
    @DisplayName("POST /router/remove: desconecta un edge semilla del core")
    void removeRouterFromCoreRouter() {
        var removeRequest = new RemoveRouterRequest();
        removeRequest.setRouterId(SEEDED_EDGE_ID);
        removeRequest.setCoreRouterId(SEEDED_CORE_ID);

        given()
                .contentType(ContentType.JSON).body(removeRequest)
                .when().post("/router/remove")
                .then().statusCode(200)
                .body("id", is(SEEDED_EDGE_ID))
                .body("routerType", is("EDGE"));
    }

    /** Ubicación de fixtures: Tully (United States), la misma que usa import.sql. */
    private static LocationRequest tully() {
        var location = new LocationRequest();
        location.setAddress("Amos Ln");
        location.setCity("Tully");
        location.setState("NY");
        location.setZipCode(13159);
        location.setCountry("United States");
        location.setLatitude(42.79731f);
        location.setLongitude(-76.13075f);
        return location;
    }
}
