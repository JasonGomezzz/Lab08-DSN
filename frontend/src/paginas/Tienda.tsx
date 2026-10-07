import { useEffect, useState, type FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router'
import { errorTexto, servicios } from '../api'
import { AvisoError } from '../componentes/Avisos'
import { Encabezado } from '../componentes/Encabezado'
import { CajaSonriente, Marcador, Visto } from '../componentes/Ilustraciones'
import { useSesion } from '../sesion'
import type { Tienda as TiendaDatos } from '../tipos'

/** Quien entra con Google o GitHub llega sin tienda: la elige una sola vez y luego solo un administrador la cambia. */
export default function Tienda() {
  const { perfil, recargar } = useSesion()
  const navegar = useNavigate()
  const [tiendas, setTiendas] = useState<TiendaDatos[] | null>(null)
  const [elegida, setElegida] = useState<number | null>(null)
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    servicios.tiendas().then(setTiendas).catch(fallo => {
      setTiendas([])
      setError(errorTexto(fallo))
    })
  }, [])

  if (perfil?.tienda) return <Navigate to="/" replace />

  async function guardar(evento: FormEvent) {
    evento.preventDefault()
    if (elegida === null) return
    setEnviando(true)
    setError(null)
    try {
      await servicios.elegirTienda(elegida)
      await recargar()
      navegar('/', { replace: true })
    } catch (fallo) {
      setError(errorTexto(fallo))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section className="max-w-[760px]">
      <CajaSonriente className="mb-6 h-24 w-24" giro={-8} />
      <Encabezado titulo="¿en qué tienda trabajas?" nota="solo lo eliges una vez" />
      <p className="mb-8 max-w-[52ch] text-body-sm">
        Tu cuenta todavía no tiene tienda. Con ella verás y actualizarás el stock de <Marcador>tu tienda</Marcador>; para
        cambiarla después necesitarás a un administrador.
      </p>
      <form onSubmit={guardar} className="space-y-6">
        {error && <AvisoError>{error}</AvisoError>}
        <fieldset className="grid gap-3 sm:grid-cols-2">
          <legend className="rotulo mb-3">elige tu tienda</legend>
          {tiendas === null && <p className="ayuda">cargando tiendas…</p>}
          {tiendas?.map(tienda => (
            <label key={tienda.id} className="block cursor-pointer">
              <input
                type="radio"
                name="tienda"
                className="peer sr-only"
                checked={elegida === tienda.id}
                onChange={() => setElegida(tienda.id)}
              />
              <span className="flex items-center justify-between gap-3 rounded-tarjeta border-[1.5px] border-charcoal bg-cream-paper p-5 shadow-papel transition-colors peer-checked:border-[3px] peer-checked:bg-dew-drop peer-focus-visible:outline-2 peer-focus-visible:outline-offset-4 peer-focus-visible:outline-charcoal">
                <span>
                  <span className="block font-gelica text-subheading font-medium leading-tight">{tienda.nombre}</span>
                  <span className="block font-geist text-caption text-charcoal/75">{tienda.ciudad}</span>
                </span>
                {elegida === tienda.id && <Visto className="h-7 w-7 shrink-0" />}
              </span>
            </label>
          ))}
        </fieldset>
        <button type="submit" className="pildora" disabled={enviando || elegida === null} aria-busy={enviando}>
          {enviando ? 'guardando…' : 'guardar mi tienda'}
        </button>
      </form>
    </section>
  )
}
