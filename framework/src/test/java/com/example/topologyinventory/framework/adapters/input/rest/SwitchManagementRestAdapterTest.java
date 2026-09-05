package com.example.topologyinventory.framework.adapters.input.rest;

import com.example.topologyinventory.domain.vo.Model;
import com.example.topologyinventory.domain.vo.SwitchType;
import com.example.topologyinventory.domain.vo.Vendor;
import com.example.topologyinventory.framework.adapters.input.rest.request.AddSwitchRequest;
import com.example.topologyinventory.framework.adapters.input.rest.request.CreateSwitchRequest;
import com.example.topologyinventory.framework.adapters.input.rest.request.LocationRequest;
import com.example.topologyinventory.framework.adapters.input.rest.request.RemoveSwitchRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Test de los input adapters REST de switch ({@code @QuarkusTest} + REST Assured). Ejercita
 * los endpoints por HTTP contra el agregado semilla: el edge router {@code ad46} tiene dos
 * switches, {@code bb46} (con redes) y {@code bb47} (sin redes). Como en este núcleo los
 * switches no se persisten, {@code add}/{@code remove} operan en memoria sobre el edge
 * recuperado desde H2 y no dejan rastro en la base: cada test lo recupera con sus dos
 * switches originales, así que el orden entre tests no importa.
 */
@QuarkusTest
class SwitchManagementRestAdapterTest {

    /** EDGE router semilla (HP/XYZ0002), padre de dos switches. */
    private static final String SEEDED_EDGE_ID = "b07f5187-2d82-4975-a14b-bdbad9a8ad46";
    /** Switch semilla sin redes: se puede desconectar (EmptyNetworkSpec lo exige). */
    private static final String SEEDED_SWITCH_NO_NETWORKS = "922dbcd5-d071-41bd-920b-00f83eb4bb47";

    @Test
    @DisplayName("POST /switch/create: crea un switch en memoria")
    void createSwitch() {
        given()
                .contentType(ContentType.JSON).body(switchRequest())
                .when().post("/switch/create")
                .then().statusCode(200)
                .body("id", notNullValue())
                .body("switchType", is("LAYER3"))
                .body("vendor", is("CISCO"))
                .body("networks.size()", is(0));
    }

    @Test
    @DisplayName("POST /switch/add: conecta un switch nuevo al edge semilla (2 -> 3)")
    void addSwitchToEdgeRouter() {
        var request = new AddSwitchRequest();
        request.setVendor(Vendor.CISCO);
        request.setModel(Model.XYZ0002);
        request.setIp("11.0.0.11");
        request.setLocation(tully());
        request.setSwitchType(SwitchType.LAYER3);
        request.setEdgeRouterId(SEEDED_EDGE_ID);

        given()
                .contentType(ContentType.JSON).body(request)
                .when().post("/switch/add")
                .then().statusCode(200)
                .body("id", is(SEEDED_EDGE_ID))
                .body("routerType", is("EDGE"))
                .body("switchIds.size()", is(3));
    }

    @Test
    @DisplayName("POST /switch/remove: desconecta un switch sin redes del edge (2 -> sin ese switch)")
    void removeSwitchFromEdgeRouter() {
        var request = new RemoveSwitchRequest();
        request.setEdgeRouterId(SEEDED_EDGE_ID);
        request.setSwitchId(SEEDED_SWITCH_NO_NETWORKS);

        given()
                .contentType(ContentType.JSON).body(request)
                .when().post("/switch/remove")
                .then().statusCode(200)
                .body("id", is(SEEDED_EDGE_ID))
                .body("switchIds", not(hasItem(SEEDED_SWITCH_NO_NETWORKS)));
    }

    private static CreateSwitchRequest switchRequest() {
        var request = new CreateSwitchRequest();
        request.setVendor(Vendor.CISCO);
        request.setModel(Model.XYZ0002);
        request.setIp("11.0.0.11");
        request.setLocation(tully());
        request.setSwitchType(SwitchType.LAYER3);
        return request;
    }

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
