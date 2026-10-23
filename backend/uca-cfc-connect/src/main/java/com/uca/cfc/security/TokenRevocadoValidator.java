package com.uca.cfc.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import com.uca.cfc.repository.TokenRevocadoRepository;

/** Rechaza los tokens cuya sesión ya se cerró (logout), aunque su firma y fecha sigan vigentes. */
public class TokenRevocadoValidator implements OAuth2TokenValidator<Jwt> {

    private final TokenRevocadoRepository tokenRevocadoRepository;

    public TokenRevocadoValidator(TokenRevocadoRepository tokenRevocadoRepository) {
        this.tokenRevocadoRepository = tokenRevocadoRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (jwt.getId() != null && tokenRevocadoRepository.existsByJti(jwt.getId())) {
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "La sesión fue cerrada", null));
        }
        return OAuth2TokenValidatorResult.success();
    }
}
