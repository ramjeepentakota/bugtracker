import { createContext, useContext, useState, useEffect } from 'react'
import axios from 'axios'
import toast from 'react-hot-toast'

const AuthContext = createContext()

export const useAuth = () => {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const token = localStorage.getItem('token')
    if (token) {
      validateToken()
    } else {
      setLoading(false)
    }
  }, [])

  const validateToken = async () => {
    try {
      const token = localStorage.getItem('token')
      if (token) {
        // Decode JWT token to get user info
        const payload = JSON.parse(atob(token.split('.')[1]))
        setUser({
          id: payload.sub,
          username: payload.sub,
          role: payload.role || 'CLIENT'
        })
      } else {
        logout()
      }
    } catch (error) {
      logout()
    } finally {
      setLoading(false)
    }
  }

  const login = async (username, password) => {
    try {
      console.log('Frontend login attempt for user:', username)
      const response = await axios.post('/api/auth/login', {
        username,
        password
      })

      console.log('Login response status:', response.status)
      console.log('Login response data:', response.data)

      const { token, user: userData } = response.data
      localStorage.setItem('token', token)
      axios.defaults.headers.common['Authorization'] = `Bearer ${token}`
      setUser(userData)

      toast.success('Login successful!')
      return { success: true }
    } catch (error) {
      console.error('Login error:', error)
      console.error('Error response:', error.response)
      const message = error.response?.data?.error || 'Login failed'
      toast.error(message)
      return { success: false, error: message }
    }
  }

  const logout = () => {
    localStorage.removeItem('token')
    delete axios.defaults.headers.common['Authorization']
    setUser(null)
    toast.success('Logged out successfully')
  }

  const value = {
    user,
    login,
    logout,
    loading
  }

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  )
}