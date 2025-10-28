import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import {
  LayoutDashboard,
  Users,
  Building,
  FileText,
  Bug,
  Shield,
  X
} from 'lucide-react'

const Sidebar = ({ isOpen, setIsOpen }) => {
  const location = useLocation()
  const { user } = useAuth()

  const menuItems = [
    {
      path: '/dashboard',
      label: 'Dashboard',
      icon: LayoutDashboard,
      roles: ['ADMIN', 'TESTER', 'CLIENT']
    },
    {
      path: '/clients',
      label: 'Clients',
      icon: Users,
      roles: ['ADMIN']
    },
    {
      path: '/applications',
      label: 'Applications',
      icon: Building,
      roles: ['ADMIN', 'TESTER']
    },
    {
      path: '/test-plans',
      label: 'Test Plans',
      icon: FileText,
      roles: ['ADMIN', 'TESTER']
    },
    {
      path: '/vapt-reports',
      label: 'VAPT Report',
      icon: Shield,
      roles: ['ADMIN', 'TESTER']
    },
    {
      path: '/defects',
      label: 'Defects',
      icon: Bug,
      roles: ['ADMIN', 'TESTER']
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
        <div
          className="fixed inset-0 bg-black bg-opacity-50 z-40 lg:hidden"
          onClick={() => setIsOpen(false)}
        />
      )}

      {/* Sidebar */}
      <div className={`
        fixed inset-y-0 left-0 z-50 w-64 bg-dark-800 border-r border-dark-700 transform transition-transform duration-300 ease-in-out lg:translate-x-0 lg:static lg:inset-0 lg:top-0 lg:h-screen
        ${isOpen ? 'translate-x-0' : '-translate-x-full'}
      `}>
        <div className="flex items-center justify-between p-4 border-b border-dark-700">
          <h2 className="text-lg font-semibold text-white">Navigation</h2>
          <button
            onClick={() => setIsOpen(false)}
            className="text-gray-400 hover:text-white lg:hidden"
          >
            <X className="w-6 h-6" />
          </button>
        </div>

        <nav className="mt-8">
          <ul className="space-y-2 px-4 pb-20">
            {filteredMenuItems.map((item) => {
              const Icon = item.icon
              const isActive = location.pathname === item.path

              return (
                <li key={item.path}>
                  <Link
                    to={item.path}
                    onClick={() => setIsOpen(false)}
                    className={`
                      flex items-center px-4 py-3 text-sm font-medium rounded-lg transition-colors
                      ${isActive
                        ? 'bg-primary-500/20 text-primary-400 border-r-2 border-primary-400'
                        : 'text-gray-300 hover:bg-dark-700 hover:text-white'
                      }
                    `}
                  >
                    <Icon className="w-5 h-5 mr-3" />
                    {item.label}
                  </Link>
                </li>
              )
            })}
          </ul>
        </nav>

        <div className="absolute bottom-4 left-4 right-4">
          <div className="bg-dark-700 rounded-lg p-4 text-center">
            <h3 className="text-sm font-medium text-white mb-2">VAPT Portal</h3>
            <p className="text-xs text-gray-400">
              Cybersecurity defect tracking system
            </p>
          </div>
        </div>
      </div>
    </>
  )
}

export default Sidebar