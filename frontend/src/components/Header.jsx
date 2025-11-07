import { useAuth } from '../hooks/useAuth'
import { LogOut, User, Shield, Bell, Settings } from 'lucide-react'
import { motion } from 'framer-motion'

const Header = ({ onMenuClick }) => {
  const { user, logout } = useAuth()

  return (
    <motion.header
      initial={{ y: -100, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      className="relative overflow-hidden bg-gradient-to-r from-dark-900 via-dark-800 to-dark-900 border-b border-dark-700/50 backdrop-blur-xl"
    >
      {/* Animated background */}
      <div className="absolute inset-0 bg-gradient-to-r from-cyan-500/5 via-purple-500/5 to-pink-500/5 animate-pulse"></div>
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_30%_20%,rgba(59,130,246,0.1),transparent_50%)]"></div>
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_70%_80%,rgba(168,85,247,0.1),transparent_50%)]"></div>

      <div className="relative px-6 py-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <motion.button
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={onMenuClick}
              className="lg:hidden p-2 rounded-lg bg-dark-700/50 border border-dark-600/50 text-gray-400 hover:text-white hover:bg-dark-700/70 transition-all duration-200 backdrop-blur-sm"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
              </svg>
            </motion.button>

            <div className="flex items-center space-x-3">
              <div className="relative">
                <Shield className="w-8 h-8 text-cyan-400 drop-shadow-lg" />
                <div className="absolute -top-1 -right-1 w-3 h-3 bg-green-400 rounded-full border-2 border-dark-800 animate-pulse"></div>
              </div>
              <div>
                <h1 className="text-xl font-bold bg-gradient-to-r from-cyan-400 via-blue-400 to-purple-400 bg-clip-text text-transparent">
                  Defect Tracker
                </h1>
                <p className="text-xs text-gray-400 -mt-1">VAPT Portal</p>
              </div>
            </div>
          </div>

          <div className="flex items-center space-x-4">
            {/* Notifications */}
            <motion.button
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              className="relative p-2 rounded-lg bg-dark-700/50 border border-dark-600/50 text-gray-400 hover:text-white hover:bg-dark-700/70 transition-all duration-200 backdrop-blur-sm"
            >
              <Bell className="w-5 h-5" />
              <span className="absolute -top-1 -right-1 w-2 h-2 bg-red-400 rounded-full border border-dark-800"></span>
            </motion.button>

            {/* Settings */}
            <motion.button
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              className="p-2 rounded-lg bg-dark-700/50 border border-dark-600/50 text-gray-400 hover:text-white hover:bg-dark-700/70 transition-all duration-200 backdrop-blur-sm"
            >
              <Settings className="w-5 h-5" />
            </motion.button>

            {/* User Profile */}
            <div className="flex items-center space-x-3 pl-4 border-l border-dark-700/50">
              <div className="flex items-center space-x-3">
                <div className="relative">
                  <div className="w-10 h-10 rounded-full bg-gradient-to-br from-cyan-400 to-purple-500 flex items-center justify-center ring-2 ring-dark-700/50">
                    <User className="w-5 h-5 text-white" />
                  </div>
                  <div className="absolute -bottom-1 -right-1 w-4 h-4 bg-green-400 rounded-full border-2 border-dark-800 flex items-center justify-center">
                    <div className="w-2 h-2 bg-white rounded-full"></div>
                  </div>
                </div>

                <div className="hidden md:block">
                  <p className="text-sm font-medium text-white">
                    {user?.firstName || user?.username}
                  </p>
                  <div className="flex items-center space-x-2">
                    <span className={`text-xs px-2 py-1 rounded-full font-medium ${
                      user?.role === 'ADMIN' ? 'bg-red-500/20 text-red-400 border border-red-500/30' :
                      user?.role === 'TESTER' ? 'bg-blue-500/20 text-blue-400 border border-blue-500/30' :
                      'bg-gray-500/20 text-gray-400 border border-gray-500/30'
                    }`}>
                      {user?.role}
                    </span>
                  </div>
                </div>
              </div>

              <motion.button
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                onClick={logout}
                className="flex items-center space-x-2 p-2 rounded-lg bg-red-500/10 border border-red-500/20 text-red-400 hover:text-white hover:bg-red-500/20 transition-all duration-200 backdrop-blur-sm"
              >
                <LogOut className="w-4 h-4" />
                <span className="hidden sm:inline text-sm font-medium">Logout</span>
              </motion.button>
            </div>
          </div>
        </div>
      </div>

      {/* Bottom glow effect */}
      <div className="absolute bottom-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-cyan-400/50 to-transparent"></div>
    </motion.header>
  )
}

export default Header