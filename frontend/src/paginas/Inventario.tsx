import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { erroresPorCampo, errorTexto, servicios } from '../api'
import { AvisoError, AvisoInfo } from '../componentes/Avisos'
import { Campo, atributosCampo } from '../componentes/Campo'
import { Encabezado } from '../componentes/Encabezado'
import { CajaSonriente } from '../componentes/Ilustraciones'
import { useSesion } from '../sesion'
import type { DatosProducto, Producto, Tienda } from '../tipos'

const moneda = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' })
/** Mismo umbral que usa el reporte del servidor. */
const UMBRAL_POCO_STOCK = 5
const PATRON_PRECIO = '^\\d{1,8}(\\.\\d{1,2})?$'

type Modo = 'ajustar' | 'editar' | 'eliminar'

export default function Inventario() {
  const { perfil, puede } = useSesion()
  const global = perfil?.rol === 'ADMINISTRADOR' || perfil?.rol === 'AUDITOR'
  const [productos, setProductos] = useState<Producto[] | null>(null)
  const [tiendas, setTiendas] = useState<Tienda[]>([])
  const [filtro, setFiltro] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const [nuevo, setNuevo] = useState(false)
  const [abierta, setAbierta] = useState<{ id: number; modo: Modo } | null>(null)
  const [ocupado, setOcupado] = useState<number | null>(null)

  const puedeCrear = puede('CREAR_PRODUCTO')
  const puedeEditar = puede('EDITAR_PRODUCTO')
  const puedeStock = puede('ACTUALIZAR_STOCK')
  const puedeEliminar = puede('ELIMINAR_PRODUCTO')
  const hayAcciones = puedeEditar || puedeStock || puedeEliminar

  const cargar = useCallback(async () => {
    try {
      setProductos(await servicios.productos(filtro ?? undefined))
      setError(null)
    } catch (fallo) {
      setError(errorTexto(fallo))
      setProductos([])
    }
  }, [filtro])

  useEffect(() => {
    void cargar()
  }, [cargar])

  useEffect(() => {
    if (global) servicios.tiendas().then(setTiendas).catch(() => undefined)
  }, [global])

  function reemplazar(actualizado: Producto) {
    setProductos(lista => lista?.map(producto => (producto.id === actualizado.id ? actualizado : producto)) ?? null)
  }

  async function ajustarStock(producto: Producto, ajuste: number) {
    setOcupado(producto.id)
    setError(null)
    try {
      const actualizado = await servicios.ajustarStock(producto.id, ajuste)
      reemplazar(actualizado)
      setAviso(`${actualizado.nombre}: el stock quedó en ${actualizado.stock}.`)
      setAbierta(null)
    } catch (fallo) {
      setError(errorTexto(fallo))
    } finally {
      setOcupado(null)
    }
  }

  async function eliminar(producto: Producto) {
    setOcupado(producto.id)
    setError(null)
    try {
      await servicios.eliminarProducto(producto.id)
      setProductos(lista => lista?.filter(item => item.id !== producto.id) ?? null)
      setAviso(`Eliminaste «${producto.nombre}».`)
      setAbierta(null)
    } catch (fallo) {
      setError(errorTexto(fallo))
    } finally {
      setOcupado(null)
    }
  }

  const nota = global
    ? perfil?.rol === 'AUDITOR'
      ? 'todas las tiendas, solo lectura'
      : 'todas las tiendas'
    : perfil?.tienda?.nombre.toLowerCase()

  return (
    <section>
      <Encabezado
        titulo="inventario"
        nota={nota}
        acciones={
          <>
            {global && tiendas.length > 0 && (
              <div className="flex items-center gap-2">
                <label htmlFor="filtro-tienda" className="font-geist text-caption">
                  ver
                </label>
                <select
                  id="filtro-tienda"
                  className="campo !w-auto"
                  value={filtro ?? ''}
                  onChange={e => setFiltro(e.target.value ? Number(e.target.value) : null)}
                >
                  <option value="">todas las tiendas</option>
                  {tiendas.map(tienda => (
                    <option key={tienda.id} value={tienda.id}>
                      {tienda.nombre}
                    </option>
                  ))}
                </select>
              </div>
            )}
            {puedeCrear && (
              <button type="button" className="pildora" aria-expanded={nuevo} onClick={() => setNuevo(abierto => !abierto)}>
                {nuevo ? 'cerrar' : 'nuevo producto'}
              </button>
            )}
          </>
        }
      />

      <div className="mb-6 space-y-3" aria-live="polite">
        {error && <AvisoError>{error}</AvisoError>}
        {aviso && !error && <AvisoInfo>{aviso}</AvisoInfo>}
      </div>

      {nuevo && puedeCrear && (
        <FormularioNuevo
          tiendas={tiendas}
          esAdministrador={perfil?.rol === 'ADMINISTRADOR'}
          alCrear={async datos => {
            const creado = await servicios.crearProducto(datos)
            setAviso(`Agregaste «${creado.nombre}» a ${creado.tienda.nombre}.`)
            setNuevo(false)
            await cargar()
          }}
          alCancelar={() => setNuevo(false)}
        />
      )}

      {productos === null ? (
        <div role="status" className="space-y-3">
          <span className="sr-only">cargando el inventario</span>
          {[0, 1, 2, 3].map(fila => (
            <div key={fila} className="h-12 animate-pulse rounded-campo border border-dashed border-charcoal/40 bg-dew-drop" />
          ))}
        </div>
      ) : productos.length === 0 ? (
        <div className="flex max-w-[560px] flex-col items-start gap-5 py-6">
          <CajaSonriente className="h-28 w-28" giro={-7} />
          <p className="font-gelica text-heading-sm leading-snug">
            {puedeCrear ? 'todavía no hay productos aquí. agrega el primero.' : 'no hay productos para mostrar.'}
          </p>
          {puedeCrear && !nuevo && (
            <button type="button" className="pildora" onClick={() => setNuevo(true)}>
              nuevo producto
            </button>
          )}
        </div>
      ) : (
        <table className="libro">
          <caption className="sr-only">Productos del inventario con su precio y stock</caption>
          <thead>
            <tr>
              <th scope="col">sku</th>
              <th scope="col">producto</th>
              {global && <th scope="col">tienda</th>}
              <th scope="col" className="num">
                precio
              </th>
              <th scope="col" className="num">
                stock
              </th>
              {hayAcciones && <th scope="col">acciones</th>}
            </tr>
          </thead>
          <tbody>
            {productos.map(producto => {
              const detalle = abierta?.id === producto.id ? abierta.modo : null
              return (
                <FilaProducto
                  key={producto.id}
                  producto={producto}
                  global={global}
                  hayAcciones={hayAcciones}
                  ocupado={ocupado === producto.id}
                  detalle={detalle}
                  puedeStock={puedeStock}
                  puedeEditar={puedeEditar}
                  puedeEliminar={puedeEliminar}
                  alAjustarRapido={ajuste => void ajustarStock(producto, ajuste)}
                  alAbrir={modo => setAbierta(detalle === modo ? null : { id: producto.id, modo })}
                  alAjustar={ajuste => ajustarStock(producto, ajuste)}
                  alEditar={async datos => {
                    reemplazar(await servicios.editarProducto(producto.id, datos))
                    setAviso(`Guardaste los cambios de «${datos.nombre}».`)
                    setAbierta(null)
                  }}
                  alEliminar={() => eliminar(producto)}
                  alCerrar={() => setAbierta(null)}
                />
              )
            })}
          </tbody>
        </table>
      )}
    </section>
  )
}

interface PropsFila {
  producto: Producto
  global: boolean
  hayAcciones: boolean
  ocupado: boolean
  detalle: Modo | null
  puedeStock: boolean
  puedeEditar: boolean
  puedeEliminar: boolean
  alAjustarRapido: (ajuste: number) => void
  alAbrir: (modo: Modo) => void
  alAjustar: (ajuste: number) => Promise<void>
  alEditar: (datos: { nombre: string; categoria: string; precio: string }) => Promise<void>
  alEliminar: () => Promise<void>
  alCerrar: () => void
}

function FilaProducto(props: PropsFila) {
  const { producto, global, hayAcciones, ocupado, detalle } = props
  const columnas = 4 + (global ? 1 : 0) + (hayAcciones ? 1 : 0)
  return (
    <>
      <tr>
        <td data-label="sku" className="whitespace-nowrap font-medium">
          {producto.sku}
        </td>
        <td data-label="producto">
          <div>
            <span className="block font-medium">{producto.nombre}</span>
            <span className="block text-sm text-charcoal/75">{producto.categoria}</span>
            {producto.stock === 0 && <span className="tag tag-agotado mt-1">agotado</span>}
            {producto.stock > 0 && producto.stock < UMBRAL_POCO_STOCK && <span className="tag tag-poco mt-1">poco stock</span>}
          </div>
        </td>
        {global && <td data-label="tienda">{producto.tienda.nombre}</td>}
        <td data-label="precio" className="num">
          {moneda.format(producto.precio)}
        </td>
        <td data-label="stock" className="num">
          <span className="inline-flex items-center justify-end gap-2 whitespace-nowrap">
            {props.puedeStock && (
              <button
                type="button"
                className="pildora pildora-chica !px-3"
                aria-label={`Vender una unidad de ${producto.nombre}`}
                disabled={ocupado || producto.stock === 0}
                onClick={() => props.alAjustarRapido(-1)}
              >
                −
              </button>
            )}
            <span className="min-w-[2.5ch] text-center font-medium tabular-nums">{producto.stock}</span>
            {props.puedeStock && (
              <button
                type="button"
                className="pildora pildora-chica !px-3"
                aria-label={`Agregar una unidad de ${producto.nombre}`}
                disabled={ocupado}
                onClick={() => props.alAjustarRapido(1)}
              >
                +
              </button>
            )}
          </span>
        </td>
        {hayAcciones && (
          <td data-label="acciones">
            <span className="inline-flex justify-end gap-2 whitespace-nowrap">
              {props.puedeStock && (
                <button type="button" className="pildora pildora-chica" aria-expanded={detalle === 'ajustar'} onClick={() => props.alAbrir('ajustar')}>
                  ajustar
                </button>
              )}
              {props.puedeEditar && (
                <button type="button" className="pildora pildora-chica" aria-expanded={detalle === 'editar'} onClick={() => props.alAbrir('editar')}>
                  editar
                </button>
              )}
              {props.puedeEliminar && (
                <button type="button" className="pildora pildora-chica" aria-expanded={detalle === 'eliminar'} onClick={() => props.alAbrir('eliminar')}>
                  eliminar
                </button>
              )}
            </span>
          </td>
        )}
      </tr>
      {detalle && (
        <tr className="detalle">
          <td colSpan={columnas} className="sin-rotulo">
            {detalle === 'ajustar' && <DetalleAjuste producto={producto} ocupado={ocupado} alAjustar={props.alAjustar} alCerrar={props.alCerrar} />}
            {detalle === 'editar' && <DetalleEdicion producto={producto} alEditar={props.alEditar} alCerrar={props.alCerrar} />}
            {detalle === 'eliminar' && (
              <div className="flex flex-wrap items-center justify-between gap-4 py-1">
                <p className="font-gelica text-body-sm">
                  ¿Eliminar «{producto.nombre}» de {producto.tienda.nombre}? Esto no se puede deshacer.
                </p>
                <div className="flex gap-3">
                  <button type="button" className="pildora pildora-chica" disabled={ocupado} aria-busy={ocupado} onClick={() => void props.alEliminar()}>
                    {ocupado ? 'eliminando…' : 'sí, eliminar'}
                  </button>
                  <button type="button" className="pildora pildora-chica" onClick={props.alCerrar}>
                    cancelar
                  </button>
                </div>
              </div>
            )}
          </td>
        </tr>
      )}
    </>
  )
}

function DetalleAjuste({ producto, ocupado, alAjustar, alCerrar }: {
  producto: Producto
  ocupado: boolean
  alAjustar: (ajuste: number) => Promise<void>
  alCerrar: () => void
}) {
  const [texto, setTexto] = useState('-1')
  const ajuste = Number(texto)
  const valido = texto !== '' && Number.isInteger(ajuste) && ajuste !== 0 && Math.abs(ajuste) <= 100_000
  const resultado = producto.stock + ajuste
  const negativo = valido && resultado < 0

  function enviar(evento: FormEvent) {
    evento.preventDefault()
    if (valido && !negativo) void alAjustar(ajuste)
  }

  return (
    <form onSubmit={enviar} className="flex flex-wrap items-end gap-x-6 gap-y-3">
      <Campo
        id={`ajuste-${producto.id}`}
        etiqueta="unidades a sumar o restar"
        error={negativo ? `Solo hay ${producto.stock} unidades; no puedes restar tantas.` : undefined}
        ayuda={valido ? `Hoy hay ${producto.stock}; quedarían ${resultado}. Usa un número negativo para una venta.` : 'Escribe un número entero distinto de cero.'}
      >
        <input
          {...atributosCampo(`ajuste-${producto.id}`, negativo ? 'x' : undefined)}
          className="campo !w-40"
          type="number"
          step={1}
          inputMode="numeric"
          value={texto}
          onChange={e => setTexto(e.target.value)}
          autoFocus
        />
      </Campo>
      <div className="flex gap-3 pb-1">
        <button type="submit" className="pildora pildora-chica" disabled={!valido || negativo || ocupado} aria-busy={ocupado}>
          {ocupado ? 'aplicando…' : 'aplicar'}
        </button>
        <button type="button" className="pildora pildora-chica" onClick={alCerrar}>
          cancelar
        </button>
      </div>
    </form>
  )
}

function DetalleEdicion({ producto, alEditar, alCerrar }: {
  producto: Producto
  alEditar: (datos: { nombre: string; categoria: string; precio: string }) => Promise<void>
  alCerrar: () => void
}) {
  const [nombre, setNombre] = useState(producto.nombre)
  const [categoria, setCategoria] = useState(producto.categoria)
  const [precio, setPrecio] = useState(producto.precio.toFixed(2))
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [campos, setCampos] = useState<Record<string, string[]>>({})

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setEnviando(true)
    setError(null)
    setCampos({})
    try {
      await alEditar({ nombre: nombre.trim(), categoria: categoria.trim(), precio })
    } catch (fallo) {
      const porCampo = erroresPorCampo(fallo)
      setCampos(porCampo)
      if (Object.keys(porCampo).length === 0) setError(errorTexto(fallo))
      setEnviando(false)
    }
  }

  return (
    <form onSubmit={enviar} className="space-y-4">
      {error && <AvisoError>{error}</AvisoError>}
      <div className="grid gap-4 sm:grid-cols-[2fr_1fr_1fr]">
        <Campo id={`nombre-${producto.id}`} etiqueta="nombre" error={campos.nombre?.[0]}>
          <input {...atributosCampo(`nombre-${producto.id}`, campos.nombre?.[0])} className="campo" required value={nombre} onChange={e => setNombre(e.target.value)} autoFocus />
        </Campo>
        <Campo id={`categoria-${producto.id}`} etiqueta="categoría" error={campos.categoria?.[0]}>
          <input {...atributosCampo(`categoria-${producto.id}`, campos.categoria?.[0])} className="campo" required value={categoria} onChange={e => setCategoria(e.target.value)} />
        </Campo>
        <Campo id={`precio-${producto.id}`} etiqueta="precio (S/)" error={campos.precio?.[0]}>
          <input {...atributosCampo(`precio-${producto.id}`, campos.precio?.[0])} className="campo" required inputMode="decimal" pattern={PATRON_PRECIO} value={precio} onChange={e => setPrecio(e.target.value)} />
        </Campo>
      </div>
      <div className="flex gap-3">
        <button type="submit" className="pildora pildora-chica" disabled={enviando} aria-busy={enviando}>
          {enviando ? 'guardando…' : 'guardar cambios'}
        </button>
        <button type="button" className="pildora pildora-chica" onClick={alCerrar}>
          cancelar
        </button>
      </div>
    </form>
  )
}

function FormularioNuevo({ tiendas, esAdministrador, alCrear, alCancelar }: {
  tiendas: Tienda[]
  esAdministrador: boolean
  alCrear: (datos: DatosProducto) => Promise<void>
  alCancelar: () => void
}) {
  const [sku, setSku] = useState('')
  const [nombre, setNombre] = useState('')
  const [categoria, setCategoria] = useState('')
  const [precio, setPrecio] = useState('')
  const [stock, setStock] = useState('0')
  const [tiendaId, setTiendaId] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [campos, setCampos] = useState<Record<string, string[]>>({})

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setEnviando(true)
    setError(null)
    setCampos({})
    try {
      await alCrear({
        sku: sku.trim(),
        nombre: nombre.trim(),
        categoria: categoria.trim(),
        precio,
        stock: Number(stock),
        tiendaId: esAdministrador ? Number(tiendaId) : undefined,
      })
    } catch (fallo) {
      const porCampo = erroresPorCampo(fallo)
      setCampos(porCampo)
      if (Object.keys(porCampo).length === 0) setError(errorTexto(fallo))
      setEnviando(false)
    }
  }

  return (
    <form onSubmit={enviar} className="panel mb-8 space-y-4" aria-label="Nuevo producto">
      <h2 className="font-gelica text-heading-sm">nuevo producto</h2>
      {error && <AvisoError>{error}</AvisoError>}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <Campo id="nuevo-sku" etiqueta="sku" error={campos.sku?.[0]} ayuda="Letras, números, punto, guion y guion bajo.">
          <input {...atributosCampo('nuevo-sku', campos.sku?.[0])} className="campo" required maxLength={40} value={sku} onChange={e => setSku(e.target.value)} autoFocus />
        </Campo>
        <Campo id="nuevo-nombre" etiqueta="nombre" error={campos.nombre?.[0]}>
          <input {...atributosCampo('nuevo-nombre', campos.nombre?.[0])} className="campo" required maxLength={120} value={nombre} onChange={e => setNombre(e.target.value)} />
        </Campo>
        <Campo id="nuevo-categoria" etiqueta="categoría" error={campos.categoria?.[0]}>
          <input {...atributosCampo('nuevo-categoria', campos.categoria?.[0])} className="campo" required maxLength={60} value={categoria} onChange={e => setCategoria(e.target.value)} />
        </Campo>
        <Campo id="nuevo-precio" etiqueta="precio (S/)" error={campos.precio?.[0]}>
          <input {...atributosCampo('nuevo-precio', campos.precio?.[0])} className="campo" required inputMode="decimal" pattern={PATRON_PRECIO} value={precio} onChange={e => setPrecio(e.target.value)} placeholder="0.00" />
        </Campo>
        <Campo id="nuevo-stock" etiqueta="stock inicial" error={campos.stock?.[0]}>
          <input {...atributosCampo('nuevo-stock', campos.stock?.[0])} className="campo" required type="number" min={0} step={1} value={stock} onChange={e => setStock(e.target.value)} />
        </Campo>
        {esAdministrador && (
          <Campo id="nuevo-tienda" etiqueta="tienda" error={campos.tiendaId?.[0]}>
            <select {...atributosCampo('nuevo-tienda', campos.tiendaId?.[0])} className="campo" required value={tiendaId} onChange={e => setTiendaId(e.target.value)}>
              <option value="">elige una tienda</option>
              {tiendas.map(tienda => (
                <option key={tienda.id} value={tienda.id}>
                  {tienda.nombre}
                </option>
              ))}
            </select>
          </Campo>
        )}
      </div>
      <div className="flex gap-3">
        <button type="submit" className="pildora" disabled={enviando} aria-busy={enviando}>
          {enviando ? 'guardando…' : 'guardar producto'}
        </button>
        <button type="button" className="pildora" onClick={alCancelar}>
          cancelar
        </button>
      </div>
    </form>
  )
}
