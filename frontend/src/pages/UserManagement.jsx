import { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import {
  Users,
  UserPlus,
  Edit,
  Trash2,
  Search,
  ChevronRight,
  Home,
  AlertCircle,
  CheckCircle,
  Shield,
  UserCheck,
  UserX
} from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { usersAPI } from '../services/api'
import toast from 'react-hot-toast'

const UserManagement = () => {
  const navigate = useNavigate()
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedUser, setSelectedUser] = useState(null)
  const [showEditModal, setShowEditModal] = useState(false)
  const [showDeleteModal, setShowDeleteModal] = useState(false)
  const [editForm, setEditForm] = useState({
    firstName: '',
    lastName: '',
    email: '',
    role: '',
    isActive: true
  })

  useEffect(() => {
    fetchUsers()
  }, [])

  const fetchUsers = async () => {
    try {
      setLoading(true)
      const response = await usersAPI.getAll()
      setUsers(response.data)
    } catch (error) {
      console.error('Error fetching users:', error)
      toast.error('Failed to load users')
    } finally {
      setLoading(false)
    }
  }

  const filteredUsers = users.filter(user =>
    user.username.toLowerCase().includes(searchTerm.toLowerCase()) ||
    user.firstName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    user.lastName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    user.email.toLowerCase().includes(searchTerm.toLowerCase())
  )

  const handleEdit = (user) => {
    setSelectedUser(user)
    setEditForm({
      firstName: user.firstName || '',
      lastName: user.lastName || '',
      email: user.email,
      role: user.role,
      isActive: user.isActive
    })
    setShowEditModal(true)
  }

  const handleDelete = (user) => {
    setSelectedUser(user)
    setShowDeleteModal(true)
  }

  const confirmDelete = async () => {
    try {
      await usersAPI.delete(selectedUser.id)
      setUsers(users.filter(u => u.id !== selectedUser.id))
      toast.success('User deleted successfully')
      setShowDeleteModal(false)
      setSelectedUser(null)
    } catch (error) {
      console.error('Error deleting user:', error)
      toast.error('Failed to delete user')
    }
  }

  const handleUpdate = async (e) => {
    e.preventDefault()
    try {
      const updatedUser = await usersAPI.update(selectedUser.id, editForm)
      setUsers(users.map(u => u.id === selectedUser.id ? updatedUser.data : u))
      toast.success('User updated successfully')
      setShowEditModal(false)
      setSelectedUser(null)
    } catch (error) {
      console.error('Error updating user:', error)
      toast.error('Failed to update user')
    }
  }

  const getRoleBadge = (role) => {
    const roleConfig = {
      ADMIN: { color: 'bg-red-500/20 text-red-400 border-red-400/30', icon: Shield },
      TESTER: { color: 'bg-blue-500/20 text-blue-400 border-blue-400/30', icon: UserCheck },
      CLIENT: { color: 'bg-green-500/20 text-green-400 border-green-400/30', icon: UserX }
    }
    const config = roleConfig[role] || roleConfig.CLIENT
    const Icon = config.icon
    return (
      <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium border ${config.color}`}>
        <Icon className="w-3 h-3 mr-1" />
        {role}
      </span>
    )
  }

  const getStatusBadge = (isActive) => {
    return (
      <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium border ${
        isActive
          ? 'bg-green-500/20 text-green-400 border-green-400/30'
          : 'bg-red-500/20 text-red-400 border-red-400/30'
      }`}>
        <div className={`w-2 h-2 rounded-full mr-1 ${isActive ? 'bg-green-400' : 'bg-red-400'}`} />
        {isActive ? 'Active' : 'Inactive'}
      </span>
    )
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-dark-900 via-dark-800 to-dark-900 flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-2 border-cyan-400 border-t-transparent"></div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-dark-900 via-dark-800 to-dark-900">
      {/* Breadcrumb */}
      <div className="pt-8 pb-4">
        <div className="max-w-7xl mx-auto px-6">
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
            <span className="text-cyan-400">User Management</span>
          </motion.nav>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-6 pb-8">
        {/* Header */}
        <div className="mb-8">
          <motion.div
            initial={{ scale: 0.9 }}
            animate={{ scale: 1 }}
            transition={{ delay: 0.2 }}
            className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-gradient-to-br from-cyan-500/20 to-blue-500/20 border border-cyan-400/30 mb-4"
          >
            <Users className="w-8 h-8 text-cyan-400" />
          </motion.div>
          <h1 className="text-4xl font-bold bg-gradient-to-r from-cyan-400 via-blue-400 to-purple-400 bg-clip-text text-transparent mb-2">
            User Management
          </h1>
          <p className="text-gray-400 text-lg">
            Manage user accounts, roles, and permissions.
          </p>
        </div>

        {/* Search and Actions */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
          className="mb-6 flex flex-col sm:flex-row gap-4 items-center justify-between"
        >
          <div className="relative flex-1 max-w-md">
            <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
              <Search className="w-5 h-5 text-gray-400" />
            </div>
            <input
              type="text"
              placeholder="Search users..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-12 pr-4 py-3 bg-dark-700/50 border border-dark-600/50 rounded-xl text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-cyan-400/50 focus:border-cyan-400 transition-all duration-200"
            />
          </div>
          <div className="flex gap-3">
            <button
              onClick={() => navigate('/user-registration')}
              className="flex items-center px-6 py-3 bg-gradient-to-r from-cyan-500 to-blue-500 text-white rounded-xl font-semibold hover:from-cyan-600 hover:to-blue-600 focus:outline-none focus:ring-2 focus:ring-cyan-400/50 transition-all duration-200 shadow-lg shadow-cyan-500/25"
            >
              <UserPlus className="w-5 h-5 mr-2" />
              Register New User
            </button>
            <button
              onClick={() => navigate('/dashboard')}
              className="flex items-center px-6 py-3 bg-dark-700/50 border border-dark-600/50 text-gray-300 rounded-xl font-medium hover:bg-dark-700/70 hover:text-white focus:outline-none focus:ring-2 focus:ring-gray-400/50 transition-all duration-200"
            >
              Back to Dashboard
            </button>
          </div>
        </motion.div>

        {/* Users Table */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
          className="bg-gradient-to-br from-dark-800/50 to-dark-900/50 border border-dark-700/50 rounded-2xl backdrop-blur-xl shadow-2xl overflow-hidden"
        >
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-dark-700/50">
                <tr>
                  <th className="px-6 py-4 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">User</th>
                  <th className="px-6 py-4 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">Role</th>
                  <th className="px-6 py-4 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">Status</th>
                  <th className="px-6 py-4 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">Created</th>
                  <th className="px-6 py-4 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-dark-700/50">
                {filteredUsers.map((user) => (
                  <tr key={user.id} className="hover:bg-dark-700/30 transition-colors">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center">
                        <div className="flex-shrink-0 w-10 h-10">
                          <div className="w-10 h-10 rounded-full bg-gradient-to-br from-cyan-500/20 to-blue-500/20 flex items-center justify-center border border-cyan-400/30">
                            <span className="text-cyan-400 font-semibold text-sm">
                              {(user.firstName || user.username).charAt(0).toUpperCase()}
                            </span>
                          </div>
                        </div>
                        <div className="ml-4">
                          <div className="text-sm font-medium text-white">
                            {user.firstName} {user.lastName}
                          </div>
                          <div className="text-sm text-gray-400">{user.username}</div>
                          <div className="text-sm text-gray-500">{user.email}</div>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      {getRoleBadge(user.role)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      {getStatusBadge(user.isActive)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-400">
                      {new Date(user.createdAt).toLocaleDateString()}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                      <div className="flex items-center space-x-2">
                        <button
                          onClick={() => handleEdit(user)}
                          className="text-cyan-400 hover:text-cyan-300 transition-colors p-1"
                          title="Edit User"
                        >
                          <Edit className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => handleDelete(user)}
                          className="text-red-400 hover:text-red-300 transition-colors p-1"
                          title="Delete User"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {filteredUsers.length === 0 && (
            <div className="text-center py-12">
              <Users className="w-12 h-12 text-gray-600 mx-auto mb-4" />
              <p className="text-gray-400">No users found</p>
            </div>
          )}
        </motion.div>
      </div>

      {/* Edit User Modal */}
      {showEditModal && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="bg-gradient-to-br from-dark-800 to-dark-900 border border-dark-700 rounded-2xl p-6 w-full max-w-md"
          >
            <h3 className="text-xl font-bold text-white mb-4">Edit User</h3>
            <form onSubmit={handleUpdate} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-2">First Name</label>
                <input
                  type="text"
                  value={editForm.firstName}
                  onChange={(e) => setEditForm({...editForm, firstName: e.target.value})}
                  className="w-full px-4 py-3 bg-dark-700/50 border border-dark-600/50 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-cyan-400/50"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-2">Last Name</label>
                <input
                  type="text"
                  value={editForm.lastName}
                  onChange={(e) => setEditForm({...editForm, lastName: e.target.value})}
                  className="w-full px-4 py-3 bg-dark-700/50 border border-dark-600/50 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-cyan-400/50"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-2">Email</label>
                <input
                  type="email"
                  value={editForm.email}
                  onChange={(e) => setEditForm({...editForm, email: e.target.value})}
                  className="w-full px-4 py-3 bg-dark-700/50 border border-dark-600/50 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-cyan-400/50"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-2">Role</label>
                <select
                  value={editForm.role}
                  onChange={(e) => setEditForm({...editForm, role: e.target.value})}
                  className="w-full px-4 py-3 bg-dark-700/50 border border-dark-600/50 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-cyan-400/50"
                >
                  <option value="CLIENT">Client</option>
                  <option value="TESTER">Tester</option>
                  <option value="ADMIN">Admin</option>
                </select>
              </div>
              <div className="flex items-center">
                <input
                  type="checkbox"
                  id="isActive"
                  checked={editForm.isActive}
                  onChange={(e) => setEditForm({...editForm, isActive: e.target.checked})}
                  className="w-4 h-4 text-cyan-600 bg-dark-700 border-dark-600 rounded focus:ring-cyan-500"
                />
                <label htmlFor="isActive" className="ml-2 text-sm text-gray-300">Active User</label>
              </div>
              <div className="flex gap-3 pt-4">
                <button
                  type="submit"
                  className="flex-1 px-4 py-3 bg-gradient-to-r from-cyan-500 to-blue-500 text-white rounded-xl font-semibold hover:from-cyan-600 hover:to-blue-600"
                >
                  Update User
                </button>
                <button
                  type="button"
                  onClick={() => setShowEditModal(false)}
                  className="px-4 py-3 bg-dark-700/50 border border-dark-600/50 text-gray-300 rounded-xl hover:bg-dark-700/70"
                >
                  Cancel
                </button>
              </div>
            </form>
          </motion.div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {showDeleteModal && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="bg-gradient-to-br from-dark-800 to-dark-900 border border-dark-700 rounded-2xl p-6 w-full max-w-md"
          >
            <div className="flex items-center mb-4">
              <AlertCircle className="w-6 h-6 text-red-400 mr-3" />
              <h3 className="text-xl font-bold text-white">Delete User</h3>
            </div>
            <p className="text-gray-300 mb-6">
              Are you sure you want to delete <strong>{selectedUser?.username}</strong>?
              This action cannot be undone.
            </p>
            <div className="flex gap-3">
              <button
                onClick={confirmDelete}
                className="flex-1 px-4 py-3 bg-gradient-to-r from-red-500 to-red-600 text-white rounded-xl font-semibold hover:from-red-600 hover:to-red-700"
              >
                Delete User
              </button>
              <button
                onClick={() => setShowDeleteModal(false)}
                className="px-4 py-3 bg-dark-700/50 border border-dark-600/50 text-gray-300 rounded-xl hover:bg-dark-700/70"
              >
                Cancel
              </button>
            </div>
          </motion.div>
        </div>
      )}
    </div>
  )
}

export default UserManagement