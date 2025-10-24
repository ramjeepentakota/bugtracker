import { useAuth } from '../hooks/useAuth'
import { LogOut, User } from 'lucide-react'

const Header = ({ onMenuClick }) => {
  const { user, logout } = useAuth()

  return (
    <header className="bg-dark-800 border-b border-dark-700 px-4 py-3">
      <div className="flex items-center justify-between">
        <div className="flex items-center">
          <button
            onClick={onMenuClick}
            className="text-gray-400 hover:text-white lg:hidden"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          </button>
          <h1 className="ml-4 text-xl font-semibold text-white lg:ml-0">
            Defect Tracker
          </h1>
        </div>

        <div className="flex items-center space-x-4">
          <div className="flex items-center space-x-2">
            <User className="w-4 h-4 text-gray-400" />
            <span className="text-sm text-gray-300">
              {user?.firstName || user?.username}
            </span>
            <span className="text-xs bg-primary-500/20 text-primary-400 px-2 py-1 rounded">
              {user?.role}
            </span>
          </div>

          <button
            onClick={logout}
            className="flex items-center space-x-2 text-gray-400 hover:text-white transition-colors"
          >
            <LogOut className="w-4 h-4" />
            <span className="hidden sm:inline">Logout</span>
          </button>
        </div>
      </div>
    </header>
  )
}

export default Header