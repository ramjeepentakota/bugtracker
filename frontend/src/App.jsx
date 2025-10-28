import { Routes, Route, Navigate } from 'react-router-dom'
import { useState, useEffect } from 'react'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Clients from './pages/Clients'
import Applications from './pages/Applications'
import TestPlans from './pages/TestPlans'
import VaptReports from './pages/VaptReports'
import Defects from './pages/Defects'
import Sidebar from './components/Sidebar'
import Header from './components/Header'
import { AuthProvider, useAuth } from './hooks/useAuth'

function AppContent() {
  const { user, loading } = useAuth()
  const [sidebarOpen, setSidebarOpen] = useState(false)

  if (loading) {
    return (
      <div className="min-h-screen bg-dark-900 flex items-center justify-center">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-primary-500"></div>
      </div>
    )
  }

  if (!user) {
    return <Login />
  }

  // Check if user has access to current route
  const currentPath = window.location.pathname
  const restrictedPaths = ['/clients', '/applications', '/test-plans', '/vapt-reports', '/defects']
  const isRestrictedPath = restrictedPaths.includes(currentPath)

  if (isRestrictedPath && user.role === 'CLIENT') {
    // Redirect clients to dashboard if they try to access restricted pages
    window.location.href = '/dashboard'
    return null
  }

  return (
    <div className="min-h-screen bg-dark-900 flex">
      <Sidebar isOpen={sidebarOpen} setIsOpen={setSidebarOpen} />
      <div className="flex-1 flex flex-col">
        <Header onMenuClick={() => setSidebarOpen(true)} />
        <main className="flex-1 p-6">
          <Routes>
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/clients" element={<Clients />} />
            <Route path="/applications" element={<Applications />} />
            <Route path="/test-plans" element={<TestPlans />} />
            <Route path="/vapt-reports" element={<VaptReports />} />
            <Route path="/defects" element={<Defects />} />
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </main>
      </div>
    </div>
  )
}

function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  )
}

export default App