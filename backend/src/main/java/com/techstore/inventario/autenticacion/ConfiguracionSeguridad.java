package com.techstore.inventario.autenticacion;

import static com.techstore.inventario.comun.GlobalExceptionHandler.problema;

import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.inventario.comun.PropiedadesTechStore;
import com.techstore.inventario.usuarios.UsuarioRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class ConfiguracionSeguridad {

    @Bean
    PasswordEncoder codificadorPassword(PropiedadesTechStore propiedades) {
        return new BCryptPasswordEncoder(propiedades.seguridad().bcryptCosto());
    }

    @Bean
    SecurityFilterChain seguridad(HttpSecurity http, ServicioJwt jwt, UsuarioRepository usuarios,
                                  ObjectMapper json) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(reglas -> reglas
                .requestMatchers(HttpMethod.POST, "/api/auth/registro", "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/tiendas", "/api/auth/proveedores",
                    "/api/actuator/health/**").permitAll()
                .requestMatchers("/api/auth/mfa/**").hasAuthority(FiltroJwt.AUTORIDAD_MFA)
                .requestMatchers("/api/**").hasAuthority(FiltroJwt.AUTORIDAD_ACCESO)
                .anyRequest().permitAll())
            .exceptionHandling(errores -> errores
                .authenticationEntryPoint((request, response, ex) -> {
                    Object motivo = request.getAttribute(FiltroJwt.ATRIBUTO_ERROR);
                    escribir(response, json, problema(HttpStatus.UNAUTHORIZED, "No autenticado",
                        motivo == null ? "Falta el token de autenticación" : "El token no es válido",
                        motivo == null ? "TOKEN_AUSENTE" : motivo.toString()));
                })
                .accessDeniedHandler((request, response, ex) -> escribir(response, json,
                    problema(HttpStatus.FORBIDDEN, "Acceso denegado",
                        "El token no permite esta operación; completa el inicio de sesión primero",
                        "TOKEN_NO_PERMITIDO"))))
            .addFilterBefore(new FiltroJwt(jwt, usuarios), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static void escribir(HttpServletResponse response, ObjectMapper json, ProblemDetail problema)
            throws IOException {
        response.setStatus(problema.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        json.writeValue(response.getWriter(), problema);
    }
}
