import { useEffect, useState, type FormEvent } from 'react'
import { useLocation, useNavigate } from 'react-router'
import { almacen, errorTexto, servicios } from '../api'
import { AvisoError, AvisoInfo } from '../componentes/Avisos'
import { Campo, atributosCampo } from '../componentes/Campo'
import { Nota } from '../componentes/Encabezado'
import { EtiquetaLaminada } from '../componentes/Etiqueta'
import { CorazonConOjos, Destello, Marcador, Rayo } from '../componentes/Ilustraciones'

const mensajesSocial: Record<string, string> = {
  CUENTA_EXISTENTE: 'Ya existe una cuenta con ese correo registrada con contraseña. Entra con tu correo y tu contraseña.',
  EMAIL_NO_VERIFICADO: 'Tu proveedor no entregó un correo verificado, así que no podemos crear la cuenta.',
  CUENTA_BLOQUEADA: 'Tu cuenta está bloqueada por demasiados intentos fallidos. Intenta de nuevo en unos minutos.',
  LOGIN_SOCIAL_FALLIDO: 'No pudimos completar el inicio de sesión con tu proveedor. Intenta de nuevo.',
}

const nombresProveedor: Record<string, string> = { google: 'google', github: 'github' }

export default function Login() {
  const navegar = useNavigate()
  const ubicacion = useLocation()
  const estado = ubicacion.state as { aviso?: string; registrado?: string } | null

  const [email, setEmail] = useState(estado?.registrado ?? '')
  const [password, setPassword] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [proveedores, setProveedores] = useState<string[] | null>(null)

  // El login social devuelve el error en el fragmento de la URL (no llega al servidor ni a los registros).
  useEffect(() => {
    const codigo = new URLSearchParams(window.location.hash.slice(1)).get('error')
    if (codigo) {
      setError(mensajesSocial[codigo] ?? mensajesSocial.LOGIN_SOCIAL_FALLIDO)
      history.replaceState(null, '', window.location.pathname)
    }
  }, [])

  useEffect(() => {
    servicios.proveedores().then(setProveedores).catch(() => setProveedores([]))
  }, [])

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setEnviando(true)
    setError(null)
    try {
      const resultado = await servicios.login(email.trim(), password)
      almacen.guardarMfa({
        mfaToken: resultado.mfaToken,
        estado: resultado.estado,
        venceEn: Date.now() + resultado.expiraEnSegundos * 1000,
      })
      navegar('/mfa')
    } catch (fallo) {
      setError(errorTexto(fallo))
      setPassword('')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section className="grid items-start gap-14 pt-4 lg:grid-cols-[1.15fr_0.85fr] lg:gap-16">
      <div className="relative">
        <Nota giro={-3}>hola de nuevo,</Nota>
        <h1 className="mt-3 break-words font-gelica text-[clamp(3rem,8.4vw,6.5rem)] font-semibold leading-[1.08]">
          tu inventario, bajo llave.
        </h1>
        <p className="mt-8 max-w-[36ch] text-body">
          entra con tu correo o con <Marcador>google o github</Marcador>. después te pedimos un código de 6 dígitos de tu
          app autenticadora.
        </p>
        <EtiquetaLaminada
          className="mt-12 hidden lg:block"
          giro={-4}
          filas={[
            { campo: 'producto', valor: 'laptop pro 14"' },
            { campo: 'tienda', valor: 'lima centro' },
            { campo: 'stock', valor: '12 unidades' },
          ]}
        />
        <Destello className="absolute -right-2 top-[42%] hidden h-14 w-14 lg:block" giro={12} retraso={260} />
      </div>

      <div className="relative">
        <Rayo className="absolute -left-7 -top-9 z-10 hidden h-20 w-20 sm:block" giro={-12} retraso={120} />
        <CorazonConOjos className="absolute -bottom-14 right-8 z-10 hidden h-20 w-20 sm:block" giro={9} retraso={220} />

        <form onSubmit={enviar} className="tarjeta space-y-5">
          <h2 className="font-gelica text-heading-sm">entrar</h2>
          {estado?.registrado && <AvisoInfo>Tu cuenta quedó creada. Entra con tu correo y tu contraseña.</AvisoInfo>}
          {estado?.aviso && <AvisoInfo>{estado.aviso}</AvisoInfo>}
          {error && <AvisoError>{error}</AvisoError>}

          <Campo id="email" etiqueta="correo">
            <input
              {...atributosCampo('email')}
              className="campo"
              type="email"
              autoComplete="username"
              required
              value={email}
              onChange={e => setEmail(e.target.value)}
              placeholder="tu@correo.com"
            />
          </Campo>
          <Campo id="password" etiqueta="contraseña">
            <input
              {...atributosCampo('password')}
              className="campo"
              type="password"
              autoComplete="current-password"
              required
              value={password}
              onChange={e => setPassword(e.target.value)}
            />
          </Campo>

          <button type="submit" className="pildora w-full" disabled={enviando || !email || !password} aria-busy={enviando}>
            {enviando ? 'entrando…' : 'entrar'}
          </button>

          <div className="border-t border-dashed border-charcoal/40 pt-5">
            {proveedores === null ? (
              <p className="ayuda">buscando formas de entrar…</p>
            ) : proveedores.length > 0 ? (
              <div className="space-y-3">
                <p className="ayuda !mt-0">o entra con</p>
                {proveedores.map(proveedor => (
                  <a key={proveedor} href={`/oauth2/authorization/${proveedor}`} className="pildora w-full">
                    seguir con {nombresProveedor[proveedor] ?? proveedor}
                  </a>
                ))}
              </div>
            ) : (
              <p className="ayuda !mt-0">
                El acceso con Google y GitHub se activa cuando el servidor tiene sus credenciales configuradas; en este
                entorno todavía no.
              </p>
            )}
          </div>
        </form>
      </div>
    </section>
  )
}
