import { Navigate, Outlet, useLocation } from 'react-router'
import { useSesion } from '../sesion'
import type { Accion, Perfil } from '../tipos'
import { Encabezado } from './Encabezado'
import { Fantasma } from './Ilustraciones'

/** Los perfiles de tienda no pueden operar hasta tener una tienda asignada. */
function necesitaTienda(perfil: Perfil) {
  return perfil.tienda === null && (perfil.rol === 'GERENTE_TIENDA' || perfil.rol === 'EMPLEADO_VENTAS')
}

export function Cargando() {
  return (
    <div role="status" aria-live="polite" className="space-y-4 py-8">
      <p className="font-gelica text-subheading">abriendo el cuaderno…</p>
      {[0, 1, 2].map(fila => (
        <div key={fila} className="h-9 animate-pulse rounded-campo border border-dashed border-charcoal/40 bg-dew-drop" />
      ))}
    </div>
  )
}

/** Rutas de acceso (login, registro): quien ya tiene sesión pasa directo al inventario. */
export function RutaPublica() {
  const { perfil, cargando } = useSesion()
  if (cargando) return <Cargando />
  if (perfil) return <Navigate to={necesitaTienda(perfil) ? '/tienda' : '/'} replace />
  return <Outlet />
}

export function RutaProtegida() {
  const { perfil, cargando } = useSesion()
  const ubicacion = useLocation()
  if (cargando) return <Cargando />
  if (!perfil) return <Navigate to="/login" replace />
  if (necesitaTienda(perfil) && ubicacion.pathname !== '/tienda') return <Navigate to="/tienda" replace />
  return <Outlet />
}

export function RutaConPermiso({ accion }: { accion: Accion }) {
  const { puede } = useSesion()
  return puede(accion) ? <Outlet /> : <SinAcceso />
}

export function SinAcceso() {
  return (
    <section className="flex flex-col items-start gap-6 py-8">
      <Fantasma className="h-28 w-28" giro={-8} />
      <Encabezado titulo="esta hoja no es para tu perfil" nota="pide acceso a un administrador" />
      <p className="max-w-[60ch] text-body-sm">
        Tu perfil no tiene permiso para ver esta sección. Si crees que es un error, avisa a quien administra TechStore.
      </p>
    </section>
  )
}

export function NoEncontrada() {
  return (
    <section className="flex flex-col items-start gap-6 py-8">
      <Fantasma className="h-28 w-28" giro={8} />
      <Encabezado titulo="no encontramos esta página" nota="se borró con el marcador" />
    </section>
  )
}
