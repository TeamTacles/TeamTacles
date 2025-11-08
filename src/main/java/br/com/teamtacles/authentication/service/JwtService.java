package br.com.teamtacles.authentication.service;

import java.time.Instant;
import java.util.stream.Collectors;
import br.com.teamtacles.user.model.Role;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;

import br.com.teamtacles.user.model.User;

@Service
public class JwtService {
    private final JwtEncoder jwtEncoder;

    public JwtService(JwtEncoder encoder) {
        this.jwtEncoder = encoder;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        long expireInSeconds = 3600L;

        String scope = user.getRoles().stream()
                .map(Role::getRoleName)
                .map(Enum::name)
                .collect(Collectors.joining(" "));

        var claims = JwtClaimsSet.builder()
                .issuer("teamtacles-api")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expireInSeconds))
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("scope", scope)
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
