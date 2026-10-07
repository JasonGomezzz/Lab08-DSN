import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useNavigate } from 'react-router'
import { almacen, EVENTO_SESION_EXPIRADA, servicios } from './api'
import type { Accion, Perfil } from './tipos'

interface ContextoSesion {
  perfil: Perfil | null
  cargando: boolean
  puede: (accion: Accion) => boolean
  /** Guarda el token completo y carga el perfil de quien acaba de entrar. */
  iniciar: (token: string) => Promise<Perfil>
  recargar: () => Promise<Perfil | null>
  salir: () => Promise<void>
}

const Contexto = createContext<ContextoSesion | null>(null)

export function ProveedorSesion({ children }: { children: ReactNode }) {
  const navegar = useNavigate()
  const [perfil, setPerfil] = useState<Perfil | null>(null)
  const [cargando, setCargando] = useState(() => almacen.token() !== null)

  useEffect(() => {
    if (!almacen.token()) return
    let vigente = true
    servicios
      .perfil()
      .then(datos => vigente && setPerfil(datos))
      .catch(() => almacen.borrarToken())
      .finally(() => vigente && setCargando(false))
    return () => {
      vigente = false
    }
  }, [])

  useEffect(() => {
    const alExpirar = () => {
      setPerfil(null)
      navegar('/login', { replace: true, state: { aviso: 'Tu sesión venció. Entra de nuevo para continuar.' } })
    }
    window.addEventListener(EVENTO_SESION_EXPIRADA, alExpirar)
    return () => window.removeEventListener(EVENTO_SESION_EXPIRADA, alExpirar)
  }, [navegar])

  const iniciar = useCallback(async (token: string) => {
    almacen.guardarToken(token)
    almacen.borrarMfa()
    const datos = await servicios.perfil()
    setPerfil(datos)
    return datos
  }, [])

  const recargar = useCallback(async () => {
    if (!almacen.token()) return null
    const datos = await servicios.perfil()
    setPerfil(datos)
    return datos
  }, [])

  const salir = useCallback(async () => {
    try {
      await servicios.logout()
    } catch {
      /* el token puede haber vencido ya: igual se cierra la sesión local */
    }
    almacen.borrarToken()
    almacen.borrarMfa()
    setPerfil(null)
    navegar('/login', { replace: true })
  }, [navegar])

  const valor = useMemo<ContextoSesion>(
    () => ({
      perfil,
      cargando,
      puede: accion => perfil?.permisos.includes(accion) ?? false,
      iniciar,
      recargar,
      salir,
    }),
    [perfil, cargando, iniciar, recargar, salir],
  )

  return <Contexto.Provider value={valor}>{children}</Contexto.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useSesion(): ContextoSesion {
  const contexto = useContext(Contexto)
  if (!contexto) throw new Error('useSesion debe usarse dentro de ProveedorSesion')
  return contexto
}
