import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { applicationsAPI, clientsAPI } from '../services/api'
import { Plus, Search, Edit, Trash2, Building } from 'lucide-react'
import toast from 'react-hot-toast'

const Applications = () => {
  const navigate = useNavigate()
  const [applications, setApplications] = useState([])
  const [clients, setClients] = useState([])
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState('')
  const [showModal, setShowModal] = useState(false)
  const [editingApplication, setEditingApplication] = useState(null)
  const [formData, setFormData] = useState({
    applicationName: '',
    client: null,
    environment: 'PRODUCTION',
    technologyStack: ''
  })

  useEffect(() => {
    fetchApplications()
    fetchClients()
  }, [])

  const fetchApplications = async () => {
    try {
      const response = await applicationsAPI.getAll()
      setApplications(response.data)
    } catch (error) {
      console.error('Failed to fetch applications:', error)
      toast.error('Failed to fetch applications')
      setApplications([])
    } finally {
      setLoading(false)
    }
  }

  const fetchClients = async () => {
    try {
      const response = await clientsAPI.getAll()
      setClients(response.data)
    } catch (error) {
      console.error('Failed to fetch clients:', error)
      setClients([])
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      if (editingApplication) {
        await applicationsAPI.update(editingApplication.id, formData)
        toast.success('Application updated successfully')
      } else {
        await applicationsAPI.create(formData)
        toast.success('Application created successfully')
      }
      fetchApplications()
      setShowModal(false)
      resetForm()
    } catch (error) {
      toast.error('Failed to save application')
    }
  }

  const handleEdit = (application) => {
    setEditingApplication(application)
    setFormData({
      applicationName: application.applicationName,
      client: application.client,
      environment: application.environment,
      technologyStack: application.technologyStack || ''
    })
    setShowModal(true)
  }

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this application?')) {
      try {
        await applicationsAPI.delete(id)
        toast.success('Application deleted successfully')
        fetchApplications()
      } catch (error) {
        toast.error('Failed to delete application')
      }
    }
  }

  const resetForm = () => {
    setFormData({
      applicationName: '',
      client: null,
      environment: 'PRODUCTION',
      technologyStack: ''
    })
    setEditingApplication(null)
  }

  const getClientName = (clientId) => {
    const client = clients.find(c => c.id === clientId)
    return client ? client.clientName : 'Unknown'
  }

  const filteredApplications = applications.filter(app =>
    app.applicationName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    getClientName(app.client.id).toLowerCase().includes(searchTerm.toLowerCase())
  )

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-500"></div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <button
            onClick={() => navigate('/dashboard')}
            className="btn-secondary flex items-center"
          >
            ← Back to Dashboard
          </button>
          <h1 className="text-2xl font-bold text-white">Applications</h1>
        </div>
        <button
          onClick={() => setShowModal(true)}
          className="btn-primary flex items-center"
        >
          <Plus className="w-4 h-4 mr-2" />
          Add New Application
        </button>
      </div>

      {/* Search */}
      <div className="flex items-center space-x-4">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
          <input
            type="text"
            placeholder="Search applications..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="input-field pl-10 w-full"
          />
        </div>
      </div>

      {/* Applications Table */}
      <div className="card">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-dark-700">
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-300">Application Name</th>
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-300">Client</th>
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-300">Environment</th>
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-300">Technology Stack</th>
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-300">Added Date</th>
                <th className="text-right py-3 px-4 text-sm font-medium text-gray-300">Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredApplications.map((app) => (
                <tr key={app.id} className="border-b border-dark-700 hover:bg-dark-700/50">
                  <td className="py-3 px-4 text-sm text-white flex items-center">
                    <Building className="w-4 h-4 mr-2 text-gray-400" />
                    {app.applicationName}
                  </td>
                  <td className="py-3 px-4 text-sm text-gray-300">
                    {getClientName(app.client.id)}
                  </td>
                  <td className="py-3 px-4 text-sm text-gray-300">
                    <span className={`status-badge ${
                      app.environment === 'PRODUCTION' ? 'bg-green-500/20 text-green-400' :
                      app.environment === 'STAGING' ? 'bg-yellow-500/20 text-yellow-400' :
                      app.environment === 'DEVELOPMENT' ? 'bg-blue-500/20 text-blue-400' :
                      'bg-gray-500/20 text-gray-400'
                    }`}>
                      {app.environment}
                    </span>
                  </td>
                  <td className="py-3 px-4 text-sm text-gray-300 max-w-xs truncate">
                    {app.technologyStack || '-'}
                  </td>
                  <td className="py-3 px-4 text-sm text-gray-300">
                    {new Date(app.createdAt).toLocaleDateString()}
                  </td>
                  <td className="py-3 px-4 text-right">
                    <div className="flex items-center justify-end space-x-2">
                      <button
                        onClick={() => handleEdit(app)}
                        className="text-blue-400 hover:text-blue-300 p-1"
                      >
                        <Edit className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => handleDelete(app.id)}
                        className="text-red-400 hover:text-red-300 p-1"
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
      </div>

      {/* Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-dark-800 rounded-xl p-6 w-full max-w-md mx-4">
            <h2 className="text-xl font-bold text-white mb-4">
              {editingApplication ? 'Edit Application' : 'Add New Application'}
            </h2>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">Application Name</label>
                <input
                  type="text"
                  value={formData.applicationName}
                  onChange={(e) => setFormData({...formData, applicationName: e.target.value})}
                  className="input-field w-full"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">Client</label>
                <select
                  value={formData.client ? formData.client.id : ''}
                  onChange={(e) => {
                    const selectedClient = clients.find(c => c.id === parseInt(e.target.value));
                    setFormData({...formData, client: selectedClient || null});
                  }}
                  className="input-field w-full"
                  required
                >
                  <option value="">Select a client</option>
                  {clients.map(client => (
                    <option key={client.id} value={client.id}>
                      {client.clientName} - {client.companyName}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">Environment</label>
                <select
                  value={formData.environment}
                  onChange={(e) => setFormData({...formData, environment: e.target.value})}
                  className="input-field w-full"
                >
                  <option value="PRODUCTION">Production</option>
                  <option value="STAGING">Staging</option>
                  <option value="DEVELOPMENT">Development</option>
                  <option value="TESTING">Testing</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">Technology Stack</label>
                <input
                  type="text"
                  value={formData.technologyStack}
                  onChange={(e) => setFormData({...formData, technologyStack: e.target.value})}
                  className="input-field w-full"
                  placeholder="e.g., React, Node.js, MySQL"
                />
              </div>
              <div className="flex space-x-3 pt-4">
                <button type="submit" className="btn-primary flex-1">
                  {editingApplication ? 'Update' : 'Create'}
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setShowModal(false)
                    resetForm()
                  }}
                  className="btn-secondary flex-1"
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}

export default Applications