import { Navigate, Route, Routes } from "react-router";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import AsciiBackground from "./components/AsciiBackground";
import AuthProvider from "./auth/AuthProvider";
import { AccountPage, SessionGate } from "./auth/SessionRoutes";

function App() {
  return (
    <AuthProvider>
      <AsciiBackground />
      <Routes>
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="/login" element={<SessionGate><LoginPage /></SessionGate>} />
        <Route path="/register" element={<SessionGate><RegisterPage /></SessionGate>} />
        <Route path="/account" element={<SessionGate protectedRoute><AccountPage /></SessionGate>} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </AuthProvider>
  );
}

export default App;
