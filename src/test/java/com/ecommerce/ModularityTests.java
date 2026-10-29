package com.ecommerce;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityTests {

  private final ApplicationModules modules = ApplicationModules.of(CatalogApplication.class);

  @Test
  @DisplayName("Should comply with Modulith boundaries when verifying modules")
  void shouldComplyWithModulithBoundaries_whenVerifyingModules() {
    modules.verify();
  }

  @Test
  @DisplayName("Should generate architecture documentation and PlantUML diagrams")
  void shouldGenerateArchitectureDocumentation_whenDocumenting() {
    new Documenter(modules)
        .writeDocumentation();
  }
}
