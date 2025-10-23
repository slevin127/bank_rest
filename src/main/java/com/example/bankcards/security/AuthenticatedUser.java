package com.example.bankcards.security;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.UserAccount;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AuthenticatedUser implements UserDetails {

    private final UUID id;
    private final String username;
    private final String password;
    private final boolean enabled;
    private final Set<Role> roles;

    public AuthenticatedUser(UUID id, String username, String password, boolean enabled, Set<Role> roles) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        this.roles = roles;
    }

    public static AuthenticatedUser from(UserAccount user) {
        return new AuthenticatedUser(user.getId(), user.getUsername(), user.getPasswordHash(), user.isEnabled(), user.getRoles());
    }

    public UUID id() {
        return id;
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    public Set<Role> roles() {
        return roles;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
