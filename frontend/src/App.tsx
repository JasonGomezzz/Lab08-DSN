import { Navigate, Route, Routes } from 'react-router'
import { Capa } from './componentes/Capa'
import { NoEncontrada, RutaConPermiso, RutaProtegida, RutaPublica } from './componentes/Rutas'
import Inventario from './paginas/Inventario'
import Login from './paginas/Login'
import Mfa from './paginas/Mfa'
import Registro from './paginas/Registro'
import Reportes from './paginas/Reportes'
import Tienda from './paginas/Tienda'
import Usuarios from './paginas/Usuarios'

export default function App() {
  return (
    <Routes>
      <Route element={<Capa />}>
        <Route element={<RutaPublica />}>
          <Route path="/login" element={<Login />} />
          <Route path="/registro" element={<Registro />} />
        </Route>
        {/* La verificación MFA se alcanza con un token parcial, aún sin sesión completa. */}
        <Route path="/mfa" element={<Mfa />} />

        <Route element={<RutaProtegida />}>
          <Route index element={<Navigate to="/inventario" replace />} />
          <Route path="/tienda" element={<Tienda />} />
          <Route element={<RutaConPermiso accion="VER_PRODUCTOS" />}>
            <Route path="/inventario" element={<Inventario />} />
          </Route>
          <Route element={<RutaConPermiso accion="VER_REPORTE" />}>
            <Route path="/reportes" element={<Reportes />} />
          </Route>
          <Route element={<RutaConPermiso accion="VER_USUARIOS" />}>
            <Route path="/usuarios" element={<Usuarios />} />
          </Route>
        </Route>

        <Route path="*" element={<NoEncontrada />} />
      </Route>
    </Routes>
  )
}
