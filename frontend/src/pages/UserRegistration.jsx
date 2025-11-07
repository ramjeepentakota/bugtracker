import { useState } from 'react'
import { motion } from 'framer-motion'
import {
  UserPlus,
  User,
  Mail,
  Lock,
  Eye,
  EyeOff,
  ChevronRight,
  Home,
  AlertCircle,
  CheckCircle
} from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { usersAPI } from '../services/api'

const UserRegistration = () => {
  const navigate = useNavigate()
  const [formData, setFormData] = useState({
    firstName: '',
    surname: '',
    username: '',
    role: '',
    password: '',
    confirmPassword: ''
  })
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [errors, setErrors] = useState({})
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [success, setSuccess] = useState(false)

  const roles = [
    { value: 'Tester', label: 'Tester' },
    { value: 'PM', label: 'Project Manager (PM)' },
    { value: 'DM', label: 'Delivery Manager (DM)' },
    { value: 'Admin', label: 'Admin' }
  ]

  const handleInputChange = (e) => {
    const { name, value } = e.target
    setFormData(prev => ({
      ...prev,
      [name]: value
    }))
    // Clear error when user starts typing
    if (errors[name]) {
      setErrors(prev => ({
        ...prev,
        [name]: ''
      }))
    }
  }

  const validateForm = () => {
    const newErrors = {}

    if (!formData.firstName.trim()) {
      newErrors.firstName = 'First name is required'
    }

    if (!formData.surname.trim()) {
      newErrors.surname = 'Surname is required'
    }

    if (!formData.username.trim()) {
      newErrors.username = 'Username is required'
    } else if (formData.username.length < 3) {
      newErrors.username = 'Username must be at least 3 characters'
    } else if (!/^[a-zA-Z0-9._]+$/.test(formData.username)) {
      newErrors.username = 'Username can only contain letters, numbers, dots, and underscores'
    }

    if (!formData.role) {
      newErrors.role = 'Please select a role'
    }

    if (!formData.password) {
      newErrors.password = 'Password is required'
    } else if (formData.password.length < 8) {
      newErrors.password = 'Password must be at least 8 characters'
    }

    if (!formData.confirmPassword) {
      newErrors.confirmPassword = 'Please confirm your password'
    } else if (formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = 'Passwords do not match'
    }

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleSubmit = async (e) => {
    e.preventDefault()

    if (!validateForm()) {
      return
    }

    setIsSubmitting(true)

    try {
      // Use provided username or generate one if empty
      const username = formData.username.trim() || `${formData.firstName.toLowerCase()}.${formData.surname.toLowerCase()}`
      const email = `${username}@company.com`

      const userData = {
        firstName: formData.firstName,
        surname: formData.surname,
        username: username,
        email: email,
        password: formData.password,
        role: formData.role
      }

      // Make API call to register the user
      const response = await usersAPI.register(userData)

      setSuccess(true)

      // Reset form after success
      setTimeout(() => {
        setFormData({
          firstName: '',
          surname: '',
          username: '',
          role: '',
          password: '',
          confirmPassword: ''
        })
        setSuccess(false)
        navigate('/dashboard')
      }, 3000)

    } catch (error) {
      console.error('Registration error:', error)
      setErrors({ submit: error.response?.data?.message || 'Registration failed. Please try again.' })
    } finally {
      setIsSubmitting(false)
    }
  }

  const getPasswordStrength = (password) => {
    if (!password) return 0
    let strength = 0
    if (password.length >= 8) strength++
    if (/[A-Z]/.test(password)) strength++
    if (/[a-z]/.test(password)) strength++
    if (/[0-9]/.test(password)) strength++
    if (/[^A-Za-z0-9]/.test(password)) strength++
    return strength
  }

  const passwordStrength = getPasswordStrength(formData.password)

  return (
    <div className="min-h-screen bg-gradient-to-br from-dark-900 via-dark-800 to-dark-900">
      {/* Breadcrumb */}
      <div className="pt-8 pb-4">
        <div className="max-w-4xl mx-auto px-6">
          <motion.nav
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            className="flex items-center space-x-2 text-sm text-gray-400"
          >
            <button
              onClick={() => navigate('/dashboard')}
              className="flex items-center hover:text-cyan-400 transition-colors"
            >
              <Home className="w-4 h-4 mr-1" />
              Dashboard
            </button>
            <ChevronRight className="w-4 h-4" />
            <span className="text-cyan-400">User Enroll</span>
            <ChevronRight className="w-4 h-4" />
            <span>Register New User</span>
          </motion.nav>
        </div>
      </div>

      <div className="flex items-center justify-center min-h-[calc(100vh-120px)] px-6">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          className="w-full max-w-2xl"
        >
          {/* Header */}
          <div className="text-center mb-8">
            <motion.div
              initial={{ scale: 0.9 }}
              animate={{ scale: 1 }}
              transition={{ delay: 0.2 }}
              className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-gradient-to-br from-cyan-500/20 to-blue-500/20 border border-cyan-400/30 mb-4"
            >
              <UserPlus className="w-8 h-8 text-cyan-400" />
            </motion.div>
            <h1 className="text-4xl font-bold bg-gradient-to-r from-cyan-400 via-blue-400 to-purple-400 bg-clip-text text-transparent mb-2">
              User Enrollment
            </h1>
            <p className="text-gray-400 text-lg">
              Register a new user and assign a role for dashboard access.
            </p>
          </div>

          {/* Success Message */}
          {success && (
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              className="mb-6 p-4 rounded-xl bg-gradient-to-r from-green-500/20 to-emerald-500/20 border border-green-400/30 backdrop-blur-xl"
            >
              <div className="flex items-center">
                <CheckCircle className="w-5 h-5 text-green-400 mr-3" />
                <span className="text-green-300 font-medium">User registered successfully!</span>
              </div>
            </motion.div>
          )}

          {/* Form Card */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3 }}
            className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-dark-800/50 to-dark-900/50 border border-dark-700/50 backdrop-blur-xl shadow-2xl"
          >
            {/* Background gradient overlay */}
            <div className="absolute inset-0 bg-gradient-to-br from-cyan-500/5 via-transparent to-purple-500/5"></div>

            <div className="relative p-8">
              <form onSubmit={handleSubmit} className="space-y-6">
                {/* Name Fields */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  {/* First Name */}
                  <div>
                    <label className="block text-sm font-medium text-gray-300 mb-2">
                      First Name <span className="text-red-400">*</span>
                    </label>
                    <div className="relative">
                      <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                        <User className="w-5 h-5 text-gray-400" />
                      </div>
                      <input
                        type="text"
                        name="firstName"
                        value={formData.firstName}
                        onChange={handleInputChange}
                        className={`w-full pl-12 pr-4 py-3 bg-dark-700/50 border rounded-xl text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-cyan-400/50 focus:border-cyan-400 transition-all duration-200 ${
                          errors.firstName ? 'border-red-400/50' : 'border-dark-600/50'
                        }`}
                        placeholder="Enter first name"
                      />
                    </div>
                    {errors.firstName && (
                      <p className="mt-1 text-sm text-red-400 flex items-center">
                        <AlertCircle className="w-4 h-4 mr-1" />
                        {errors.firstName}
                      </p>
                    )}
                  </div>

                  {/* Surname */}
                  <div>
                    <label className="block text-sm font-medium text-gray-300 mb-2">
                      Surname <span className="text-red-400">*</span>
                    </label>
                    <div className="relative">
                      <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                        <User className="w-5 h-5 text-gray-400" />
                      </div>
                      <input
                        type="text"
                        name="surname"
                        value={formData.surname}
                        onChange={handleInputChange}
                        className={`w-full pl-12 pr-4 py-3 bg-dark-700/50 border rounded-xl text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-cyan-400/50 focus:border-cyan-400 transition-all duration-200 ${
                          errors.surname ? 'border-red-400/50' : 'border-dark-600/50'
                        }`}
                        placeholder="Enter surname"
                      />
                    </div>
                    {errors.surname && (
                      <p className="mt-1 text-sm text-red-400 flex items-center">
                        <AlertCircle className="w-4 h-4 mr-1" />
                        {errors.surname}
                      </p>
                    )}
                  </div>
                </div>

                {/* Username */}
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2">
                    Username <span className="text-red-400">*</span>
                  </label>
                  <div className="relative">
                    <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                      <User className="w-5 h-5 text-gray-400" />
                    </div>
                    <input
                      type="text"
                      name="username"
                      value={formData.username}
                      onChange={handleInputChange}
                      className={`w-full pl-12 pr-4 py-3 bg-dark-700/50 border rounded-xl text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-cyan-400/50 focus:border-cyan-400 transition-all duration-200 ${
                        errors.username ? 'border-red-400/50' : 'border-dark-600/50'
                      }`}
                      placeholder="Enter username (e.g., john.doe)"
                    />
                  </div>
                  {errors.username && (
                    <p className="mt-1 text-sm text-red-400 flex items-center">
                      <AlertCircle className="w-4 h-4 mr-1" />
                      {errors.username}
                    </p>
                  )}
                  <p className="mt-1 text-xs text-gray-500">
                    Leave empty to auto-generate from first name and surname
                  </p>
                </div>

                {/* Role */}
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2">
                    Role <span className="text-red-400">*</span>
                  </label>
                  <div className="relative">
                    <select
                      name="role"
                      value={formData.role}
                      onChange={handleInputChange}
                      className={`w-full px-4 py-3 bg-dark-700/50 border rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-cyan-400/50 focus:border-cyan-400 transition-all duration-200 appearance-none ${
                        errors.role ? 'border-red-400/50' : 'border-dark-600/50'
                      }`}
                    >
                      <option value="">Select a role</option>
                      {roles.map(role => (
                        <option key={role.value} value={role.value} className="bg-dark-800">
                          {role.label}
                        </option>
                      ))}
                    </select>
                    <div className="absolute inset-y-0 right-0 pr-4 flex items-center pointer-events-none">
                      <ChevronRight className="w-5 h-5 text-gray-400 rotate-90" />
                    </div>
                  </div>
                  {errors.role && (
                    <p className="mt-1 text-sm text-red-400 flex items-center">
                      <AlertCircle className="w-4 h-4 mr-1" />
                      {errors.role}
                    </p>
                  )}
                </div>

                {/* Password Fields */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  {/* Password */}
                  <div>
                    <label className="block text-sm font-medium text-gray-300 mb-2">
                      Password <span className="text-red-400">*</span>
                    </label>
                    <div className="relative">
                      <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                        <Lock className="w-5 h-5 text-gray-400" />
                      </div>
                      <input
                        type={showPassword ? 'text' : 'password'}
                        name="password"
                        value={formData.password}
                        onChange={handleInputChange}
                        className={`w-full pl-12 pr-12 py-3 bg-dark-700/50 border rounded-xl text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-cyan-400/50 focus:border-cyan-400 transition-all duration-200 ${
                          errors.password ? 'border-red-400/50' : 'border-dark-600/50'
                        }`}
                        placeholder="Enter password"
                      />
                      <button
                        type="button"
                        onClick={() => setShowPassword(!showPassword)}
                        className="absolute inset-y-0 right-0 pr-4 flex items-center text-gray-400 hover:text-gray-300"
                      >
                        {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                      </button>
                    </div>
                    {/* Password Strength Indicator */}
                    {formData.password && (
                      <div className="mt-2">
                        <div className="flex space-x-1">
                          {[1, 2, 3, 4, 5].map(level => (
                            <div
                              key={level}
                              className={`h-1 flex-1 rounded-full transition-colors ${
                                level <= passwordStrength
                                  ? passwordStrength <= 2
                                    ? 'bg-red-400'
                                    : passwordStrength <= 3
                                    ? 'bg-yellow-400'
                                    : 'bg-green-400'
                                  : 'bg-dark-600'
                              }`}
                            />
                          ))}
                        </div>
                        <p className="text-xs text-gray-400 mt-1">
                          {passwordStrength <= 2 ? 'Weak' : passwordStrength <= 3 ? 'Medium' : 'Strong'} password
                        </p>
                      </div>
                    )}
                    {errors.password && (
                      <p className="mt-1 text-sm text-red-400 flex items-center">
                        <AlertCircle className="w-4 h-4 mr-1" />
                        {errors.password}
                      </p>
                    )}
                  </div>

                  {/* Confirm Password */}
                  <div>
                    <label className="block text-sm font-medium text-gray-300 mb-2">
                      Confirm Password <span className="text-red-400">*</span>
                    </label>
                    <div className="relative">
                      <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                        <Lock className="w-5 h-5 text-gray-400" />
                      </div>
                      <input
                        type={showConfirmPassword ? 'text' : 'password'}
                        name="confirmPassword"
                        value={formData.confirmPassword}
                        onChange={handleInputChange}
                        className={`w-full pl-12 pr-12 py-3 bg-dark-700/50 border rounded-xl text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-cyan-400/50 focus:border-cyan-400 transition-all duration-200 ${
                          errors.confirmPassword ? 'border-red-400/50' : 'border-dark-600/50'
                        }`}
                        placeholder="Confirm password"
                      />
                      <button
                        type="button"
                        onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                        className="absolute inset-y-0 right-0 pr-4 flex items-center text-gray-400 hover:text-gray-300"
                      >
                        {showConfirmPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                      </button>
                    </div>
                    {errors.confirmPassword && (
                      <p className="mt-1 text-sm text-red-400 flex items-center">
                        <AlertCircle className="w-4 h-4 mr-1" />
                        {errors.confirmPassword}
                      </p>
                    )}
                  </div>
                </div>

                {/* Submit Error */}
                {errors.submit && (
                  <div className="p-4 rounded-xl bg-red-500/20 border border-red-400/30">
                    <p className="text-red-300 text-sm flex items-center">
                      <AlertCircle className="w-4 h-4 mr-2" />
                      {errors.submit}
                    </p>
                  </div>
                )}

                {/* Buttons */}
                <div className="flex flex-col sm:flex-row gap-4 pt-4">
                  <motion.button
                    type="submit"
                    disabled={isSubmitting}
                    whileHover={{ scale: 1.02 }}
                    whileTap={{ scale: 0.98 }}
                    className="flex-1 px-8 py-4 bg-gradient-to-r from-cyan-500 to-blue-500 text-white rounded-xl font-semibold hover:from-cyan-600 hover:to-blue-600 focus:outline-none focus:ring-2 focus:ring-cyan-400/50 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 shadow-lg shadow-cyan-500/25"
                  >
                    {isSubmitting ? (
                      <div className="flex items-center justify-center">
                        <div className="animate-spin rounded-full h-5 w-5 border-2 border-white border-t-transparent mr-2"></div>
                        Registering...
                      </div>
                    ) : (
                      'Register User'
                    )}
                  </motion.button>

                  <motion.button
                    type="button"
                    onClick={() => navigate('/user-management')}
                    whileHover={{ scale: 1.02 }}
                    whileTap={{ scale: 0.98 }}
                    className="px-8 py-4 bg-gradient-to-r from-amber-500 to-orange-500 text-white rounded-xl font-medium hover:from-amber-600 hover:to-orange-600 focus:outline-none focus:ring-2 focus:ring-amber-400/50 transition-all duration-200 shadow-lg shadow-amber-500/25"
                  >
                    Manage Users
                  </motion.button>

                  <motion.button
                    type="button"
                    onClick={() => navigate('/dashboard')}
                    whileHover={{ scale: 1.02 }}
                    whileTap={{ scale: 0.98 }}
                    className="px-8 py-4 bg-dark-700/50 border border-dark-600/50 text-gray-300 rounded-xl font-medium hover:bg-dark-700/70 hover:text-white focus:outline-none focus:ring-2 focus:ring-gray-400/50 transition-all duration-200"
                  >
                    Back to Dashboard
                  </motion.button>
                </div>
              </form>
            </div>
          </motion.div>
        </motion.div>
      </div>
    </div>
  )
}

export default UserRegistration