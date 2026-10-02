import { Navigate, Route, Routes } from 'react-router-dom';
import AppLayout from './layouts/AppLayout';
import ProtectedRoute from './components/ProtectedRoute';
import { ToastHost } from './components/Toast';

import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import Cases from './pages/Cases';
import CaseNew from './pages/CaseNew';
import CaseDetails from './pages/CaseDetails';
import CaseEdit from './pages/CaseEdit';
import Evidence from './pages/Evidence';
import EvidenceDetails from './pages/EvidenceDetails';
import ChainOfCustody from './pages/ChainOfCustody';
import AuditLogs from './pages/AuditLogs';
import Users from './pages/Users';
import Notifications from './pages/Notifications';
import Reports from './pages/Reports';
import Profile from './pages/Profile';
import Settings from './pages/Settings';
import NotFound from './pages/NotFound';

import { CAN_CREATE_CASE, CAN_EDIT_CASE, CAN_MANAGE_USERS, CAN_VIEW_AUDIT_LOGS } from './utils/constants';

export default function App() {
  return (
    <>
      <ToastHost />
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />

        <Route
          element={
            <ProtectedRoute>
              <AppLayout />
            </ProtectedRoute>
          }
        >
          <Route path="/dashboard" element={<Dashboard />} />

          <Route path="/cases" element={<Cases />} />
          <Route
            path="/cases/new"
            element={<ProtectedRoute roles={CAN_CREATE_CASE}><CaseNew /></ProtectedRoute>}
          />
          <Route path="/cases/:id" element={<CaseDetails />} />
          <Route
            path="/cases/:id/edit"
            element={<ProtectedRoute roles={CAN_EDIT_CASE}><CaseEdit /></ProtectedRoute>}
          />

          <Route path="/evidence" element={<Evidence />} />
          <Route path="/evidence/:id" element={<EvidenceDetails />} />

          <Route path="/chain-of-custody" element={<ChainOfCustody />} />

          <Route path="/reports" element={<Reports />} />

          <Route path="/notifications" element={<Notifications />} />

          <Route
            path="/audit-logs"
            element={<ProtectedRoute roles={CAN_VIEW_AUDIT_LOGS}><AuditLogs /></ProtectedRoute>}
          />

          <Route
            path="/users"
            element={<ProtectedRoute roles={CAN_MANAGE_USERS}><Users /></ProtectedRoute>}
          />

          <Route path="/profile" element={<Profile />} />
          <Route path="/settings" element={<Settings />} />
        </Route>

        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="*" element={<NotFound />} />
      </Routes>
    </>
  );
}
