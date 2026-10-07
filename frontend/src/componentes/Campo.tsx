import type { ReactNode } from 'react'

interface Props {
  id: string
  etiqueta: string
  error?: string
  ayuda?: ReactNode
  children: ReactNode
}

/** Atributos que enlazan un control con su mensaje de error para lectores de pantalla. */
export function atributosCampo(id: string, error?: string) {
  return {
    id,
    'aria-invalid': error ? true : undefined,
    'aria-describedby': error ? `${id}-error` : undefined,
  } as const
}

export function Campo({ id, etiqueta, error, ayuda, children }: Props) {
  return (
    <div>
      <label htmlFor={id} className="rotulo">
        {etiqueta}
      </label>
      {children}
      {ayuda && !error && <p className="ayuda">{ayuda}</p>}
      {error && (
        <p id={`${id}-error`} className="error-campo">
          {error}
        </p>
      )}
    </div>
  )
}
