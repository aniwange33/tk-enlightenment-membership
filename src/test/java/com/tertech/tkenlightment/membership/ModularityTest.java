package com.tertech.tkenlightment.membership;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTest {

    ApplicationModules modules = ApplicationModules.of(MembershipApplication.class);

    @Test
    void verifyModularStructure() {
        modules.verify();
    }

    @Test
    void printModules() {
        modules.forEach(System.out::println);
    }
}
