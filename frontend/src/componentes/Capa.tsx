import { Link, NavLink, Outlet, useLocation } from 'react-router'
import { useSesion } from '../sesion'
import { etiquetasRol } from '../tipos'
import { MarcaMano, Marcador } from './Ilustraciones'

export function Capa() {
  const { perfil, puede, salir } = useSesion()
  const { pathname } = useLocation()

  const vinculos = [
    { a: '/inventario', texto: 'inventario', visible: puede('VER_PRODUCTOS') },
    { a: '/reportes', texto: 'reportes', visible: puede('VER_REPORTE') },
    { a: '/usuarios', texto: 'usuarios', visible: puede('VER_USUARIOS') },
  ].filter(vinculo => vinculo.visible)
  const mostrarNavegacion = perfil !== null && !(perfil.tienda === null && vinculos.length > 0 && pathname === '/tienda')

  return (
    <div className="flex min-h-dvh flex-col">
      <a href="#contenido" className="saltar pildora">
        saltar al contenido
      </a>

      <header className="mx-auto flex w-full max-w-[1200px] items-center justify-between gap-4 px-4 py-4 sm:px-8">
        <Link to="/" aria-label="TechStore, ir al inicio" className="rounded-campo">
          <MarcaMano className="h-8 w-8" />
        </Link>
        <div className="flex items-center gap-4">
          {perfil ? (
            <>
              <p className="hidden text-right font-geist text-caption leading-tight sm:block">
                {perfil.nombreCompleto}
                <span className="block text-sm text-charcoal/75">
                  {etiquetasRol[perfil.rol]}
                  {perfil.tienda ? ` · ${perfil.tienda.nombre}` : ''}
                </span>
              </p>
              <button type="button" className="pildora" onClick={() => void salir()}>
                salir
              </button>
            </>
          ) : pathname === '/registro' ? (
            <Link className="pildora" to="/login">
              entrar
            </Link>
          ) : pathname === '/login' ? (
            <Link className="pildora" to="/registro">
              crear cuenta
            </Link>
          ) : null}
        </div>
      </header>

      {mostrarNavegacion && vinculos.length > 0 && (
        <nav aria-label="Secciones" className="mx-auto w-full max-w-[1200px] px-4 sm:px-8">
          <ul className="flex flex-wrap gap-x-8 gap-y-2 py-2 font-geist text-body-sm">
            {vinculos.map(vinculo => (
              <li key={vinculo.a}>
                <NavLink to={vinculo.a} className="rounded-campo px-1 py-1 no-underline hover:underline">
                  {({ isActive }) => (isActive ? <Marcador>{vinculo.texto}</Marcador> : vinculo.texto)}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
      )}

      <main id="contenido" className="mx-auto w-full max-w-[1200px] flex-1 px-4 pb-20 pt-6 sm:px-8">
        <Outlet />
      </main>

      <footer className="pie-marcador px-6 py-6 sm:px-12">
        <p className="mx-auto max-w-[1200px] text-caption font-medium sm:text-body-sm">
          techstore · laboratorio 8 · desarrollo de soluciones en la nube
        </p>
      </footer>
    </div>
  )
}
