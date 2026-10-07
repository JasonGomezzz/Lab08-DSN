import { useEffect, useState } from 'react'
import { errorTexto, servicios } from '../api'
import { AvisoError, AvisoInfo } from '../componentes/Avisos'
import { Encabezado } from '../componentes/Encabezado'
import { useSesion } from '../sesion'
import { etiquetasRol, type Rol, type Tienda, type UsuarioAdmin } from '../tipos'

const roles = Object.keys(etiquetasRol) as Rol[]

export default function Usuarios() {
  const { perfil, puede } = useSesion()
  const puedeGestionar = puede('GESTIONAR_USUARIOS')
  const [usuarios, setUsuarios] = useState<UsuarioAdmin[] | null>(null)
  const [tiendas, setTiendas] = useState<Tienda[]>([])
  const [error, setError] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const [ocupado, setOcupado] = useState<number | null>(null)

  useEffect(() => {
    servicios.usuarios().then(setUsuarios).catch(fallo => {
      setError(errorTexto(fallo))
      setUsuarios([])
    })
    servicios.tiendas().then(setTiendas).catch(() => undefined)
  }, [])

  async function aplicar(usuario: UsuarioAdmin, accion: Promise<UsuarioAdmin>, mensaje: string) {
    setOcupado(usuario.id)
    setError(null)
    try {
      const actualizado = await accion
      setUsuarios(lista => lista?.map(item => (item.id === actualizado.id ? actualizado : item)) ?? null)
      setAviso(mensaje)
    } catch (fallo) {
      setError(errorTexto(fallo))
    } finally {
      setOcupado(null)
    }
  }

  return (
    <section>
      <Encabezado titulo="usuarios" nota={puedeGestionar ? 'tú decides quién hace qué' : 'solo lectura'} />

      <div className="mb-6 space-y-3" aria-live="polite">
        {error && <AvisoError>{error}</AvisoError>}
        {aviso && !error && <AvisoInfo>{aviso}</AvisoInfo>}
      </div>

      {usuarios === null ? (
        <div role="status" className="space-y-3">
          <span className="sr-only">cargando usuarios</span>
          {[0, 1, 2, 3].map(fila => (
            <div key={fila} className="h-14 animate-pulse rounded-campo border border-dashed border-charcoal/40 bg-dew-drop" />
          ))}
        </div>
      ) : (
        <table className="libro">
          <caption className="sr-only">Usuarios con su perfil, tienda y estado de seguridad</caption>
          <thead>
            <tr>
              <th scope="col">persona</th>
              <th scope="col">perfil</th>
              <th scope="col">tienda</th>
              <th scope="col">seguridad</th>
              {puedeGestionar && <th scope="col">acciones</th>}
            </tr>
          </thead>
          <tbody>
            {usuarios.map(usuario => {
              const esYo = usuario.id === perfil?.id
              return (
                <tr key={usuario.id}>
                  <td data-label="persona">
                    <div>
                      <span className="block font-medium">
                        {usuario.nombreCompleto}
                        {esYo && <span className="ml-2 tag">tú</span>}
                      </span>
                      <span className="block text-sm text-charcoal/75">{usuario.email}</span>
                    </div>
                  </td>
                  <td data-label="perfil">
                    {puedeGestionar ? (
                      <>
                        <label htmlFor={`rol-${usuario.id}`} className="sr-only">
                          Perfil de {usuario.nombreCompleto}
                        </label>
                        <select
                          id={`rol-${usuario.id}`}
                          className="campo !w-auto"
                          value={usuario.rol}
                          disabled={esYo || ocupado === usuario.id}
                          title={esYo ? 'No puedes cambiar tu propio perfil' : undefined}
                          onChange={e =>
                            void aplicar(usuario, servicios.cambiarRol(usuario.id, e.target.value as Rol),
                              `${usuario.nombreCompleto} ahora es ${etiquetasRol[e.target.value as Rol]}.`)
                          }
                        >
                          {roles.map(rol => (
                            <option key={rol} value={rol}>
                              {etiquetasRol[rol]}
                            </option>
                          ))}
                        </select>
                      </>
                    ) : (
                      etiquetasRol[usuario.rol]
                    )}
                  </td>
                  <td data-label="tienda">
                    {puedeGestionar ? (
                      <>
                        <label htmlFor={`tienda-${usuario.id}`} className="sr-only">
                          Tienda de {usuario.nombreCompleto}
                        </label>
                        <select
                          id={`tienda-${usuario.id}`}
                          className="campo !w-auto"
                          value={usuario.tienda?.id ?? ''}
                          disabled={ocupado === usuario.id}
                          onChange={e =>
                            e.target.value
                              ? void aplicar(usuario, servicios.asignarTienda(usuario.id, Number(e.target.value)),
                                  `Cambiaste la tienda de ${usuario.nombreCompleto}.`)
                              : undefined
                          }
                        >
                          {!usuario.tienda && <option value="">sin tienda</option>}
                          {tiendas.map(tienda => (
                            <option key={tienda.id} value={tienda.id}>
                              {tienda.nombre}
                            </option>
                          ))}
                        </select>
                      </>
                    ) : (
                      (usuario.tienda?.nombre ?? 'sin tienda')
                    )}
                  </td>
                  <td data-label="seguridad">
                    <span className="inline-flex flex-wrap justify-end gap-2">
                      {usuario.bloqueado && <span className="tag tag-agotado">bloqueada</span>}
                      <span className="tag">{usuario.mfaHabilitado ? 'mfa activo' : 'sin mfa'}</span>
                      <span className="tag">{usuario.proveedor.toLowerCase()}</span>
                    </span>
                  </td>
                  {puedeGestionar && (
                    <td data-label="acciones">
                      <span className="inline-flex flex-wrap justify-end gap-2">
                        <button
                          type="button"
                          className="pildora pildora-chica"
                          disabled={!usuario.bloqueado || ocupado === usuario.id}
                          onClick={() => void aplicar(usuario, servicios.desbloquear(usuario.id), `Desbloqueaste a ${usuario.nombreCompleto}.`)}
                        >
                          desbloquear
                        </button>
                        <button
                          type="button"
                          className="pildora pildora-chica"
                          disabled={!usuario.mfaHabilitado || ocupado === usuario.id}
                          onClick={() =>
                            void aplicar(usuario, servicios.reiniciarMfa(usuario.id),
                              `${usuario.nombreCompleto} deberá registrar su app autenticadora en el próximo inicio.`)
                          }
                        >
                          reiniciar mfa
                        </button>
                      </span>
                    </td>
                  )}
                </tr>
              )
            })}
          </tbody>
        </table>
      )}
    </section>
  )
}
