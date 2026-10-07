export type Rol = 'ADMINISTRADOR' | 'GERENTE_TIENDA' | 'EMPLEADO_VENTAS' | 'AUDITOR'

export type Accion =
  | 'VER_PRODUCTOS'
  | 'CREAR_PRODUCTO'
  | 'EDITAR_PRODUCTO'
  | 'ACTUALIZAR_STOCK'
  | 'ELIMINAR_PRODUCTO'
  | 'VER_REPORTE'
  | 'VER_USUARIOS'
  | 'GESTIONAR_USUARIOS'

export type Proveedor = 'LOCAL' | 'GOOGLE' | 'GITHUB'

export const etiquetasRol: Record<Rol, string> = {
  ADMINISTRADOR: 'administrador',
  GERENTE_TIENDA: 'gerente de tienda',
  EMPLEADO_VENTAS: 'empleado de ventas',
  AUDITOR: 'auditor',
}

export interface Tienda {
  id: number
  codigo: string
  nombre: string
  ciudad: string
}

export interface Perfil {
  id: number
  email: string
  nombreCompleto: string
  rol: Rol
  tienda: Tienda | null
  proveedor: Proveedor
  mfaHabilitado: boolean
  permisos: Accion[]
}

export interface Producto {
  id: number
  sku: string
  nombre: string
  categoria: string
  precio: number
  stock: number
  tienda: Tienda
}

export interface DatosProducto {
  sku: string
  nombre: string
  categoria: string
  precio: string
  stock: number
  tiendaId?: number
}

export interface UsuarioAdmin {
  id: number
  email: string
  nombreCompleto: string
  rol: Rol
  tienda: Tienda | null
  proveedor: Proveedor
  mfaHabilitado: boolean
  bloqueado: boolean
  bloqueadoHasta: string | null
}

export interface ResumenTienda {
  tiendaId: number | null
  codigo: string
  nombre: string
  productos: number
  unidades: number
  valorInventario: number
  productosConPocoStock: number
}

export interface ReporteInventario {
  generadoEn: string
  umbralStockBajo: number
  tiendas: ResumenTienda[]
  total: ResumenTienda
}

export type EstadoMfa = 'MFA_REQUERIDO' | 'MFA_ENROLAMIENTO'

export interface ResultadoLogin {
  estado: EstadoMfa
  mfaToken: string
  expiraEnSegundos: number
}

export interface DatosEnrolamiento {
  secreto: string
  otpauthUri: string
}

export interface SesionIniciada {
  token: string
  tipo: string
  expiraEnSegundos: number
}

/** Cuerpo de error de la API (application/problem+json) con el código estable que decide el mensaje. */
export interface ProblemaApi {
  codigo?: string
  title?: string
  detail?: string
  errores?: Record<string, string[]>
  intentosRestantes?: number
  minutosRestantes?: number
}
