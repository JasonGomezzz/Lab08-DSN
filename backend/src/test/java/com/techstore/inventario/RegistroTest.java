package com.techstore.inventario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.inventario.usuarios.Rol;
import com.techstore.inventario.usuarios.Usuario;
import com.techstore.inventario.usuarios.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@AutoConfigureMockMvc
class RegistroTest extends PruebaIntegracion {
    @Autowired MockMvc api;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired FabricaUsuarios fabrica;

    private ResultActions registrar(Map<String, Object> cuerpo) throws Exception {
        return api.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(cuerpo)));
    }

    private Map<String, Object> solicitud(String email) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("email", email);
        cuerpo.put("password", "Segura#2026");
        cuerpo.put("nombreCompleto", "Ana Torres Ríos");
        cuerpo.put("tiendaId", fabrica.tienda("LIM-01").getId());
        return cuerpo;
    }

    private String correoNuevo() {
        return "reg-" + UUID.randomUUID() + "@techstore.test";
    }

    @Test
    void registraUnEmpleadoDeVentasYNoDevuelveElHash() throws Exception {
        String email = correoNuevo();

        registrar(solicitud(email))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.rol").value("EMPLEADO_VENTAS"))
            .andExpect(jsonPath("$.tienda.codigo").value("LIM-01"))
            .andExpect(jsonPath("$.mfaHabilitado").value(false))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("hash"))))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Segura"))));
    }

    @Test
    void guardaLaContrasenaConBCryptYElCorreoEnMinusculas() throws Exception {
        String email = correoNuevo();

        registrar(solicitud(email.toUpperCase())).andExpect(status().isCreated());

        Usuario guardado = usuarios.findByEmail(email).orElseThrow();
        assertThat(guardado.getPasswordHash()).startsWith("$2").doesNotContain("Segura");
    }

    @Test
    void elRegistroPublicoIgnoraUnRolEnviadoEnLaSolicitud() throws Exception {
        String email = correoNuevo();
        Map<String, Object> cuerpo = solicitud(email);
        cuerpo.put("rol", "ADMINISTRADOR");

        registrar(cuerpo).andExpect(status().isCreated()).andExpect(jsonPath("$.rol").value("EMPLEADO_VENTAS"));

        assertThat(usuarios.findByEmail(email).orElseThrow().getRol()).isEqualTo(Rol.EMPLEADO_VENTAS);
    }

    @Test
    void rechazaUnCorreoYaRegistradoSinDistinguirMayusculas() throws Exception {
        String email = correoNuevo();
        registrar(solicitud(email)).andExpect(status().isCreated());

        registrar(solicitud(email.toUpperCase()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("EMAIL_EXISTENTE"));
    }

    @Test
    void informaTodasLasReglasDeLaContrasenaIncumplidas() throws Exception {
        Map<String, Object> cuerpo = solicitud(correoNuevo());
        cuerpo.put("password", "abc");

        registrar(cuerpo)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
            .andExpect(jsonPath("$.errores.password.length()").value(4));
    }

    @Test
    void cadaReglaDeContrasenaSeRechazaPorSeparado() throws Exception {
        for (String debil : new String[] {"segura#2026", "Segura#Clave", "Segura2026", "Ab1!xyz"}) {
            Map<String, Object> cuerpo = solicitud(correoNuevo());
            cuerpo.put("password", debil);
            registrar(cuerpo).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.password.length()").value(1));
        }
    }

    @Test
    void rechazaUnaTiendaInexistente() throws Exception {
        Map<String, Object> cuerpo = solicitud(correoNuevo());
        cuerpo.put("tiendaId", 999_999);

        registrar(cuerpo).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.tiendaId[0]").value("La tienda indicada no existe"));
    }

    @Test
    void rechazaCorreosConFormatoInvalido() throws Exception {
        for (String invalido : new String[] {"sin-arroba", "a@b", "con espacio@x.com", " pre@fijo.com", ""}) {
            registrar(solicitud(invalido)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.email").isNotEmpty());
        }
    }

    @Test
    void rechazaSolicitudesSinCamposObligatorios() throws Exception {
        registrar(new LinkedHashMap<>()).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.email").isNotEmpty())
            .andExpect(jsonPath("$.errores.password").isNotEmpty())
            .andExpect(jsonPath("$.errores.nombreCompleto").isNotEmpty())
            .andExpect(jsonPath("$.errores.tiendaId").isNotEmpty());
    }

    @Test
    void rechazaUnCuerpoQueNoEsJson() throws Exception {
        api.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content("{no es json"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void laListaDeTiendasEsPublicaParaElFormularioDeRegistro() throws Exception {
        api.perform(get("/api/tiendas"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.codigo=='LIM-01')].nombre").value("TechStore Lima Centro"))
            .andExpect(jsonPath("$[?(@.codigo=='AQP-01')]").isNotEmpty())
            .andExpect(jsonPath("$[?(@.codigo=='TRU-01')]").isNotEmpty());
    }
}
