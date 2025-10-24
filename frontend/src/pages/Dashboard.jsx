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
  Clock
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
  Line
} from 'recharts'

const Dashboard = () => {
  const [stats, setStats] = useState(null)
  const [monthlyTrendsData, setMonthlyTrendsData] = useState([])
  const [loading, setLoading] = useState(true)

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
        totalClients: 0,
        totalApplications: 0,
        totalTestPlans: 0,
        totalDefects: 0,
        openDefects: 0,
        closedDefects: 0,
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

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-500"></div>
      </div>
    )
  }

  const statCards = [
    {
      title: 'Total Clients',
      value: stats?.totalClients || 0,
      icon: Users,
      color: 'text-blue-400',
      bgColor: 'bg-blue-500/10'
    },
    {
      title: 'Total Applications',
      value: stats?.totalApplications || 0,
      icon: FolderOpen,
      color: 'text-green-400',
      bgColor: 'bg-green-500/10'
    },
    {
      title: 'Test Plans',
      value: stats?.totalTestPlans || 0,
      icon: FileText,
      color: 'text-purple-400',
      bgColor: 'bg-purple-500/10'
    },
    {
      title: 'Total Defects',
      value: stats?.totalDefects || 0,
      icon: Bug,
      color: 'text-red-400',
      bgColor: 'bg-red-500/10'
    },
    {
      title: 'Open Defects',
      value: stats?.openDefects || 0,
      icon: Clock,
      color: 'text-yellow-400',
      bgColor: 'bg-yellow-500/10'
    },
    {
      title: 'Closed Defects',
      value: stats?.closedDefects || 0,
      icon: CheckCircle,
      color: 'text-green-400',
      bgColor: 'bg-green-500/10'
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
  })) : []

  const statusData = stats?.defectsByStatus ? Object.entries(stats.defectsByStatus).map(([key, value]) => ({
    name: key.replace('_', ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase()),
    value: value
  })) : []

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-white">Dashboard</h1>
        <div className="text-sm text-gray-400">
          Last updated: {new Date().toLocaleString()}
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
        {statCards.map((card, index) => (
          <motion.div
            key={card.title}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: index * 0.1 }}
            className="card p-6"
          >
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-400 mb-1">{card.title}</p>
                <p className="text-3xl font-bold text-white">{card.value}</p>
              </div>
              <div className={`p-4 rounded-xl ${card.bgColor}`}>
                <card.icon className={`w-8 h-8 ${card.color}`} />
              </div>
            </div>
          </motion.div>
        ))}
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Severity Pie Chart */}
        <motion.div
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          className="card p-6"
        >
          <h3 className="text-xl font-semibold text-white mb-6">Defects by Severity</h3>
          <ResponsiveContainer width="100%" height={350}>
            <PieChart>
              <Pie
                data={severityData}
                cx="50%"
                cy="50%"
                outerRadius={100}
                dataKey="value"
                label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
              >
                {severityData.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={entry.color} />
                ))}
              </Pie>
              <Tooltip
                contentStyle={{
                  backgroundColor: '#1f2937',
                  border: '1px solid #374151',
                  borderRadius: '8px',
                  color: '#fff'
                }}
              />
            </PieChart>
          </ResponsiveContainer>
        </motion.div>

        {/* Status Bar Chart */}
        <motion.div
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
          className="card p-6"
        >
          <h3 className="text-xl font-semibold text-white mb-6">Defects by Status</h3>
          <ResponsiveContainer width="100%" height={350}>
            <BarChart data={statusData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#374151" />
              <XAxis dataKey="name" stroke="#9ca3af" fontSize={12} />
              <YAxis stroke="#9ca3af" />
              <Tooltip
                contentStyle={{
                  backgroundColor: '#1f2937',
                  border: '1px solid #374151',
                  borderRadius: '8px',
                  color: '#fff'
                }}
              />
              <Bar dataKey="value" fill="#3b82f6" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </motion.div>
      </div>

      {/* Trend Chart */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="card p-6"
      >
        <h3 className="text-xl font-semibold text-white mb-6">Defect Trends (Last 6 Months)</h3>
        <ResponsiveContainer width="100%" height={350}>
          <LineChart data={monthlyTrendsData}>
            <CartesianGrid strokeDasharray="3 3" stroke="#374151" />
            <XAxis dataKey="month" stroke="#9ca3af" fontSize={12} />
            <YAxis stroke="#9ca3af" />
            <Tooltip
              contentStyle={{
                backgroundColor: '#1f2937',
                border: '1px solid #374151',
                borderRadius: '8px',
                color: '#fff'
              }}
            />
            <Line
              type="monotone"
              dataKey="count"
              stroke="#3b82f6"
              strokeWidth={3}
              dot={{ fill: '#3b82f6', strokeWidth: 2, r: 6 }}
              activeDot={{ r: 8, stroke: '#3b82f6', strokeWidth: 2, fill: '#1f2937' }}
            />
          </LineChart>
        </ResponsiveContainer>
      </motion.div>
    </div>
  )
}

export default Dashboard