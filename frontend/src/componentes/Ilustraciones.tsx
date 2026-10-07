import type { CSSProperties, ReactNode } from 'react'

const TINTA = '#171717'
const PAPEL = '#fdfbf9'
const MANO =
  'M11 6.6C11 5.2 12.1 4.1 13.2 4.1C14.3 4.1 15.4 5.2 15.4 6.6V13.4C15.8 12.5 17 12.2 17.9 12.9C18.4 12.4 19.8 12.2 20.6 13.2C21.2 12.8 22.6 12.7 23.4 13.8C25.6 14.2 26 15.8 26 17.5V22C26 25.5 24 28 21 28H14.5C12.5 28 11.2 27 10.4 25.6L6.4 20.2C5.6 19 6.8 17.6 8.2 18.3L11 20.3Z'

interface PropsSticker {
  className?: string
  /** Giro en grados: los stickers se pegan torcidos, nunca alineados a una cuadrícula. */
  giro?: number
  retraso?: number
  titulo?: string
}

function Sticker({ children, className = '', giro = 0, retraso = 0, titulo }: PropsSticker & { children: ReactNode }) {
  const estilo = { '--giro': `${giro}deg`, '--retraso': `${retraso}ms` } as CSSProperties
  return (
    <svg
      viewBox="0 0 100 100"
      className={`sticker ${className}`}
      style={estilo}
      role={titulo ? 'img' : undefined}
      aria-label={titulo}
      aria-hidden={titulo ? undefined : true}
      fill="none"
      stroke={TINTA}
      strokeWidth={3}
      strokeLinejoin="round"
      strokeLinecap="round"
    >
      {children}
    </svg>
  )
}

export function Rayo(props: PropsSticker) {
  return (
    <Sticker {...props}>
      <polygon points="58,6 20,56 46,56 38,94 80,40 54,40" fill="#3b82f6" />
      <path d="M52 20 L34 48" stroke={PAPEL} strokeWidth={3} opacity={0.7} />
    </Sticker>
  )
}

export function CorazonConOjos(props: PropsSticker) {
  return (
    <Sticker {...props}>
      <path
        d="M50 88 C20 66 8 48 8 32 C8 18 19 9 31 9 C40 9 46 14 50 21 C54 14 60 9 69 9 C81 9 92 18 92 32 C92 48 80 66 50 88 Z"
        fill="#ff6f1e"
      />
      <circle cx="37" cy="36" r="9" fill={PAPEL} />
      <circle cx="63" cy="36" r="9" fill={PAPEL} />
      <circle cx="39" cy="37" r="3.8" fill={TINTA} />
      <circle cx="61" cy="37" r="3.8" fill={TINTA} />
      <path d="M42 56 Q50 64 58 56" />
    </Sticker>
  )
}

export function Fantasma(props: PropsSticker) {
  return (
    <Sticker {...props}>
      <path d="M17 90 V44 C17 23 31 10 50 10 C69 10 83 23 83 44 V90 L71 80 L60 90 L50 80 L40 90 L29 80 Z" fill={TINTA} />
      <circle cx="39" cy="44" r="6.5" fill={PAPEL} stroke="none" />
      <circle cx="61" cy="44" r="6.5" fill={PAPEL} stroke="none" />
      <path d="M43 60 Q50 67 57 60" stroke={PAPEL} />
    </Sticker>
  )
}

export function Destello(props: PropsSticker) {
  return (
    <Sticker {...props}>
      <path d="M50 6 C54 32 68 46 94 50 C68 54 54 68 50 94 C46 68 32 54 6 50 C32 46 46 32 50 6 Z" fill="#ff66cf" />
    </Sticker>
  )
}

export function CajaSonriente(props: PropsSticker) {
  return (
    <Sticker {...props}>
      <path d="M12 34 L50 18 L88 34 L88 72 L50 90 L12 72 Z" fill="#22c55e" />
      <path d="M12 34 L50 50 L88 34 M50 50 V90" />
      <path d="M31 26 L69 42" strokeWidth={2.5} />
      <circle cx="29" cy="60" r="2.6" fill={TINTA} stroke="none" />
      <circle cx="40" cy="65" r="2.6" fill={TINTA} stroke="none" />
      <path d="M28 70 Q35 78 43 74" strokeWidth={2.5} />
    </Sticker>
  )
}

export function Candado(props: PropsSticker) {
  return (
    <Sticker {...props}>
      <path d="M31 46 V33 C31 21 39 13 50 13 C61 13 69 21 69 33 V46" strokeWidth={7} stroke={TINTA} />
      <path d="M31 46 V33 C31 21 39 13 50 13 C61 13 69 21 69 33 V46" strokeWidth={2.4} stroke={PAPEL} />
      <rect x="18" y="43" width="64" height="47" rx="11" fill="#3b82f6" />
      <circle cx="50" cy="63" r="6" fill={PAPEL} />
      <path d="M50 66 V77" strokeWidth={5} stroke={PAPEL} />
    </Sticker>
  )
}

/** Flecha curva trazada a mano: nace en un pie de texto y termina con un rizo sobre el objeto que señala. */
export function FlechaCurva({ className = '', espejo = false }: { className?: string; espejo?: boolean }) {
  return (
    <svg
      viewBox="0 0 90 110"
      className={className}
      style={espejo ? { transform: 'scaleX(-1)' } : undefined}
      aria-hidden="true"
      fill="none"
      stroke={TINTA}
      strokeWidth={1.6}
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M8 6 C 30 2, 62 20, 58 52 C 55 78, 28 82, 33 98 C 36 106, 48 104, 52 96" />
      <path d="M40 92 L52 97 L55 84" />
    </svg>
  )
}

/** Subrayado de marcador con pulso irregular, el mismo naranja del pie de página. */
export function Marcador({ children }: { children: ReactNode }) {
  return (
    <span className="relative inline-block whitespace-nowrap">
      <span className="relative z-10">{children}</span>
      <svg
        className="absolute left-0 right-0 -bottom-[7px] h-[10px] w-full"
        viewBox="0 0 120 12"
        preserveAspectRatio="none"
        aria-hidden="true"
        fill="none"
      >
        <path d="M2 7 C 18 2, 34 11, 52 6 S 88 3, 118 7" stroke="#ff6f1e" strokeWidth={3.6} strokeLinecap="round" />
      </svg>
    </span>
  )
}

/** Marca de TechStore: la mano que señala. El ícono es la marca, sin nombre al lado. */
export function MarcaMano({ className = '' }: { className?: string }) {
  return (
    <svg viewBox="0 0 32 32" className={className} role="img" aria-label="TechStore" fill="none">
      <path
        d={MANO}
        fill="#fdfbf9"
        stroke={TINTA}
        strokeWidth={1.5}
        strokeLinejoin="round"
      />
      <path d="M17.9 13V17.6M20.6 13.2V17.8M23.4 13.8V18" stroke={TINTA} strokeWidth={1.4} strokeLinecap="round" />
    </svg>
  )
}

export function Visto({ className = '' }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" className={className} aria-hidden="true" fill="none" stroke={TINTA} strokeWidth={2.6}
      strokeLinecap="round" strokeLinejoin="round">
      <path d="M4 12.5 L9.5 18 L20 6" />
    </svg>
  )
}

export function Pendiente({ className = '' }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" className={className} aria-hidden="true" fill="none" stroke={TINTA} strokeWidth={1.8}>
      <circle cx="12" cy="12" r="7" strokeDasharray="3 3.2" />
    </svg>
  )
}

export function Exclamacion({ className = '' }: { className?: string }) {
  return (
    <svg viewBox="0 0 28 28" className={className} aria-hidden="true">
      <circle cx="14" cy="14" r="12" fill="#ff6f1e" stroke={TINTA} strokeWidth={2} />
      <path d="M14 7.5 V15.5" stroke={TINTA} strokeWidth={2.8} strokeLinecap="round" />
      <circle cx="14" cy="20" r="1.8" fill={TINTA} />
    </svg>
  )
}
