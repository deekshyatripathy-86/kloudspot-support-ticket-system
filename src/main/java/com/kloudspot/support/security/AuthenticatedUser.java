package com.kloudspot.support.security;

import com.kloudspot.support.common.Role;

public record AuthenticatedUser(Long userId, Long organizationId, String email, Role role, String name) {}
