import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import {
  LayoutDashboard,
  Users,
  Building,
  FileText,
  Bug,
  Shield,
  X,
  ChevronRight,
  Activity,
  Zap,
  UserPlus,
  Settings
} from 'lucide-react'
import { motion } from 'framer-motion'

const Sidebar = ({ isOpen, setIsOpen }) => {
  const location = useLocation()
  const { user } = useAuth()

  const menuItems = [
    {
      path: '/dashboard',
      label: 'Dashboard',
      icon: LayoutDashboard,
      roles: ['ADMIN', 'TESTER', 'CLIENT', 'PM_DM'],
      color: 'text-cyan-400',
      bgColor: 'bg-cyan-500/10',
      description: 'Analytics & Insights'
    },
    {
      path: '/user-registration',
      label: 'User Registration',
      icon: UserPlus,
      roles: ['ADMIN'],
      color: 'text-indigo-400',
      bgColor: 'bg-indigo-500/10',
      description: 'Register New Users'
    },
    {
      path: '/user-management',
      label: 'User Management',
      icon: Settings,
      roles: ['ADMIN'],
      color: 'text-amber-400',
      bgColor: 'bg-amber-500/10',
      description: 'Manage User Accounts'
    },
    {
      path: '/clients',
      label: 'Clients',
      icon: Users,
      roles: ['ADMIN', 'PM_DM'],
      color: 'text-blue-400',
      bgColor: 'bg-blue-500/10',
      description: 'Manage Organizations'
    },
    {
      path: '/applications',
      label: 'Applications',
      icon: Building,
      roles: ['ADMIN', 'TESTER', 'PM_DM'],
      color: 'text-emerald-400',
      bgColor: 'bg-emerald-500/10',
      description: 'App Portfolio'
    },
    {
      path: '/test-plans',
      label: 'Test Plans',
      icon: FileText,
      roles: ['ADMIN', 'TESTER'],
      color: 'text-purple-400',
      bgColor: 'bg-purple-500/10',
      description: 'Vulnerability Tests'
    },
    {
      path: '/vapt-reports',
      label: 'VAPT Reports',
      icon: Shield,
      roles: ['ADMIN', 'TESTER', 'PM_DM'],
      color: 'text-orange-400',
      bgColor: 'bg-orange-500/10',
      description: 'Assessment Reports'
    },
    {
      path: '/defects',
      label: 'Defects',
      icon: Bug,
      roles: ['ADMIN', 'TESTER'],
      color: 'text-red-400',
      bgColor: 'bg-red-500/10',
      description: 'Security Issues'
    }
  ]

  // Filter menu items based on user role
  const filteredMenuItems = menuItems.filter(item =>
    !item.roles || item.roles.includes(user?.role)
  )

  return (
    <>
      {/* Mobile overlay */}
      {isOpen && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 bg-black/60 backdrop-blur-sm z-40 lg:hidden"
          onClick={() => setIsOpen(false)}
        />
      )}

      {/* Sidebar */}
      <motion.div
        initial={{ x: -320 }}
        animate={{ x: isOpen ? 0 : -320 }}
        transition={{ type: "spring", damping: 30, stiffness: 300 }}
        className={`
          fixed inset-y-0 left-0 z-50 w-80 bg-gradient-to-b from-dark-900 via-dark-800 to-dark-900 border-r border-dark-700/50 backdrop-blur-xl shadow-2xl lg:translate-x-0 lg:static lg:inset-0 lg:top-0 lg:h-screen overflow-hidden
        `}
      >
        {/* Animated background */}
        <div className="absolute inset-0 bg-gradient-to-b from-cyan-500/5 via-transparent to-purple-500/5"></div>
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_20%_30%,rgba(59,130,246,0.1),transparent_70%)]"></div>

        <div className="relative h-full flex flex-col">
          {/* Header */}
          <div className="flex items-center justify-between p-6 border-b border-dark-700/50">
            <div className="flex items-center space-x-3">
              <div className="relative">
                <Shield className="w-8 h-8 text-cyan-400" />
                <div className="absolute -top-1 -right-1 w-3 h-3 bg-green-400 rounded-full border-2 border-dark-800 animate-pulse"></div>
              </div>
              <div>
                <h2 className="text-lg font-bold text-white">Navigation</h2>
                <p className="text-xs text-gray-400">VAPT Portal</p>
              </div>
            </div>
            <motion.button
              whileHover={{ scale: 1.1 }}
              whileTap={{ scale: 0.9 }}
              onClick={() => setIsOpen(false)}
              className="lg:hidden p-2 rounded-lg bg-dark-700/50 border border-dark-600/50 text-gray-400 hover:text-white hover:bg-dark-700/70 transition-all duration-200"
            >
              <X className="w-5 h-5" />
            </motion.button>
          </div>

          {/* Navigation */}
          <nav className="flex-1 mt-6 px-4">
            <ul className="space-y-2">
              {filteredMenuItems.map((item, index) => {
                const Icon = item.icon
                const isActive = location.pathname === item.path

                return (
                  <motion.li
                    key={item.path}
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: index * 0.1 }}
                  >
                    <Link
                      to={item.path}
                      onClick={() => setIsOpen(false)}
                      className={`
                        group relative flex items-center px-4 py-4 text-sm font-medium rounded-xl transition-all duration-300 overflow-hidden
                        ${isActive
                          ? 'bg-gradient-to-r from-cyan-500/20 to-blue-500/20 text-cyan-300 border-r-2 border-cyan-400 shadow-lg shadow-cyan-500/20'
                          : 'text-gray-300 hover:bg-dark-700/50 hover:text-white hover:translate-x-1'
                        }
                      `}
                    >
                      {/* Active background glow */}
                      {isActive && (
                        <div className="absolute inset-0 bg-gradient-to-r from-cyan-500/10 to-blue-500/10 rounded-xl"></div>
                      )}

                      {/* Hover effect */}
                      <div className="absolute inset-0 bg-gradient-to-r from-transparent to-white/5 opacity-0 group-hover:opacity-100 transition-opacity duration-300 rounded-xl"></div>

                      <div className={`relative flex items-center justify-center w-10 h-10 rounded-lg mr-4 transition-all duration-300 ${
                        isActive ? `${item.bgColor} ring-2 ring-cyan-400/30` : 'bg-dark-700/50 group-hover:bg-dark-700/70'
                      }`}>
                        <Icon className={`w-5 h-5 ${isActive ? item.color : 'text-gray-400 group-hover:text-white'}`} />
                      </div>

                      <div className="flex-1 relative">
                        <div className="flex items-center justify-between">
                          <span className={`font-medium ${isActive ? 'text-white' : 'text-gray-300'}`}>
                            {item.label}
                          </span>
                          <ChevronRight className={`w-4 h-4 transition-transform duration-300 ${
                            isActive ? 'text-cyan-400 rotate-90' : 'text-gray-500 group-hover:text-gray-400 group-hover:translate-x-1'
                          }`} />
                        </div>
                        <p className={`text-xs mt-1 ${isActive ? 'text-cyan-300/70' : 'text-gray-500 group-hover:text-gray-400'}`}>
                          {item.description}
                        </p>
                      </div>

                      {/* Active indicator */}
                      {isActive && (
                        <div className="absolute left-0 top-1/2 -translate-y-1/2 w-1 h-8 bg-gradient-to-b from-cyan-400 to-blue-400 rounded-r-full"></div>
                      )}
                    </Link>
                  </motion.li>
                )
              })}
            </ul>
          </nav>

          {/* Footer */}
          <div className="p-4 border-t border-dark-700/50">
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.8 }}
              className="relative overflow-hidden rounded-xl bg-gradient-to-r from-dark-800/50 to-dark-700/50 border border-dark-600/30 backdrop-blur-sm p-4"
            >
              <div className="absolute inset-0 bg-gradient-to-r from-cyan-500/5 to-purple-500/5 rounded-xl"></div>
              <div className="relative flex items-center space-x-3">
                <div className="flex items-center justify-center w-10 h-10 rounded-lg bg-gradient-to-br from-cyan-400/20 to-purple-500/20 border border-cyan-400/30">
                  <Activity className="w-5 h-5 text-cyan-400" />
                </div>
                <div>
                  <h3 className="text-sm font-semibold text-white">System Status</h3>
                  <div className="flex items-center space-x-2 mt-1">
                    <div className="flex items-center space-x-1">
                      <div className="w-2 h-2 bg-green-400 rounded-full animate-pulse"></div>
                      <span className="text-xs text-green-400 font-medium">Online</span>
                    </div>
                    <span className="text-xs text-gray-500">•</span>
                    <span className="text-xs text-gray-400">All systems operational</span>
                  </div>
                </div>
              </div>
            </motion.div>
          </div>
        </div>
      </motion.div>
    </>
  )
}

export default Sidebar