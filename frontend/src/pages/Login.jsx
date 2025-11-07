import { useState } from 'react'
import { useAuth } from '../hooks/useAuth'
import { useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import { Shield, Eye, EyeOff, Lock, User, Zap, Activity } from 'lucide-react'
import { motion } from 'framer-motion'

const Login = () => {
  const [credentials, setCredentials] = useState({
    username: '',
    password: ''
  })
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const { login } = useAuth()
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)

    try {
      const result = await login(credentials.username, credentials.password)
      if (result.success) {
        navigate('/dashboard')
      }
    } catch (error) {
      // Error is handled in useAuth hook
    } finally {
      setLoading(false)
    }
  }

  const handleChange = (e) => {
    setCredentials({
      ...credentials,
      [e.target.name]: e.target.value
    })
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-dark-900 via-dark-800 to-dark-900 flex items-center justify-center px-4 relative overflow-hidden">
      {/* Animated background */}
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_20%_20%,rgba(59,130,246,0.1),transparent_50%)]"></div>
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_80%_80%,rgba(168,85,247,0.1),transparent_50%)]"></div>
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_40%_60%,rgba(6,182,212,0.1),transparent_50%)]"></div>

      {/* Floating particles */}
      <div className="absolute inset-0">
        {[...Array(20)].map((_, i) => (
          <motion.div
            key={i}
            className="absolute w-1 h-1 bg-cyan-400/30 rounded-full"
            animate={{
              x: [0, Math.random() * 100 - 50],
              y: [0, Math.random() * 100 - 50],
              opacity: [0.3, 0.8, 0.3],
            }}
            transition={{
              duration: Math.random() * 3 + 2,
              repeat: Infinity,
              repeatType: "reverse",
            }}
            style={{
              left: `${Math.random() * 100}%`,
              top: `${Math.random() * 100}%`,
            }}
          />
        ))}
      </div>

      <div className="max-w-md w-full relative z-10">
        {/* Logo and Title */}
        <motion.div
          initial={{ opacity: 0, y: -50 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8 }}
          className="text-center mb-8"
        >
          <div className="relative inline-flex items-center justify-center w-20 h-20 mb-6">
            <div className="absolute inset-0 bg-gradient-to-r from-cyan-400 via-blue-500 to-purple-500 rounded-full animate-pulse"></div>
            <div className="relative w-18 h-18 bg-dark-900 rounded-full flex items-center justify-center border border-cyan-400/30">
              <Shield className="w-10 h-10 text-cyan-400 drop-shadow-lg" />
            </div>
            <div className="absolute -top-1 -right-1 w-4 h-4 bg-green-400 rounded-full border-2 border-dark-900 animate-pulse">
              <div className="w-full h-full bg-green-400 rounded-full animate-ping"></div>
            </div>
          </div>

          <motion.h1
            initial={{ opacity: 0, scale: 0.8 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ delay: 0.2, duration: 0.6 }}
            className="text-4xl font-bold bg-gradient-to-r from-cyan-400 via-blue-400 to-purple-400 bg-clip-text text-transparent mb-2"
          >
            Defect Tracker
          </motion.h1>

          <motion.p
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.4, duration: 0.6 }}
            className="text-gray-400 text-lg"
          >
            VAPT Cybersecurity Portal
          </motion.p>

          <motion.div
            initial={{ scaleX: 0 }}
            animate={{ scaleX: 1 }}
            transition={{ delay: 0.6, duration: 0.8 }}
            className="mt-4 h-0.5 w-32 mx-auto bg-gradient-to-r from-transparent via-cyan-400 to-transparent"
          ></motion.div>
        </motion.div>

        {/* Login Form */}
        <motion.div
          initial={{ opacity: 0, y: 50 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3, duration: 0.8 }}
          className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-dark-800/50 to-dark-900/50 border border-dark-700/50 backdrop-blur-xl shadow-2xl"
        >
          {/* Form background glow */}
          <div className="absolute inset-0 bg-gradient-to-br from-cyan-500/5 via-transparent to-purple-500/5 rounded-2xl"></div>

          <div className="relative p-8">
            <div className="text-center mb-8">
              <h2 className="text-2xl font-bold text-white mb-2">Secure Access</h2>
              <p className="text-gray-400 text-sm">Enter your credentials to continue</p>
            </div>

            <form onSubmit={handleSubmit} className="space-y-6">
              <motion.div
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.5 }}
              >
                <label className="block text-sm font-medium text-gray-300 mb-3 flex items-center">
                  <User className="w-4 h-4 mr-2 text-cyan-400" />
                  Username
                </label>
                <div className="relative">
                  <input
                    type="text"
                    name="username"
                    value={credentials.username}
                    onChange={handleChange}
                    className="w-full px-4 py-3 bg-dark-700/50 border border-dark-600/50 rounded-xl text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-cyan-400/50 focus:border-cyan-400/50 transition-all duration-200 backdrop-blur-sm"
                    placeholder="Enter your username"
                    required
                  />
                  <div className="absolute inset-0 rounded-xl bg-gradient-to-r from-cyan-400/5 to-purple-400/5 opacity-0 hover:opacity-100 transition-opacity duration-200 pointer-events-none"></div>
                </div>
              </motion.div>

              <motion.div
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.6 }}
              >
                <label className="block text-sm font-medium text-gray-300 mb-3 flex items-center">
                  <Lock className="w-4 h-4 mr-2 text-cyan-400" />
                  Password
                </label>
                <div className="relative">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    name="password"
                    value={credentials.password}
                    onChange={handleChange}
                    className="w-full px-4 py-3 pr-12 bg-dark-700/50 border border-dark-600/50 rounded-xl text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-cyan-400/50 focus:border-cyan-400/50 transition-all duration-200 backdrop-blur-sm"
                    placeholder="Enter your password"
                    required
                  />
                  <motion.button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-cyan-400 transition-colors duration-200"
                    whileHover={{ scale: 1.1 }}
                    whileTap={{ scale: 0.9 }}
                  >
                    {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                  </motion.button>
                  <div className="absolute inset-0 rounded-xl bg-gradient-to-r from-cyan-400/5 to-purple-400/5 opacity-0 hover:opacity-100 transition-opacity duration-200 pointer-events-none"></div>
                </div>
              </motion.div>

              <motion.button
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.7 }}
                type="submit"
                disabled={loading}
                className="relative w-full overflow-hidden rounded-xl bg-gradient-to-r from-cyan-500 via-blue-500 to-purple-500 p-0.5 transition-all duration-200 hover:shadow-lg hover:shadow-cyan-500/25 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <div className="w-full px-6 py-3 bg-dark-900 rounded-xl flex items-center justify-center space-x-2 hover:bg-dark-800/50 transition-colors duration-200">
                  {loading ? (
                    <>
                      <div className="animate-spin rounded-full h-5 w-5 border-2 border-white border-t-transparent"></div>
                      <span className="text-white font-medium">Authenticating...</span>
                    </>
                  ) : (
                    <>
                      <Zap className="w-5 h-5" />
                      <span className="text-white font-medium">Access Portal</span>
                    </>
                  )}
                </div>
              </motion.button>
            </form>

          </div>
        </motion.div>

        {/* Footer */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 1 }}
          className="text-center mt-6 text-xs text-gray-500"
        >
          <p>© 2025 Defect Tracker - VAPT Cybersecurity Portal</p>
          <p className="mt-1">Securing digital assets with advanced vulnerability management</p>
        </motion.div>
      </div>
    </div>
  )
}

export default Login