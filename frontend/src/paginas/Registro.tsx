import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router'
import { errorTexto, erroresPorCampo, servicios } from '../api'
import { AvisoError } from '../componentes/Avisos'
import { Campo, atributosCampo } from '../componentes/Campo'
import { Nota } from '../componentes/Encabezado'
import { Destello, Marcador, Pendiente, Rayo, Visto, CajaSonriente } from '../componentes/Ilustraciones'
import type { Tienda } from '../tipos'

/** Mismas reglas que valida el servidor; aquí solo sirven para guiar mientras se escribe. */
function reglasPassword(password: string) {
  return [
    { texto: 'entre 8 y 64 caracteres', cumple: password.length >= 8 && password.length <= 64 },
    { texto: 'una letra mayúscula', cumple: /\p{Lu}/u.test(password) },
    { texto: 'un número', cumple: /\p{Nd}/u.test(password) },
    { texto: 'un carácter especial', cumple: /[^\p{L}\p{N}\s]/u.test(password) },
  ]
}

export default function Registro() {
  const navegar = useNavigate()
  const [tiendas, setTiendas] = useState<Tienda[] | null>(null)
  const [nombre, setNombre] = useState('')
  const [email, setEmail] = useState('')
  const [tiendaId, setTiendaId] = useState('')
  const [password, setPassword] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [campos, setCampos] = useState<Record<string, string[]>>({})

  useEffect(() => {
    servicios.tiendas().then(setTiendas).catch(() => {
      setTiendas([])
      setError('No pudimos cargar la lista de tiendas. Recarga la página.')
    })
  }, [])

  const reglas = useMemo(() => reglasPassword(password), [password])
  const completo = nombre.trim().length >= 3 && email && tiendaId && reglas.every(regla => regla.cumple)

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setEnviando(true)
    setError(null)
    setCampos({})
    try {
      await servicios.registrar({ email: email.trim(), password, nombreCompleto: nombre.trim(), tiendaId: Number(tiendaId) })
      navegar('/login', { state: { registrado: email.trim() } })
    } catch (fallo) {
      const porCampo = erroresPorCampo(fallo)
      setCampos(porCampo)
      if (Object.keys(porCampo).length === 0) setError(errorTexto(fallo))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section className="grid items-start gap-14 pt-4 lg:grid-cols-[1fr_1fr] lg:gap-16">
      <div className="relative">
        <Nota giro={-3}>primera vez por aquí?</Nota>
        <h1 className="mt-3 break-words font-gelica text-[clamp(2.75rem,7vw,5.25rem)] font-semibold leading-[1.08]">
          crea tu cuenta.
        </h1>
        <p className="mt-8 max-w-[38ch] text-body">
          tu cuenta nace como <Marcador>empleado de ventas</Marcador> de la tienda que elijas. si necesitas otro perfil, un
          administrador te lo cambia después.
        </p>
        <CajaSonriente className="mt-12 hidden h-32 w-32 lg:block" giro={-7} retraso={160} />
        <Destello className="absolute right-6 top-[46%] hidden h-14 w-14 lg:block" giro={14} retraso={300} />
      </div>

      <div className="relative">
        <Rayo className="absolute -right-5 -top-10 z-10 hidden h-20 w-20 sm:block" giro={11} retraso={120} />
        <form onSubmit={enviar} className="tarjeta space-y-5">
          <h2 className="font-gelica text-heading-sm">tus datos</h2>
          {error && <AvisoError>{error}</AvisoError>}

          <Campo id="nombre" etiqueta="nombre completo" error={campos.nombreCompleto?.[0]}>
            <input
              {...atributosCampo('nombre', campos.nombreCompleto?.[0])}
              className="campo"
              autoComplete="name"
              required
              value={nombre}
              onChange={e => setNombre(e.target.value)}
            />
          </Campo>
          <Campo id="email" etiqueta="correo" error={campos.email?.[0]}>
            <input
              {...atributosCampo('email', campos.email?.[0])}
              className="campo"
              type="email"
              autoComplete="username"
              required
              value={email}
              onChange={e => setEmail(e.target.value)}
            />
          </Campo>
          <Campo id="tienda" etiqueta="tienda" error={campos.tiendaId?.[0]}>
            <select
              {...atributosCampo('tienda', campos.tiendaId?.[0])}
              className="campo"
              required
              value={tiendaId}
              onChange={e => setTiendaId(e.target.value)}
              disabled={tiendas === null}
            >
              <option value="">{tiendas === null ? 'cargando tiendas…' : 'elige tu tienda'}</option>
              {tiendas?.map(tienda => (
                <option key={tienda.id} value={tienda.id}>
                  {tienda.nombre}
                </option>
              ))}
            </select>
          </Campo>
          <Campo id="password" etiqueta="contraseña" error={campos.password?.join(' ')}>
            <input
              {...atributosCampo('password', campos.password?.join(' '))}
              className="campo"
              type="password"
              autoComplete="new-password"
              required
              value={password}
              onChange={e => setPassword(e.target.value)}
              aria-describedby={['reglas-password', campos.password ? 'password-error' : null].filter(Boolean).join(' ')}
            />
          </Campo>

          <ul id="reglas-password" className="space-y-1 font-geist text-caption" aria-label="Reglas de la contraseña">
            {reglas.map(regla => (
              <li key={regla.texto} className="flex items-center gap-2">
                {regla.cumple ? <Visto className="h-5 w-5" /> : <Pendiente className="h-5 w-5" />}
                <span className={regla.cumple ? '' : 'text-charcoal/75'}>
                  {regla.texto}
                  <span className="sr-only">{regla.cumple ? ' (cumplida)' : ' (pendiente)'}</span>
                </span>
              </li>
            ))}
          </ul>

          <button type="submit" className="pildora w-full" disabled={enviando || !completo} aria-busy={enviando}>
            {enviando ? 'creando tu cuenta…' : 'crear cuenta'}
          </button>
        </form>
      </div>
    </section>
  )
}
