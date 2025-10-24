import { useState } from 'react'
import { useAuth } from '../hooks/useAuth'
import { useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import { Bug, Eye, EyeOff } from 'lucide-react'

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
    <div className="min-h-screen bg-dark-900 flex items-center justify-center px-4">
      <div className="max-w-md w-full">
        {/* Logo and Title */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-primary-500/20 rounded-full mb-4">
            <Bug className="w-8 h-8 text-primary-400" />
          </div>
          <h1 className="text-3xl font-bold text-white mb-2">Defect Tracker</h1>
          <p className="text-gray-400">VAPT Cybersecurity Portal</p>
        </div>

        {/* Login Form */}
        <div className="card">
          <h2 className="text-xl font-semibold text-white mb-6 text-center">Sign In</h2>

          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">
                Username
              </label>
              <input
                type="text"
                name="username"
                value={credentials.username}
                onChange={handleChange}
                className="input-field w-full"
                placeholder="Enter your username"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">
                Password
              </label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  name="password"
                  value={credentials.password}
                  onChange={handleChange}
                  className="input-field w-full pr-10"
                  placeholder="Enter your password"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-300"
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="btn-primary w-full flex items-center justify-center"
            >
              {loading ? (
                <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
              ) : (
                'Sign In'
              )}
            </button>
          </form>

          {/* Demo Credentials */}
          <div className="mt-6 p-4 bg-dark-700 rounded-lg">
            <h3 className="text-sm font-medium text-white mb-2">Demo Credentials</h3>
            <div className="text-xs text-gray-400 space-y-1">
              <div><strong>Admin:</strong> admin / password</div>
              <div><strong>Tester:</strong> tester1 / password</div>
              <div><strong>Client:</strong> client1 / password</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

export default Login