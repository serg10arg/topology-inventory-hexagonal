package com.example.topologyinventory.application;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * Runner de la suite de aceptación (Cucumber sobre JUnit 5 Platform). Descubre los
 * ficheros {@code .feature} bajo el classpath del paquete de aplicación y los enlaza
 * con los step definitions de este mismo paquete (glue).
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("com/example/topologyinventory/application")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.example.topologyinventory.application")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, html:target/cucumber-result.html")
public class ApplicationTest {
}