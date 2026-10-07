import type { CSSProperties, ReactNode } from 'react'

interface Props {
  filas: { campo: string; valor: ReactNode }[]
  className?: string
  giro?: number
}

/** Etiqueta laminada de cuaderno: tarjeta blanca de borde fino con campos de una línea. */
export function EtiquetaLaminada({ filas, className = '', giro = 0 }: Props) {
  return (
    <div
      className={`sticker w-[270px] rounded-campo border border-charcoal bg-white p-4 shadow-papel ${className}`}
      style={{ '--giro': `${giro}deg` } as CSSProperties}
      aria-hidden="true"
    >
      {filas.map(({ campo, valor }) => (
        <div key={campo} className="flex items-baseline gap-3 border-b border-dashed border-charcoal/40 py-1.5 last:border-b-0">
          <span className="w-16 shrink-0 font-geist text-sm text-charcoal/70">{campo}</span>
          <span className="font-gelica text-body-sm font-medium">{valor}</span>
        </div>
      ))}
    </div>
  )
}
