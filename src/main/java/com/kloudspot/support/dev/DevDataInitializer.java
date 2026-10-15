package com.kloudspot.support.dev;

import com.kloudspot.support.common.Role;
import com.kloudspot.support.organization.*;
import com.kloudspot.support.user.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Configuration
public class DevDataInitializer {
    @Bean
    @ConditionalOnProperty(prefix = "app.demo-data", name = "enabled", havingValue = "true", matchIfMissing = true)
    CommandLineRunner seedDemoData(OrganizationRepository organizations, UserRepository users, PasswordEncoder encoder) {
        return args -> seed(organizations, users, encoder);
    }
    @Transactional
    void seed(OrganizationRepository organizations, UserRepository users, PasswordEncoder encoder) {
        Organization demo = organizations.findAll().stream().filter(o -> o.getName().equals("Demo Organization")).findFirst()
                .orElseGet(() -> organizations.save(new Organization("Demo Organization")));
        createIfMissing(users, encoder, demo, "Demo Admin", "admin@demo.com", Role.ADMIN);
        createIfMissing(users, encoder, demo, "Demo Agent", "agent@demo.com", Role.AGENT);
        createIfMissing(users, encoder, demo, "Demo Customer", "customer@demo.com", Role.CUSTOMER);
        Organization second = organizations.findAll().stream().filter(o -> o.getName().equals("Second Organization")).findFirst()
                .orElseGet(() -> organizations.save(new Organization("Second Organization")));
        createIfMissing(users, encoder, second, "Second Customer", "customer@second.com", Role.CUSTOMER);
    }
    private void createIfMissing(UserRepository users, PasswordEncoder encoder, Organization org, String name, String email, Role role) {
        if (!users.existsByOrganization_IdAndEmailIgnoreCase(org.getId(), email))
            users.save(new AppUser(org,name,email,encoder.encode("ChangeMe123!"),role));
    }
}
