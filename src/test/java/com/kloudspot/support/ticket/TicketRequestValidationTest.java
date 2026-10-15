package com.kloudspot.support.ticket;

import jakarta.validation.Validation;
import com.kloudspot.support.common.Role;
import com.kloudspot.support.user.UserDtos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TicketRequestValidationTest {
    @Test
    void createRequestRejectsBlankAndOversizedFields() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var blank = new TicketDtos.CreateRequest(" ", "Details", null);
            var oversized = new TicketDtos.CreateRequest("x".repeat(256), "Details", null);

            assertEquals(2, validator.validate(blank).size());
            assertEquals(2, validator.validate(oversized).size());
        }
    }

    @Test
    void userCreationRejectsInvalidEmailAndShortPassword() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var invalid = new UserDtos.CreateRequest("New User", "not-an-email", "short", Role.AGENT);

            assertEquals(2, validator.validate(invalid).size());
        }
    }
}
