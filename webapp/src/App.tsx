import { Navigate, Route, Routes } from 'react-router-dom';
import { AppShell } from './components/AppShell';
import { AdminPage } from './pages/AdminPage';
import { BoardPage } from './pages/BoardPage';
import { LoginPage } from './pages/LoginPage';
import { NotePage } from './pages/NotePage';

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<AppShell />}>
        <Route path="/note" element={<NotePage />} />
        <Route path="/board" element={<BoardPage />} />
        <Route path="/admin" element={<AdminPage />} />
      </Route>
      <Route path="/" element={<Navigate to="/note" replace />} />
      <Route path="*" element={<Navigate to="/note" replace />} />
    </Routes>
  );
}

export default App;
