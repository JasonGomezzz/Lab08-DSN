import { useEffect, useState } from 'react'
import { errorTexto, servicios } from '../api'
import { AvisoError } from '../componentes/Avisos'
import { Encabezado } from '../componentes/Encabezado'
import { Marcador } from '../componentes/Ilustraciones'
import { useSesion } from '../sesion'
import type { ReporteInventario } from '../tipos'

const moneda = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' })
const numero = new Intl.NumberFormat('es-PE')
const fechaHora = new Intl.DateTimeFormat('es-PE', { dateStyle: 'long', timeStyle: 'short' })

/** Barra de valor relativa a la tienda con más inventario: sirve para comparar de un vistazo. */
function Barra({ nombre, valor, maximo }: { nombre: string; valor: number; maximo: number }) {
  const proporcion = maximo > 0 ? (valor / maximo) * 100 : 0
  return (
    <div
      role="img"
      aria-label={`El valor de ${nombre} es el ${Math.round(proporcion)} % del de la tienda con más valor`}
      className="h-3 w-full min-w-28 rounded-etiqueta border border-charcoal"
    >
      <div className="h-full rounded-etiqueta bg-charcoal" style={{ width: `${Math.max(proporcion, valor > 0 ? 4 : 0)}%` }} />
    </div>
  )
}

export default function Reportes() {
  const { perfil } = useSesion()
  const [reporte, setReporte] = useState<ReporteInventario | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    servicios.reporte().then(setReporte).catch(fallo => setError(errorTexto(fallo)))
  }, [])

  const global = perfil?.rol === 'ADMINISTRADOR' || perfil?.rol === 'AUDITOR'
  const maximo = reporte ? Math.max(0, ...reporte.tiendas.map(tienda => tienda.valorInventario)) : 0

  return (
    <section>
      <Encabezado titulo="reportes" nota={global ? 'todas las tiendas' : perfil?.tienda?.nombre.toLowerCase()} />
      {error && <AvisoError>{error}</AvisoError>}
      {!reporte && !error && (
        <div role="status" className="space-y-3">
          <span className="sr-only">generando el reporte</span>
          {[0, 1, 2].map(fila => (
            <div key={fila} className="h-14 animate-pulse rounded-campo border border-dashed border-charcoal/40 bg-dew-drop" />
          ))}
        </div>
      )}
      {reporte && (
        <>
          <table className="libro">
            <caption className="sr-only">Resumen del inventario por tienda</caption>
            <thead>
              <tr>
                <th scope="col">tienda</th>
                <th scope="col" className="num">
                  productos
                </th>
                <th scope="col" className="num">
                  unidades
                </th>
                <th scope="col" className="num">
                  con poco stock
                </th>
                <th scope="col" className="num">
                  valor del inventario
                </th>
                {reporte.tiendas.length > 1 && <th scope="col">comparación</th>}
              </tr>
            </thead>
            <tbody>
              {reporte.tiendas.map(tienda => (
                <tr key={tienda.codigo}>
                  <td data-label="tienda" className="font-medium">
                    {tienda.nombre}
                  </td>
                  <td data-label="productos" className="num">
                    {numero.format(tienda.productos)}
                  </td>
                  <td data-label="unidades" className="num">
                    {numero.format(tienda.unidades)}
                  </td>
                  <td data-label="con poco stock" className="num">
                    {numero.format(tienda.productosConPocoStock)}
                  </td>
                  <td data-label="valor del inventario" className="num whitespace-nowrap">
                    {moneda.format(tienda.valorInventario)}
                  </td>
                  {reporte.tiendas.length > 1 && (
                    <td data-label="comparación" className="min-w-40">
                      <Barra nombre={tienda.nombre} valor={tienda.valorInventario} maximo={maximo} />
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
            {reporte.tiendas.length > 1 && (
              <tfoot>
                <tr>
                  <td data-label="tienda">
                    <Marcador>total</Marcador>
                  </td>
                  <td data-label="productos" className="num">
                    {numero.format(reporte.total.productos)}
                  </td>
                  <td data-label="unidades" className="num">
                    {numero.format(reporte.total.unidades)}
                  </td>
                  <td data-label="con poco stock" className="num">
                    {numero.format(reporte.total.productosConPocoStock)}
                  </td>
                  <td data-label="valor del inventario" className="num whitespace-nowrap">
                    {moneda.format(reporte.total.valorInventario)}
                  </td>
                  <td className="sin-rotulo" />
                </tr>
              </tfoot>
            )}
          </table>
          <p className="mt-8 max-w-[70ch] font-geist text-caption text-charcoal/75">
            «Con poco stock» cuenta los productos con menos de {reporte.umbralStockBajo} unidades. Generado el{' '}
            {fechaHora.format(new Date(reporte.generadoEn))}; los importes están en soles (S/).
          </p>
        </>
      )}
    </section>
  )
}
