package com.example.topologyinventory.framework.adapters.input.rest;

import com.example.topologyinventory.framework.adapters.input.rest.request.AddNetworkRequest;
import com.example.topologyinventory.framework.adapters.input.rest.request.CreateNetworkRequest;
import com.example.topologyinventory.framework.adapters.input.rest.request.RemoveNetworkRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Test de los input adapters REST de red ({@code @QuarkusTest} + REST Assured). Usa el
 * agregado semilla: el edge {@code ad46} contiene el switch {@code bb46} (con las redes HR,
 * Marketing, Engineering) y {@code bb47} (vacío). Las altas/bajas de red operan en memoria
 * sobre el switch recuperado dentro de su edge, sin persistir: el orden entre tests no importa.
 */
@QuarkusTest
class NetworkManagementRestAdapterTest {

    private static final String SEEDED_EDGE_ID = "b07f5187-2d82-4975-a14b-bdbad9a8ad46";
    /** Switch semilla con 3 redes (HR, Marketing, Engineering). */
    private static final String SEEDED_SWITCH_WITH_NETWORKS = "922dbcd5-d071-41bd-920b-00f83eb4bb46";
    /** Switch semilla vacío: alta de red sin colisiones de disponibilidad ni de cantidad. */
    private static final String SEEDED_SWITCH_EMPTY = "922dbcd5-d071-41bd-920b-00f83eb4bb47";

    @Test
    @DisplayName("POST /network/create: crea una red en memoria")
    void createNetwork() {
        var request = new CreateNetworkRequest();
        request.setNetworkAddress("40.0.0.0");
        request.setNetworkName("Sales");
        request.setNetworkCidr(8);

        given()
                .contentType(ContentType.JSON).body(request)
                .when().post("/network/create")
                .then().statusCode(200)
                .body("networkName", is("Sales"))
                .body("networkCidr", is(8))
                .body("networkAddress", is("40.0.0.0"));
    }

    @Test
    @DisplayName("POST /network/add: añade una red al switch vacío del edge (0 -> 1)")
    void addNetworkToSwitch() {
        var request = new AddNetworkRequest();
        request.setNetworkAddress("40.0.0.0");
        request.setNetworkName("Sales");
        request.setNetworkCidr(8);
        request.setEdgeRouterId(SEEDED_EDGE_ID);
        request.setSwitchId(SEEDED_SWITCH_EMPTY);

        given()
                .contentType(ContentType.JSON).body(request)
                .when().post("/network/add")
                .then().statusCode(200)
                .body("id", is(SEEDED_SWITCH_EMPTY))
                .body("networks.size()", is(1))
                .body("networks.networkName", hasItem("Sales"));
    }

    @Test
    @DisplayName("POST /network/remove: quita una red por nombre del switch semilla (3 -> 2)")
    void removeNetworkFromSwitch() {
        var request = new RemoveNetworkRequest();
        request.setEdgeRouterId(SEEDED_EDGE_ID);
        request.setSwitchId(SEEDED_SWITCH_WITH_NETWORKS);
        request.setNetworkName("HR");

        given()
                .contentType(ContentType.JSON).body(request)
                .when().post("/network/remove")
                .then().statusCode(200)
                .body("id", is(SEEDED_SWITCH_WITH_NETWORKS))
                .body("networks.size()", is(2))
                .body("networks.networkName", not(hasItem("HR")));
    }
}
