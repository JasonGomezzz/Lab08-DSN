import axios from 'axios'
import type {
  DatosEnrolamiento, DatosProducto, EstadoMfa, Perfil, Producto, ProblemaApi, ReporteInventario, ResultadoLogin,
  Rol, SesionIniciada, Tienda, UsuarioAdmin,
} from './tipos'

const CLAVE_TOKEN = 'techstore_token'
const CLAVE_MFA = 'techstore_mfa'
export const EVENTO_SESION_EXPIRADA = 'techstore:sesion-expirada'

export interface PendienteMfa {
  mfaToken: string
  estado: EstadoMfa
  /** Marca de tiempo (ms) en la que vence el desafío; el servidor lo invalida a los 5 minutos. */
  venceEn: number
}

/**
 * El token vive en sessionStorage: se borra al cerrar la pestaña, pero JavaScript de la página puede leerlo.
 * Por eso la autorización real está en el servidor y esta interfaz solo decide qué mostrar.
 */
function leer(clave: string): string | null {
  try {
    return sessionStorage.getItem(clave)
  } catch {
    return null
  }
}

function escribir(clave: string, valor: string | null) {
  try {
    if (valor === null) sessionStorage.removeItem(clave)
    else sessionStorage.setItem(clave, valor)
  } catch {
    /* sessionStorage no disponible: la sesión simplemente no sobrevive a una recarga */
  }
}

export const almacen = {
  token: () => leer(CLAVE_TOKEN),
  guardarToken: (token: string) => escribir(CLAVE_TOKEN, token),
  borrarToken: () => escribir(CLAVE_TOKEN, null),
  mfa(): PendienteMfa | null {
    const texto = leer(CLAVE_MFA)
    if (!texto) return null
    try {
      return JSON.parse(texto) as PendienteMfa
    } catch {
      return null
    }
  },
  guardarMfa: (pendiente: PendienteMfa) => escribir(CLAVE_MFA, JSON.stringify(pendiente)),
  borrarMfa: () => escribir(CLAVE_MFA, null),
}

export const api = axios.create({ baseURL: import.meta.env.VITE_API_URL || '/api' })

api.interceptors.request.use(config => {
  const esMfa = (config.url ?? '').startsWith('/auth/mfa')
  const token = esMfa ? almacen.mfa()?.mfaToken : almacen.token()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  respuesta => respuesta,
  error => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      const codigo = (error.response.data as ProblemaApi | undefined)?.codigo
      const esMfa = (error.config?.url ?? '').startsWith('/auth/mfa')
      if (codigo?.startsWith('TOKEN_') && !esMfa && almacen.token()) {
        almacen.borrarToken()
        window.dispatchEvent(new Event(EVENTO_SESION_EXPIRADA))
      }
    }
    return Promise.reject(error)
  },
)

export function problema(error: unknown): ProblemaApi | null {
  if (axios.isAxiosError(error) && error.response?.data && typeof error.response.data === 'object') {
    return error.response.data as ProblemaApi
  }
  return null
}

export function erroresPorCampo(error: unknown): Record<string, string[]> {
  return problema(error)?.errores ?? {}
}

export function errorTexto(error: unknown, porDefecto = 'No pudimos completar la operación. Intenta de nuevo.'): string {
  if (axios.isAxiosError(error) && !error.response) return 'No hay conexión con el servidor. Revisa tu red e intenta de nuevo.'
  const datos = problema(error)
  if (!datos) return porDefecto
  const detalles = Object.values(datos.errores ?? {}).flat()
  return detalles.length > 0 ? detalles.join(' ') : datos.detail || datos.title || porDefecto
}

export const servicios = {
  proveedores: () => api.get<string[]>('/auth/proveedores').then(r => r.data),
  tiendas: () => api.get<Tienda[]>('/tiendas').then(r => r.data),
  registrar: (datos: { email: string; password: string; nombreCompleto: string; tiendaId: number }) =>
    api.post('/auth/registro', datos).then(r => r.data),
  login: (email: string, password: string) =>
    api.post<ResultadoLogin>('/auth/login', { email, password }).then(r => r.data),
  enrolar: () => api.post<DatosEnrolamiento>('/auth/mfa/enrolar').then(r => r.data),
  verificar: (codigo: string) => api.post<SesionIniciada>('/auth/mfa/verificar', { codigo }).then(r => r.data),
  logout: () => api.post('/auth/logout'),
  perfil: () => api.get<Perfil>('/auth/me').then(r => r.data),
  elegirTienda: (tiendaId: number) => api.put<Perfil>('/auth/me/tienda', { tiendaId }).then(r => r.data),

  productos: (tiendaId?: number) =>
    api.get<Producto[]>('/productos', { params: tiendaId ? { tiendaId } : undefined }).then(r => r.data),
  crearProducto: (datos: DatosProducto) => api.post<Producto>('/productos', datos).then(r => r.data),
  editarProducto: (id: number, datos: { nombre: string; categoria: string; precio: string }) =>
    api.put<Producto>(`/productos/${id}`, datos).then(r => r.data),
  ajustarStock: (id: number, ajuste: number) =>
    api.patch<Producto>(`/productos/${id}/stock`, { ajuste }).then(r => r.data),
  eliminarProducto: (id: number) => api.delete(`/productos/${id}`),

  reporte: () => api.get<ReporteInventario>('/reportes/inventario').then(r => r.data),

  usuarios: () => api.get<UsuarioAdmin[]>('/usuarios').then(r => r.data),
  cambiarRol: (id: number, rol: Rol) => api.patch<UsuarioAdmin>(`/usuarios/${id}/rol`, { rol }).then(r => r.data),
  asignarTienda: (id: number, tiendaId: number) =>
    api.patch<UsuarioAdmin>(`/usuarios/${id}/tienda`, { tiendaId }).then(r => r.data),
  desbloquear: (id: number) => api.post<UsuarioAdmin>(`/usuarios/${id}/desbloquear`).then(r => r.data),
  reiniciarMfa: (id: number) => api.post<UsuarioAdmin>(`/usuarios/${id}/reiniciar-mfa`).then(r => r.data),
}
