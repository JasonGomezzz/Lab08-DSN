import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router'
import { QRCodeSVG } from 'qrcode.react'
import { almacen, errorTexto, problema, servicios, type PendienteMfa } from '../api'
import { AvisoError } from '../componentes/Avisos'
import { Campo, atributosCampo } from '../componentes/Campo'
import { Nota } from '../componentes/Encabezado'
import { Candado, Destello, Fantasma, FlechaCurva, Marcador } from '../componentes/Ilustraciones'
import { useSesion } from '../sesion'
import type { DatosEnrolamiento, EstadoMfa } from '../tipos'

/** Errores tras los cuales el desafío ya no sirve y hay que volver a iniciar sesión. */
const CODIGOS_FINALES = new Set([
  'MFA_INTENTOS_AGOTADOS',
  'MFA_DESAFIO_INVALIDO',
  'CUENTA_BLOQUEADA',
  'TOKEN_EXPIRADO',
  'TOKEN_INVALIDO',
  'TOKEN_REVOCADO',
  'TOKEN_AUSENTE',
])

/** El login social entrega el token MFA en el fragmento de la URL; se guarda y se borra de la barra de direcciones. */
function recibirDelFragmento(): PendienteMfa | null {
  const datos = new URLSearchParams(window.location.hash.slice(1))
  const token = datos.get('token')
  const estado = datos.get('estado')
  if (!token || (estado !== 'MFA_REQUERIDO' && estado !== 'MFA_ENROLAMIENTO')) return null
  history.replaceState(null, '', window.location.pathname)
  let venceEn = Date.now() + 5 * 60 * 1000
  try {
    const carga = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/'))) as { exp?: number }
    if (carga.exp) venceEn = carga.exp * 1000
  } catch {
    /* si no se puede leer, se asume la vigencia de 5 minutos del servidor */
  }
  const pendiente = { mfaToken: token, estado: estado as EstadoMfa, venceEn }
  almacen.guardarMfa(pendiente)
  return pendiente
}

function formatear(segundos: number) {
  return `${Math.floor(segundos / 60)}:${String(segundos % 60).padStart(2, '0')}`
}

export default function Mfa() {
  const navegar = useNavigate()
  const { iniciar } = useSesion()
  const [pendiente] = useState<PendienteMfa | null>(() => recibirDelFragmento() ?? almacen.mfa())
  const [datos, setDatos] = useState<DatosEnrolamiento | null>(null)
  const [codigo, setCodigo] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [terminado, setTerminado] = useState<string | null>(null)
  const [ahora, setAhora] = useState(() => Date.now())
  const solicitado = useRef(false)
  const entrada = useRef<HTMLInputElement>(null)

  const enrolando = pendiente?.estado === 'MFA_ENROLAMIENTO'
  const restante = pendiente ? Math.max(0, Math.floor((pendiente.venceEn - ahora) / 1000)) : 0
  const vencido = pendiente !== null && restante === 0

  useEffect(() => {
    const reloj = setInterval(() => setAhora(Date.now()), 1000)
    return () => clearInterval(reloj)
  }, [])

  // Pide el secreto una sola vez; si se pidiera dos veces, la segunda clave anularía la que ya se escaneó.
  useEffect(() => {
    if (!enrolando || solicitado.current) return
    solicitado.current = true
    servicios.enrolar().then(setDatos).catch(fallo => terminar(fallo))
  }, [enrolando])

  function terminar(fallo: unknown) {
    almacen.borrarMfa()
    setTerminado(problema(fallo)?.detail ?? errorTexto(fallo))
  }

  function volverAEntrar() {
    almacen.borrarMfa()
    navegar('/login', { replace: true })
  }

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    if (codigo.length !== 6) return
    setEnviando(true)
    setError(null)
    try {
      const sesion = await servicios.verificar(codigo)
      await iniciar(sesion.token)
      navegar('/', { replace: true })
    } catch (fallo) {
      const datosError = problema(fallo)
      if (datosError?.codigo && CODIGOS_FINALES.has(datosError.codigo)) {
        terminar(fallo)
      } else {
        setError(errorTexto(fallo))
        setCodigo('')
        entrada.current?.focus()
      }
    } finally {
      setEnviando(false)
    }
  }

  if (!pendiente) return <Navigate to="/login" replace />

  if (terminado || vencido) {
    return (
      <section className="flex max-w-[640px] flex-col items-start gap-6 pt-4">
        <Fantasma className="h-28 w-28" giro={-8} />
        <h1 className="font-gelica text-heading-lg">se acabó el tiempo.</h1>
        <p className="text-body-sm">
          {terminado ?? 'La verificación en dos pasos dura 5 minutos y ya venció.'} Por seguridad hay que empezar de nuevo.
        </p>
        <button type="button" className="pildora" onClick={volverAEntrar}>
          volver a entrar
        </button>
      </section>
    )
  }

  return (
    <section className="grid items-start gap-12 pt-4 lg:grid-cols-[1.1fr_0.9fr] lg:gap-16">
      <div className="relative">
        <Nota giro={-3}>{enrolando ? 'un paso más, solo una vez,' : 'casi listo,'}</Nota>
        <h1 className="mt-3 break-words font-gelica text-[clamp(2.75rem,7vw,5.25rem)] font-semibold leading-[1.08]">
          {enrolando ? 'activa tu segundo candado.' : 'escribe tu código.'}
        </h1>

        {enrolando ? (
          <ol className="mt-8 max-w-[44ch] list-decimal space-y-2 pl-6 text-body-sm marker:font-geist">
            <li>abre google authenticator, authy o la app de códigos que prefieras.</li>
            <li>escanea el código de abajo, o escribe la clave a mano.</li>
            <li>
              escribe los <Marcador>6 dígitos</Marcador> que te muestra la app.
            </li>
          </ol>
        ) : (
          <p className="mt-8 max-w-[40ch] text-body">
            abre tu app autenticadora y escribe los <Marcador>6 dígitos</Marcador> de TechStore. cambian cada 30 segundos.
          </p>
        )}

        {enrolando && (
          <div className="relative mt-10 inline-block">
            <div className="tarjeta inline-block !p-4">
              {datos ? (
                <QRCodeSVG
                  value={datos.otpauthUri}
                  size={188}
                  level="M"
                  marginSize={1}
                  bgColor="#fdfbf9"
                  fgColor="#171717"
                  title="Código QR para registrar TechStore en tu app autenticadora"
                />
              ) : (
                <div role="status" className="grid h-[188px] w-[188px] animate-pulse place-items-center rounded-campo border border-dashed border-charcoal/40 bg-dew-drop font-geist text-caption">
                  preparando…
                </div>
              )}
            </div>
            <Nota giro={4} className="absolute -right-44 -top-2 hidden w-40 lg:block">
              escanéame
            </Nota>
            <FlechaCurva className="absolute -right-16 top-8 hidden h-24 w-20 lg:block" espejo />
          </div>
        )}

        {enrolando && datos && (
          <p className="mt-6 max-w-[44ch] font-geist text-caption">
            ¿no puedes escanear? escribe esta clave:{' '}
            <code className="select-all rounded-campo border border-dashed border-charcoal bg-dew-drop px-2 py-0.5 tracking-wider">
              {datos.secreto.match(/.{1,4}/g)?.join(' ')}
            </code>
          </p>
        )}
      </div>

      <div className="relative">
        <Candado className="absolute -left-8 -top-10 z-10 hidden h-20 w-20 sm:block" giro={-9} retraso={120} />
        <Destello className="absolute -bottom-7 -right-4 z-10 hidden h-14 w-14 sm:block" giro={13} retraso={260} />
        <form onSubmit={enviar} className="tarjeta space-y-5">
          <h2 className="font-gelica text-heading-sm">código de 6 dígitos</h2>
          {error && <AvisoError>{error}</AvisoError>}
          <Campo id="codigo" etiqueta="código de tu app" ayuda={`Tienes 3 intentos. Esta verificación vence en ${formatear(restante)}.`}>
            <input
              {...atributosCampo('codigo')}
              ref={entrada}
              className="campo text-center text-[2rem] tracking-[0.45em] tabular-nums"
              inputMode="numeric"
              pattern="[0-9]*"
              autoComplete="one-time-code"
              maxLength={6}
              required
              autoFocus
              value={codigo}
              onChange={e => setCodigo(e.target.value.replace(/\D/g, '').slice(0, 6))}
              placeholder="······"
            />
          </Campo>
          <button
            type="submit"
            className="pildora w-full"
            disabled={enviando || codigo.length !== 6 || (enrolando && !datos)}
            aria-busy={enviando}
          >
            {enviando ? 'verificando…' : 'verificar y entrar'}
          </button>
          <button type="button" className="pildora pildora-chica w-full" onClick={volverAEntrar}>
            cancelar y volver
          </button>
        </form>
      </div>
    </section>
  )
}
