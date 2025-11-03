package it.polimi.se.bbp.security;

import it.polimi.se.bbp.entity.User;
import it.polimi.se.bbp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Custom implementation of UserDetailsService for Spring Security.
 * Loads user-specific data from the database for authentication.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    /**
     * Repository for accessing user data from the database.
     */
    private final UserRepository userRepository;

    /**
     * Loads user by email (used as username for Spring Security authentication).
     * This is called by Spring Security during login when authenticating with email.
     * @param email user email (used as username in Spring Security context)
     * @return UserDetails object
     * @throws UsernameNotFoundException if user is not found
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        // Use email as the username in UserDetails for Spring Security
        return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(), Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    }

    /**
     * Loads user by user ID.
     * This is used by JWT filter to validate tokens (tokens contain user ID).
     * @param userId user ID
     * @return UserDetails object
     * @throws UsernameNotFoundException if user is not found
     */
    public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        User user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));
        return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(), Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    }

}