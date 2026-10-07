import type { ReactNode } from 'react'

/** Anotación a mano en naranja marcador: se usa para rotular, nunca como botón ni como estado. */
export function Nota({ children, className = '', giro = -2 }: { children: ReactNode; className?: string; giro?: number }) {
  return (
    <p
      className={`font-gelica text-subheading font-normal text-burnt-sienna ${className}`}
      style={{ transform: `rotate(${giro}deg)`, transformOrigin: 'left center' }}
    >
      {children}
    </p>
  )
}

interface Props {
  titulo: string
  nota?: ReactNode
  acciones?: ReactNode
}

export function Encabezado({ titulo, nota, acciones }: Props) {
  return (
    <div className="mb-8 flex flex-wrap items-end justify-between gap-x-8 gap-y-4">
      <div>
        <h1 className="font-gelica text-heading-lg leading-[1.1] sm:text-[3.25rem]">{titulo}</h1>
        {nota && <Nota className="mt-2">{nota}</Nota>}
      </div>
      {acciones && <div className="flex flex-wrap items-center gap-3">{acciones}</div>}
    </div>
  )
}
