import type { ReactNode } from 'react'
import { Exclamacion, Visto } from './Ilustraciones'

export function AvisoError({ children }: { children: ReactNode }) {
  return (
    <div role="alert" className="aviso">
      <Exclamacion className="mt-0.5 h-6 w-6 shrink-0" />
      <p>{children}</p>
    </div>
  )
}

export function AvisoInfo({ children }: { children: ReactNode }) {
  return (
    <div role="status" className="aviso">
      <Visto className="mt-0.5 h-6 w-6 shrink-0" />
      <p>{children}</p>
    </div>
  )
}
