package com.kloudspot.support.user;

import com.kloudspot.support.security.PrincipalUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping("/agents")
    public List<UserDtos.Response> agents() {
        return service.listAgents(PrincipalUtil.current());
    }

    @GetMapping
    public List<UserDtos.Response> list() {
        return service.listOrganizationUsers(PrincipalUtil.current());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDtos.Response create(@Valid @RequestBody UserDtos.CreateRequest request) {
        return service.create(request, PrincipalUtil.current());
    }
}
