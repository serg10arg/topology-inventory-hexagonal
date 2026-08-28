package com.example.topologyinventory.application;

import com.example.topologyinventory.domain.vo.IP;
import com.example.topologyinventory.domain.vo.Model;
import com.example.topologyinventory.domain.vo.Vendor;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

import static com.example.topologyinventory.domain.vo.RouterType.CORE;
import static com.example.topologyinventory.domain.vo.RouterType.EDGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Step definitions de la feature RouterCreate. */
public class RouterCreate extends ApplicationTestData {

    public RouterCreate() {
        loadData();
    }

    @Given("I provide all required data to create a core router")
    public void create_core_router() {
        router = this.routerManagementUseCase.createRouter(
                Vendor.CISCO, Model.XYZ0001, IP.fromAddress("20.0.0.1"), locationA, CORE);
    }

    @Then("A new core router is created")
    public void a_new_core_router_is_created() {
        assertNotNull(router);
        assertEquals(CORE, router.getRouterType());
    }

    @Given("I provide all required data to create an edge router")
    public void create_edge_router() {
        router = this.routerManagementUseCase.createRouter(
                Vendor.HP, Model.XYZ0004, IP.fromAddress("30.0.0.1"), locationA, EDGE);
    }

    @Then("A new edge router is created")
    public void a_new_edge_router_is_created() {
        assertNotNull(router);
        assertEquals(EDGE, router.getRouterType());
    }
}