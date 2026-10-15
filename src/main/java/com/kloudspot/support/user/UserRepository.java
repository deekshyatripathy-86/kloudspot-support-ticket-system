package com.kloudspot.support.user;

import com.kloudspot.support.common.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByOrganization_IdAndEmailIgnoreCase(Long organizationId, String email);
    boolean existsByOrganization_IdAndEmailIgnoreCase(Long organizationId, String email);
    List<AppUser> findAllByOrganization_IdOrderByNameAsc(Long organizationId);
    List<AppUser> findAllByOrganization_IdAndRoleOrderByNameAsc(Long organizationId, Role role);
}
