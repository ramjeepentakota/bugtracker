import { useState, useEffect } from 'react'
import axios from 'axios'
import { motion } from 'framer-motion'
import {
  Users,
  FolderOpen,
  FileText,
  Bug,
  TrendingUp,
  AlertTriangle,
  CheckCircle,
  Clock,
  Shield,
  Activity,
  Zap,
  Target,
  ChevronRight,
  Building,
  BarChart3,
  UserPlus
} from 'lucide-react'
import {
  PieChart,
  Pie,
  Cell,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  LineChart,
  Line,
  AreaChart,
  Area
} from 'recharts'

const Dashboard = () => {
  const [stats, setStats] = useState(null)
  const [monthlyTrendsData, setMonthlyTrendsData] = useState([])
  const [loading, setLoading] = useState(true)
  const [activeTab, setActiveTab] = useState('overview')

  useEffect(() => {
    fetchDashboardStats()
    fetchMonthlyTrends()
  }, [])

  const fetchDashboardStats = async () => {
    try {
      const response = await axios.get('/api/dashboard/stats')
      setStats(response.data)
    } catch (error) {
      console.error('Error fetching dashboard stats:', error)
      // Set default stats if API fails
      setStats({
        totalUsers: 0,
        totalClients: 0,
        totalApplications: 0,
        totalTestPlans: 0,
        openVulnerabilities: 0,
        closedVulnerabilities: 0,
        defectsBySeverity: {},
        defectsByStatus: {}
      })
    } finally {
      setLoading(false)
    }
  }

  const fetchMonthlyTrends = async () => {
    try {
      const response = await axios.get('/api/dashboard/monthly-trends')
      setMonthlyTrendsData(response.data.data || [])
    } catch (error) {
      console.error('Error fetching monthly trends:', error)
      // Set empty array if API fails
      setMonthlyTrendsData([])
    }
  }

  // Auto-refresh data every 30 seconds for live updates
  useEffect(() => {
    const interval = setInterval(() => {
      fetchDashboardStats()
      fetchMonthlyTrends()
    }, 30000) // 30 seconds

    return () => clearInterval(interval)
  }, [])

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="relative">
          <div className="animate-spin rounded-full h-16 w-16 border-4 border-primary-500 border-t-transparent"></div>
          <div className="absolute inset-0 rounded-full border-4 border-primary-400/20 animate-pulse"></div>
        </div>
      </div>
    )
  }

  const statCards = [
    {
      title: 'Total Users',
      value: stats?.totalUsers || 0,
      icon: Users,
      color: 'text-blue-400',
      bgColor: 'bg-gradient-to-br from-blue-500/20 to-indigo-500/20',
      borderColor: 'border-blue-500/30',
      glowColor: 'shadow-blue-500/25'
    },
    {
      title: 'Total Clients',
      value: stats?.totalClients || 0,
      icon: FolderOpen,
      color: 'text-cyan-400',
      bgColor: 'bg-gradient-to-br from-cyan-500/20 to-blue-500/20',
      borderColor: 'border-cyan-500/30',
      glowColor: 'shadow-cyan-500/25'
    },
    {
      title: 'Total Applications',
      value: stats?.totalApplications || 0,
      icon: FileText,
      color: 'text-emerald-400',
      bgColor: 'bg-gradient-to-br from-emerald-500/20 to-green-500/20',
      borderColor: 'border-emerald-500/30',
      glowColor: 'shadow-emerald-500/25'
    },
    {
      title: 'Test Plans',
      value: stats?.totalTestPlans || 0,
      icon: Target,
      color: 'text-purple-400',
      bgColor: 'bg-gradient-to-br from-purple-500/20 to-violet-500/20',
      borderColor: 'border-purple-500/30',
      glowColor: 'shadow-purple-500/25'
    },
    {
      title: 'Open Vulnerabilities',
      value: stats?.openVulnerabilities || 0,
      icon: AlertTriangle,
      color: 'text-red-400',
      bgColor: 'bg-gradient-to-br from-red-500/20 to-pink-500/20',
      borderColor: 'border-red-500/30',
      glowColor: 'shadow-red-500/25'
    },
    {
      title: 'Closed Vulnerabilities',
      value: stats?.closedVulnerabilities || 0,
      icon: Shield,
      color: 'text-green-400',
      bgColor: 'bg-gradient-to-br from-green-500/20 to-emerald-500/20',
      borderColor: 'border-green-500/30',
      glowColor: 'shadow-green-500/25'
    }
  ]

  // Prepare chart data
  const severityData = stats?.defectsBySeverity ? Object.entries(stats.defectsBySeverity).map(([key, value]) => ({
    name: key.charAt(0).toUpperCase() + key.slice(1).toLowerCase(),
    value: value,
    color: key === 'CRITICAL' ? '#ef4444' :
           key === 'HIGH' ? '#f59e0b' :
           key === 'MEDIUM' ? '#eab308' :
           key === 'LOW' ? '#3b82f6' : '#6b7280'
  })).filter(item => item.value > 0) : []

  const statusData = stats?.defectsByStatus ? Object.entries(stats.defectsByStatus).map(([key, value]) => ({
    name: key.replace('_', ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase()),
    value: value
  })).filter(item => item.value > 0) : []


  const tabs = [
    {
      id: 'overview',
      label: 'Overview',
      icon: BarChart3,
      color: 'text-cyan-400',
      bgColor: 'bg-cyan-500/10'
    },
    {
      id: 'user-registration',
      label: 'User Registration',
      icon: UserPlus,
      color: 'text-indigo-400',
      bgColor: 'bg-indigo-500/10'
    },
    {
      id: 'clients',
      label: 'Clients',
      icon: Users,
      color: 'text-blue-400',
      bgColor: 'bg-blue-500/10'
    },
    {
      id: 'applications',
      label: 'Applications',
      icon: Building,
      color: 'text-emerald-400',
      bgColor: 'bg-emerald-500/10'
    },
    {
      id: 'test-plans',
      label: 'Test Plans',
      icon: Target,
      color: 'text-purple-400',
      bgColor: 'bg-purple-500/10'
    },
    {
      id: 'vapt-reports',
      label: 'VAPT Reports',
      icon: Shield,
      color: 'text-orange-400',
      bgColor: 'bg-orange-500/10'
    },
    {
      id: 'defects',
      label: 'Defects',
      icon: Bug,
      color: 'text-red-400',
      bgColor: 'bg-red-500/10'
    }
  ]

  const renderTabContent = () => {
    switch (activeTab) {
      case 'overview':
        return (
          <>
            {/* Stats Cards */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
              {statCards.map((card, index) => (
                <motion.div
                  key={card.title}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: index * 0.1, duration: 0.5 }}
                  whileHover={{ scale: 1.02, y: -5 }}
                  className={`relative overflow-hidden rounded-2xl border backdrop-blur-xl ${card.bgColor} ${card.borderColor} ${card.glowColor} shadow-2xl`}
                >
                  {/* Animated background gradient */}
                  <div className="absolute inset-0 bg-gradient-to-br from-transparent via-transparent to-white/5 opacity-0 hover:opacity-100 transition-opacity duration-500"></div>

                  {/* Glow effect */}
                  <div className={`absolute -inset-1 bg-gradient-to-r ${card.bgColor} rounded-2xl blur opacity-20 group-hover:opacity-40 transition-opacity duration-500`}></div>

                  <div className="relative p-6">
                    <div className="flex items-center justify-between mb-4">
                      <div className={`p-3 rounded-xl ${card.bgColor} ring-1 ring-white/10`}>
                        <card.icon className={`w-6 h-6 ${card.color}`} />
                      </div>
                      <div className="flex items-center space-x-1">
                        <div className="w-2 h-2 bg-green-400 rounded-full animate-pulse"></div>
                        <span className="text-xs text-green-400 font-medium">Live</span>
                      </div>
                    </div>

                    <div>
                      <p className="text-sm text-gray-400 mb-2 font-medium">{card.title}</p>
                      <p className="text-3xl font-bold text-white mb-1">{card.value.toLocaleString()}</p>
                      <div className="flex items-center space-x-1">
                        <TrendingUp className="w-4 h-4 text-green-400" />
                        <span className="text-xs text-green-400 font-medium">+12.5%</span>
                        <span className="text-xs text-gray-500">vs last month</span>
                      </div>
                    </div>
                  </div>
                </motion.div>
              ))}
            </div>

          </>
        )
      case 'user-registration':
        return (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            <div className="text-center py-12">
              <UserPlus className="w-16 h-16 text-indigo-400 mx-auto mb-4" />
              <h3 className="text-2xl font-bold text-white mb-2">User Registration</h3>
              <p className="text-gray-400 mb-6">Register new users and assign roles for dashboard access</p>
              <button
                onClick={() => window.location.href = '/user-registration'}
                className="px-6 py-3 bg-gradient-to-r from-indigo-500 to-purple-500 text-white rounded-lg font-medium hover:from-indigo-600 hover:to-purple-600 transition-all duration-200"
              >
                Go to User Registration
              </button>
            </div>
          </motion.div>
        )
      case 'clients':
        return (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            <div className="text-center py-12">
              <Users className="w-16 h-16 text-blue-400 mx-auto mb-4" />
              <h3 className="text-2xl font-bold text-white mb-2">Client Management</h3>
              <p className="text-gray-400 mb-6">Manage your organization clients and their access</p>
              <button
                onClick={() => window.location.href = '/clients'}
                className="px-6 py-3 bg-gradient-to-r from-blue-500 to-cyan-500 text-white rounded-lg font-medium hover:from-blue-600 hover:to-cyan-600 transition-all duration-200"
              >
                Go to Clients
              </button>
            </div>
          </motion.div>
        )
      case 'applications':
        return (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            <div className="text-center py-12">
              <Building className="w-16 h-16 text-emerald-400 mx-auto mb-4" />
              <h3 className="text-2xl font-bold text-white mb-2">Application Portfolio</h3>
              <p className="text-gray-400 mb-6">Manage and monitor your application ecosystem</p>
              <button
                onClick={() => window.location.href = '/applications'}
                className="px-6 py-3 bg-gradient-to-r from-emerald-500 to-green-500 text-white rounded-lg font-medium hover:from-emerald-600 hover:to-green-600 transition-all duration-200"
              >
                Go to Applications
              </button>
            </div>
          </motion.div>
        )
      case 'test-plans':
        return (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            <div className="text-center py-12">
              <Target className="w-16 h-16 text-purple-400 mx-auto mb-4" />
              <h3 className="text-2xl font-bold text-white mb-2">Test Plans</h3>
              <p className="text-gray-400 mb-6">Create and manage vulnerability assessment plans</p>
              <button
                onClick={() => window.location.href = '/test-plans'}
                className="px-6 py-3 bg-gradient-to-r from-purple-500 to-violet-500 text-white rounded-lg font-medium hover:from-purple-600 hover:to-violet-600 transition-all duration-200"
              >
                Go to Test Plans
              </button>
            </div>
          </motion.div>
        )
      case 'vapt-reports':
        return (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            <div className="text-center py-12">
              <Shield className="w-16 h-16 text-orange-400 mx-auto mb-4" />
              <h3 className="text-2xl font-bold text-white mb-2">VAPT Reports</h3>
              <p className="text-gray-400 mb-6">View and analyze security assessment reports</p>
              <button
                onClick={() => window.location.href = '/vapt-reports'}
                className="px-6 py-3 bg-gradient-to-r from-orange-500 to-red-500 text-white rounded-lg font-medium hover:from-orange-600 hover:to-red-600 transition-all duration-200"
              >
                Go to VAPT Reports
              </button>
            </div>
          </motion.div>
        )
      case 'defects':
        return (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            <div className="text-center py-12">
              <Bug className="w-16 h-16 text-red-400 mx-auto mb-4" />
              <h3 className="text-2xl font-bold text-white mb-2">Defect Tracking</h3>
              <p className="text-gray-400 mb-6">Track and resolve security vulnerabilities</p>
              <button
                onClick={() => window.location.href = '/defects'}
                className="px-6 py-3 bg-gradient-to-r from-red-500 to-pink-500 text-white rounded-lg font-medium hover:from-red-600 hover:to-pink-600 transition-all duration-200"
              >
                Go to Defects
              </button>
            </div>
          </motion.div>
        )
      default:
        return null
    }
  }

  return (
    <div className="space-y-8 max-w-7xl mx-auto">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className="flex items-center justify-between"
      >
        <div>
          <h1 className="text-4xl font-bold bg-gradient-to-r from-cyan-400 via-blue-400 to-purple-400 bg-clip-text text-transparent">
            Cybersecurity Dashboard
          </h1>
          <p className="text-gray-400 mt-2">Real-time VAPT analytics and insights</p>
        </div>
        <div className="flex items-center space-x-2 text-sm text-gray-400 bg-dark-800/50 px-4 py-2 rounded-full border border-dark-700">
          <Activity className="w-4 h-4" />
          <span>Last updated: {new Date().toLocaleString()}</span>
        </div>
      </motion.div>


      {/* Tabs */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
        className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-dark-800/50 to-dark-900/50 border border-dark-700/50 backdrop-blur-xl shadow-2xl"
      >
        <div className="absolute inset-0 bg-gradient-to-br from-cyan-500/5 via-transparent to-purple-500/5"></div>
        <div className="relative p-6">
          <div className="flex flex-wrap gap-2 mb-6">
            {tabs.map((tab, index) => {
              const Icon = tab.icon
              const isActive = activeTab === tab.id

              return (
                <motion.button
                  key={tab.id}
                  initial={{ opacity: 0, scale: 0.9 }}
                  animate={{ opacity: 1, scale: 1 }}
                  transition={{ delay: index * 0.05 }}
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                  onClick={() => setActiveTab(tab.id)}
                  className={`
                    relative flex items-center px-4 py-3 rounded-xl font-medium transition-all duration-300 overflow-hidden
                    ${isActive
                      ? 'bg-gradient-to-r from-cyan-500/20 to-blue-500/20 text-cyan-300 border border-cyan-400/30 shadow-lg shadow-cyan-500/20'
                      : 'text-gray-300 hover:bg-dark-700/50 hover:text-white'
                    }
                  `}
                >
                  {/* Active background glow */}
                  {isActive && (
                    <div className="absolute inset-0 bg-gradient-to-r from-cyan-500/10 to-blue-500/10 rounded-xl"></div>
                  )}

                  {/* Hover effect */}
                  <div className="absolute inset-0 bg-gradient-to-r from-transparent to-white/5 opacity-0 hover:opacity-100 transition-opacity duration-300 rounded-xl"></div>

                  <div className={`relative flex items-center justify-center w-6 h-6 rounded-lg mr-3 transition-all duration-300 ${
                    isActive ? `${tab.bgColor} ring-2 ring-cyan-400/30` : 'bg-dark-700/50'
                  }`}>
                    <Icon className={`w-4 h-4 ${isActive ? tab.color : 'text-gray-400'}`} />
                  </div>

                  <span className={`relative ${isActive ? 'text-white' : 'text-gray-300'}`}>
                    {tab.label}
                  </span>

                  {/* Active indicator */}
                  {isActive && (
                    <div className="absolute bottom-0 left-1/2 -translate-x-1/2 w-8 h-0.5 bg-gradient-to-r from-cyan-400 to-blue-400 rounded-full"></div>
                  )}
                </motion.button>
              )
            })}
          </div>

          {/* Tab Content */}
          <motion.div
            key={activeTab}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.3 }}
            className="min-h-[400px]"
          >
            {renderTabContent()}
          </motion.div>
        </div>
      </motion.div>
    </div>
  )
}

export default Dashboard