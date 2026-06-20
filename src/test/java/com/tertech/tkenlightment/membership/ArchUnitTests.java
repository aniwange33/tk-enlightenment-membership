package com.tertech.tkenlightment.membership;

import com.enofex.taikai.Taikai;
import org.junit.jupiter.api.Test;

class ArchUnitTests {

    @Test
    void verifyArchitectureRules() {
        Taikai.builder()
                .namespace("com.tertech.tkenlightment.membership")
                .java(java -> java
                        .imports(imports -> imports.shouldNotImport("..shaded.."))
                )
                .spring(spring -> spring
                        .noAutowiredFields()
                        .controllers(controllers -> controllers
                                .shouldBeAnnotatedWithRestController()
                                .namesShouldEndWithController()
                        )
                        .repositories(repositories -> repositories.namesShouldEndWithRepository())
                        .services(services -> services.namesShouldEndWithService())
                )
                .build()
                .check();
    }
}
