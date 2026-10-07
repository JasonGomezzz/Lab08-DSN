package com.techstore.inventario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.inventario.usuarios.ProveedorIdentidad;
import com.techstore.inventario.usuarios.Rol;
import com.techstore.inventario.usuarios.Usuario;
import com.techstore.inventario.usuarios.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

@AutoConfigureMockMvc
@Import(ConfiguracionProveedorFalso.class)
class LoginSocialTest extends PruebaIntegracion {
    private static final String INTERFAZ = "http://localhost:8080";

    @DynamicPropertySource
    static void correosDeGithub(DynamicPropertyRegistry propiedades) {
        propiedades.add("techstore.oauth2.github-correos-uri", () -> ProveedorFalso.base() + "/correos");
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired FabricaUsuarios fabrica;
    @Autowired RelojDePrueba reloj;

    ApiAuth auth;

    @BeforeEach
    void preparar() {
        auth = new ApiAuth(mockMvc, json, reloj);
    }

    @AfterEach
    void restablecerReloj() {
        reloj.restablecer();
    }

    private record Autorizacion(MockHttpSession sesion, String state, UriComponents destino) {
    }

    private Autorizacion iniciar(String registro) throws Exception {
        MvcResult resultado = mockMvc.perform(get("/oauth2/authorization/" + registro))
            .andExpect(status().is3xxRedirection()).andReturn();
        UriComponents destino = UriComponentsBuilder.fromUriString(resultado.getResponse().getRedirectedUrl()).build();
        String state = UriUtils.decode(destino.getQueryParams().getFirst("state"), StandardCharsets.UTF_8);
        return new Autorizacion((MockHttpSession) resultado.getRequest().getSession(false), state, destino);
    }

    private MvcResult volver(String registro, Autorizacion autorizacion, String codigo) throws Exception {
        return volverConEstado(registro, autorizacion.sesion(), "code=" + codigo, autorizacion.state());
    }

    private MvcResult volverConEstado(String registro, MockHttpSession sesion, String parametro, String state)
            throws Exception {
        URI callback = new URI("http://localhost:8080/login/oauth2/code/" + registro + "?" + parametro
            + "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8));
        var peticion = get(callback);
        if (sesion != null) {
            peticion = peticion.session(sesion);
        }
        return mockMvc.perform(peticion).andReturn();
    }

    /** Recorre el baile completo y devuelve la redirección final hacia la interfaz. */
    private MvcResult entrar(String registro, String codigo, String perfil, String correos) throws Exception {
        ProveedorFalso.registrar(codigo, perfil, correos);
        return volver(registro, iniciar(registro), codigo);
    }

    private UriComponents destino(MvcResult resultado) {
        assertThat(resultado.getResponse().getStatus()).isEqualTo(302);
        return UriComponentsBuilder.fromUriString(resultado.getResponse().getRedirectedUrl()).build();
    }

    private Map<String, String> fragmento(UriComponents destino) {
        Map<String, String> valores = new HashMap<>();
        Arrays.stream(destino.getFragment().split("&")).map(par -> par.split("=", 2))
            .forEach(par -> valores.put(par[0], par[1]));
        return valores;
    }

    private String perfilGoogle(String sub, String email, boolean verificado) {
        return "{\"sub\":\"" + sub + "\",\"email\":\"" + email + "\",\"email_verified\":" + verificado
            + ",\"name\":\"Gloria Ruiz\"}";
    }

    private String correoNuevo() {
        return "social-" + UUID.randomUUID() + "@techstore.test";
    }

    @Test
    void elInicioRedirigeAlProveedorConLosParametrosCorrectos() throws Exception {
        Autorizacion autorizacion = iniciar("google");

        assertThat(autorizacion.destino().getPath()).isEqualTo("/autorizar");
        var parametros = autorizacion.destino().getQueryParams();
        assertThat(parametros.getFirst("client_id")).isEqualTo("id-google");
        assertThat(parametros.getFirst("response_type")).isEqualTo("code");
        assertThat(parametros.getFirst("scope")).contains("profile").contains("email").doesNotContain("openid");
        assertThat(parametros.getFirst("redirect_uri")).isEqualTo("http://localhost:8080/login/oauth2/code/google");
        assertThat(autorizacion.state()).isNotBlank();
    }

    @Test
    void laListaDeProveedoresIncluyeGithubYGoogleOrdenada() throws Exception {
        mockMvc.perform(get("/api/auth/proveedores")).andExpect(status().isOk())
            .andExpect(jsonPath("$[0]").value("github")).andExpect(jsonPath("$[1]").value("google"))
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void googleCreaLaCuentaYEntregaElTokenMfaEnElFragmentoDeLaUrl() throws Exception {
        String email = correoNuevo();

        MvcResult resultado = entrar("google", "g-" + UUID.randomUUID(), perfilGoogle("sub-" + UUID.randomUUID(), email, true), null);

        UriComponents destino = destino(resultado);
        assertThat(destino.getScheme() + "://" + destino.getHost() + ":" + destino.getPort()).isEqualTo(INTERFAZ);
        assertThat(destino.getPath()).isEqualTo("/mfa");
        assertThat(destino.getQuery()).isNull();
        Map<String, String> datos = fragmento(destino);
        assertThat(datos).containsEntry("estado", "MFA_ENROLAMIENTO");

        Usuario creado = usuarios.findByEmail(email).orElseThrow();
        assertThat(creado.getProveedor()).isEqualTo(ProveedorIdentidad.GOOGLE);
        assertThat(creado.getRol()).isEqualTo(Rol.EMPLEADO_VENTAS);
        assertThat(creado.getTienda()).isNull();
        assertThat(creado.getPasswordHash()).isNull();
        assertThat(creado.getNombreCompleto()).isEqualTo("Gloria Ruiz");
    }

    @Test
    void elTokenDelLoginSocialCompletaElSegundoFactorYDaAccesoCompleto() throws Exception {
        String email = correoNuevo();
        Map<String, String> datos = fragmento(destino(entrar("google", "g-" + UUID.randomUUID(),
            perfilGoogle("sub-" + UUID.randomUUID(), email, true), null)));
        String mfaToken = java.net.URLDecoder.decode(datos.get("token"), StandardCharsets.UTF_8);
        String secreto = auth.leer(auth.enrolar(mfaToken).andExpect(status().isOk()).andReturn().getResponse()
            .getContentAsString()).get("secreto").asText();

        String acceso = auth.leer(auth.verificar(mfaToken, auth.codigoActual(secreto)).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString()).get("token").asText();

        auth.me(acceso).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.proveedor").value("GOOGLE")).andExpect(jsonPath("$.tienda").doesNotExist());
    }

    @Test
    void elLoginSocialDescartaLaSesionHttp() throws Exception {
        ProveedorFalso.registrar("g-sesion", perfilGoogle("sub-" + UUID.randomUUID(), correoNuevo(), true), null);
        Autorizacion autorizacion = iniciar("google");

        volver("google", autorizacion, "g-sesion");

        assertThat(autorizacion.sesion().isInvalid()).isTrue();
    }

    @Test
    void unSegundoLoginConLaMismaIdentidadNoDuplicaLaCuentaYYaPideElCodigo() throws Exception {
        String email = correoNuevo();
        String sub = "sub-" + UUID.randomUUID();
        Map<String, String> primero = fragmento(destino(entrar("google", "g-" + UUID.randomUUID(),
            perfilGoogle(sub, email, true), null)));
        String mfaToken = java.net.URLDecoder.decode(primero.get("token"), StandardCharsets.UTF_8);
        String secreto = auth.leer(auth.enrolar(mfaToken).andReturn().getResponse().getContentAsString())
            .get("secreto").asText();
        auth.verificar(mfaToken, auth.codigoActual(secreto)).andExpect(status().isOk());

        Map<String, String> segundo = fragmento(destino(entrar("google", "g-" + UUID.randomUUID(),
            perfilGoogle(sub, email, true), null)));

        assertThat(segundo).containsEntry("estado", "MFA_REQUERIDO");
        assertThat(usuarios.findAll().stream().filter(u -> email.equals(u.getEmail())).count()).isEqualTo(1);
    }

    @Test
    void githubUsaElCorreoVerificadoDeLaListaYNoElDelPerfilPublico() throws Exception {
        String verificado = correoNuevo();
        String perfil = "{\"id\":" + (int) (Math.random() * 1_000_000_000) + ",\"login\":\"gabo-hub\",\"name\":\"Gabo Hub\","
            + "\"email\":\"publico-sin-verificar@techstore.test\"}";
        String correos = "[{\"email\":\"" + verificado + "\",\"primary\":true,\"verified\":true}]";

        MvcResult resultado = entrar("github", "h-" + UUID.randomUUID(), perfil, correos);

        assertThat(fragmento(destino(resultado))).containsEntry("estado", "MFA_ENROLAMIENTO");
        Usuario creado = usuarios.findByEmail(verificado).orElseThrow();
        assertThat(creado.getProveedor()).isEqualTo(ProveedorIdentidad.GITHUB);
        assertThat(creado.getNombreCompleto()).isEqualTo("Gabo Hub");
        assertThat(usuarios.existsByEmail("publico-sin-verificar@techstore.test")).isFalse();
    }

    @Test
    void githubSinCorreoVerificadoSeRechazaYNoCreaLaCuenta() throws Exception {
        long id = (long) (Math.random() * 1_000_000_000);
        String perfil = "{\"id\":" + id + ",\"login\":\"sin-correo\",\"email\":\"publico@techstore.test\"}";

        MvcResult resultado = entrar("github", "h-" + UUID.randomUUID(), perfil,
            "[{\"email\":\"publico@techstore.test\",\"primary\":true,\"verified\":false}]");

        UriComponents destino = destino(resultado);
        assertThat(destino.getPath()).isEqualTo("/login");
        assertThat(fragmento(destino)).containsEntry("error", "EMAIL_NO_VERIFICADO");
        assertThat(usuarios.findByProveedorAndProveedorId(ProveedorIdentidad.GITHUB, String.valueOf(id))).isEmpty();
    }

    @Test
    void googleConCorreoNoVerificadoSeRechaza() throws Exception {
        String email = correoNuevo();

        MvcResult resultado = entrar("google", "g-" + UUID.randomUUID(),
            perfilGoogle("sub-" + UUID.randomUUID(), email, false), null);

        assertThat(fragmento(destino(resultado))).containsEntry("error", "EMAIL_NO_VERIFICADO");
        assertThat(usuarios.existsByEmail(email)).isFalse();
    }

    @Test
    void unaCuentaLocalConElMismoCorreoNoSeFusionaConLaSocial() throws Exception {
        Usuario local = fabrica.crear(Rol.GERENTE_TIENDA, fabrica.tienda("LIM-01"));

        MvcResult resultado = entrar("google", "g-" + UUID.randomUUID(),
            perfilGoogle("sub-" + UUID.randomUUID(), local.getEmail(), true), null);

        assertThat(fragmento(destino(resultado))).containsEntry("error", "CUENTA_EXISTENTE");
        Usuario intacta = usuarios.findByEmail(local.getEmail()).orElseThrow();
        assertThat(intacta.getProveedor()).isEqualTo(ProveedorIdentidad.LOCAL);
        assertThat(intacta.getProveedorId()).isNull();
        assertThat(intacta.getRol()).isEqualTo(Rol.GERENTE_TIENDA);
    }

    @Test
    void unaCuentaBloqueadaNoPuedeEntrarPorLaViaSocial() throws Exception {
        String email = correoNuevo();
        String sub = "sub-" + UUID.randomUUID();
        entrar("google", "g-" + UUID.randomUUID(), perfilGoogle(sub, email, true), null);
        Usuario usuario = usuarios.findByEmail(email).orElseThrow();
        usuario.setBloqueadoHasta(reloj.instant().plus(Duration.ofMinutes(10)));
        usuarios.save(usuario);

        MvcResult resultado = entrar("google", "g-" + UUID.randomUUID(), perfilGoogle(sub, email, true), null);

        assertThat(fragmento(destino(resultado))).containsEntry("error", "CUENTA_BLOQUEADA");
    }

    @Test
    void unEstadoManipuladoSeRechazaSinIntercambiarElCodigo() throws Exception {
        ProveedorFalso.registrar("g-estado", perfilGoogle("sub-" + UUID.randomUUID(), correoNuevo(), true), null);
        Autorizacion autorizacion = iniciar("google");

        MvcResult resultado = volverConEstado("google", autorizacion.sesion(), "code=g-estado", "estado-falso");

        assertThat(fragmento(destino(resultado))).containsEntry("error", "LOGIN_SOCIAL_FALLIDO");
    }

    @Test
    void unCallbackSinHaberIniciadoElFlujoSeRechaza() throws Exception {
        ProveedorFalso.registrar("g-sin-sesion", perfilGoogle("sub-" + UUID.randomUUID(), correoNuevo(), true), null);

        MvcResult resultado = volverConEstado("google", null, "code=g-sin-sesion", "cualquiera");

        assertThat(fragmento(destino(resultado))).containsEntry("error", "LOGIN_SOCIAL_FALLIDO");
    }

    @Test
    void siElProveedorRechazaElCodigoElLoginFalla() throws Exception {
        MvcResult resultado = volver("google", iniciar("google"), "codigo-que-no-existe");

        assertThat(fragmento(destino(resultado))).containsEntry("error", "LOGIN_SOCIAL_FALLIDO");
    }

    @Test
    void siElUsuarioCancelaElConsentimientoElLoginFalla() throws Exception {
        Autorizacion autorizacion = iniciar("google");

        MvcResult resultado = volverConEstado("google", autorizacion.sesion(), "error=access_denied",
            autorizacion.state());

        assertThat(fragmento(destino(resultado))).containsEntry("error", "LOGIN_SOCIAL_FALLIDO");
    }
}
