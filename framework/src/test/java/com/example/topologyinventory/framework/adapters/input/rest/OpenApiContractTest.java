package com.example.topologyinventory.framework.adapters.input.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

/**
 * Verifica que el contrato OpenAPI se genera y refleja las anotaciones de los adapters.
 * No comprueba el look de la Swagger UI (eso es config de runtime en bootstrap), sino que
 * el documento en {@code /q/openapi} —siempre disponible cuando el extension está presente—
 * contiene los tags y operationId declarados con {@code @Tag}/{@code @Operation}. Así, si
 * alguien retira o renombra una anotación, este test lo detecta en el reactor, no en la UI.
 */
@QuarkusTest
class OpenApiContractTest {

    @Test
    @DisplayName("/q/openapi publica los tags de los tres adapters")
    void openApiDocumentExposesTags() {
        given()
                .when().get("/q/openapi")
                .then().statusCode(200)
                .body(containsString("Router Operations"))
                .body(containsString("Switch Operations"))
                .body(containsString("Network Operations"));
    }

    @Test
    @DisplayName("/q/openapi refleja los operationId declarados con @Operation")
    void openApiDocumentExposesOperationIds() {
        given()
                .when().get("/q/openapi")
                .then().statusCode(200)
                .body(containsString("createRouter"))
                .body(containsString("addSwitchToEdgeRouter"))
                .body(containsString("removeNetworkFromSwitch"));
    }
}
