import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import axios from 'axios'
import { Plus, Search, Edit, Trash2, Bug, Eye } from 'lucide-react'
import toast from 'react-hot-toast'

const Defects = () => {
  const navigate = useNavigate()
  const [defects, setDefects] = useState([])
  const [clients, setClients] = useState([])
  const [applications, setApplications] = useState([])
  const [testPlans, setTestPlans] = useState([])
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadingUsers, setLoadingUsers] = useState(false)
  const [searchTerm, setSearchTerm] = useState('')
  const [showModal, setShowModal] = useState(false)
  const [showViewModal, setShowViewModal] = useState(false)
  const [editingDefect, setEditingDefect] = useState(null)
  const [viewingDefect, setViewingDefect] = useState(null)
  const [formData, setFormData] = useState({
    clientId: '',
    applicationId: '',
    testPlanId: '',
    description: '',
    testingProcedure: '',
    assignedToId: '',
    status: 'NEW',
    pocFile: null
  })

  useEffect(() => {
    fetchDefects()
    fetchClients()
    fetchApplications()
    fetchTestPlans()
    fetchUsers()
  }, [])

  const fetchUsers = async () => {
    setLoadingUsers(true)
    try {
      const response = await axios.get('/api/users')
      setUsers(response.data)
    } catch (error) {
      console.error('Failed to fetch users:', error)
      setUsers([])
    } finally {
      setLoadingUsers(false)
    }
  }

  const fetchDefects = async () => {
    try {
      const response = await axios.get('/api/defects')
      setDefects(response.data)
    } catch (error) {
      console.error('Failed to fetch defects:', error)
      toast.error('Failed to fetch defects')
      setDefects([])
    } finally {
      setLoading(false)
    }
  }

  const fetchClients = async () => {
    try {
      const response = await axios.get('/api/clients')
      setClients(response.data)
    } catch (error) {
      console.error('Failed to fetch clients:', error)
      setClients([])
    }
  }

  const fetchApplications = async () => {
    try {
      const response = await axios.get('/api/applications')
      setApplications(response.data)
    } catch (error) {
      console.error('Failed to fetch applications:', error)
      setApplications([])
    }
  }

  const fetchTestPlans = async () => {
    try {
      const response = await axios.get('/api/test-plans')
      setTestPlans(response.data)
    } catch (error) {
      console.error('Failed to fetch test plans:', error)
      setTestPlans([])
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      if (editingDefect) {
        await axios.put(`/api/defects/${editingDefect.id}`, formData)
        toast.success('Defect updated successfully')
      } else {
        // Use FormData for file upload
        const formDataToSend = new FormData()
        formDataToSend.append('clientId', formData.clientId)
        formDataToSend.append('applicationId', formData.applicationId)
        formDataToSend.append('testPlanId', formData.testPlanId)
        formDataToSend.append('description', formData.description)
        formDataToSend.append('testingProcedure', formData.testingProcedure || '')
        if (formData.assignedToId) {
          formDataToSend.append('assignedToId', formData.assignedToId)
        }
        formDataToSend.append('status', formData.status)
        if (formData.pocFile) {
          formDataToSend.append('pocFile', formData.pocFile)
        }

        await axios.post('/api/defects/upload', formDataToSend, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        })
        toast.success('Defect created successfully')
      }
      fetchDefects()
      setShowModal(false)
      resetForm()
    } catch (error) {
      toast.error('Failed to save defect')
    }
  }

  const handleEdit = (defect) => {
    setEditingDefect(defect)
    setFormData({
      clientId: defect.client.id,
      applicationId: defect.application.id,
      testPlanId: defect.testPlan.id,
      description: defect.description,
      testingProcedure: defect.testingProcedure || '',
      assignedToId: defect.assignedTo?.id || '',
      status: defect.status
    })
    setShowModal(true)
  }

  const handleView = async (defect) => {
    try {
      const response = await axios.get(`/api/defects/${defect.id}/history`)
      setViewingDefect({ ...defect, history: response.data })
      setShowViewModal(true)
    } catch (error) {
      toast.error('Failed to fetch defect history')
    }
  }

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this defect?')) {
      try {
        await axios.delete(`/api/defects/${id}`)
        toast.success('Defect deleted successfully')
        fetchDefects()
      } catch (error) {
        toast.error('Failed to delete defect')
      }
    }
  }

  const resetForm = () => {
    setFormData({
      clientId: '',
      applicationId: '',
      testPlanId: '',
      description: '',
      testingProcedure: '',
      assignedToId: '',
      status: 'NEW',
      pocFile: null
    })
    setEditingDefect(null)
  }

  const getClientName = (clientId) => {
    const client = clients.find(c => c.id === clientId)
    return client ? client.clientName : 'Unknown'
  }

  const getApplicationName = (applicationId) => {
    const app = applications.find(a => a.id === applicationId)
    return app ? app.applicationName : 'Unknown'
  }

  const getTestPlanName = (testPlanId) => {
    const plan = testPlans.find(t => t.id === testPlanId)
    return plan ? plan.vulnerabilityName : 'Unknown'
  }

  const getSeverityColor = (severity) => {
    switch (severity) {
      case 'CRITICAL': return 'severity-critical'
      case 'HIGH': return 'severity-high'
      case 'MEDIUM': return 'severity-medium'
      case 'LOW': return 'severity-low'
      case 'INFO': return 'severity-info'
      default: return 'severity-info'
    }
  }

  const getStatusColor = (status) => {
    switch (status) {
      case 'NEW': return 'status-new'
      case 'OPEN': return 'status-open'
      case 'IN_PROGRESS': return 'status-in-progress'
      case 'RETEST': return 'status-retest'
      case 'CLOSED': return 'status-closed'
      default: return 'status-new'
    }
  }

  const filteredDefects = defects.filter(defect =>
    defect.defectId.toLowerCase().includes(searchTerm.toLowerCase()) ||
    defect.description.toLowerCase().includes(searchTerm.toLowerCase()) ||
    getClientName(defect.client.id).toLowerCase().includes(searchTerm.toLowerCase())
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
          <h1 className="text-2xl font-bold text-white">Defects</h1>
        </div>
        <button
          onClick={() => setShowModal(true)}
          className="btn-primary flex items-center"
        >
          <Plus className="w-4 h-4 mr-2" />
          Add New Defect
        </button>
      </div>

      {/* Search */}
      <div className="flex items-center space-x-4">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
          <input
            type="text"
            placeholder="Search defects..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="input-field pl-10 w-full"
          />
        </div>
      </div>

      {/* Defects Table */}
      <div className="card">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-dark-700">
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-300">Defect ID</th>
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-300">Client</th>
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-300">Application</th>
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-300">Vulnerability</th>
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-300">Severity</th>
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-300">Status</th>
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-300">Assigned To</th>
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-300">Date</th>
                <th className="text-right py-3 px-4 text-sm font-medium text-gray-300">Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredDefects.map((defect) => (
                <tr key={defect.id} className="border-b border-dark-700 hover:bg-dark-700/50">
                  <td className="py-3 px-4 text-sm text-white font-mono flex items-center">
                    <Bug className="w-4 h-4 mr-2 text-red-400" />
                    {defect.defectId}
                  </td>
                  <td className="py-3 px-4 text-sm text-gray-300">
                    {getClientName(defect.client.id)}
                  </td>
                  <td className="py-3 px-4 text-sm text-gray-300">
                    {getApplicationName(defect.application.id)}
                  </td>
                  <td className="py-3 px-4 text-sm text-gray-300">
                    {getTestPlanName(defect.testPlan.id)}
                  </td>
                  <td className="py-3 px-4 text-sm">
                    <span className={`status-badge ${getSeverityColor(defect.severity)}`}>
                      {defect.severity}
                    </span>
                  </td>
                  <td className="py-3 px-4 text-sm">
                    <span className={`status-badge ${getStatusColor(defect.status)}`}>
                      {defect.status.replace('_', ' ')}
                    </span>
                  </td>
                  <td className="py-3 px-4 text-sm text-gray-300">
                    {defect.assignedTo?.username || 'Unassigned'}
                  </td>
                  <td className="py-3 px-4 text-sm text-gray-300">
                    {new Date(defect.createdAt).toLocaleDateString()}
                  </td>
                  <td className="py-3 px-4 text-right">
                    <div className="flex items-center justify-end space-x-2">
                      <button
                        onClick={() => handleView(defect)}
                        className="text-green-400 hover:text-green-300 p-1"
                        title="View Details"
                      >
                        <Eye className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => handleEdit(defect)}
                        className="text-blue-400 hover:text-blue-300 p-1"
                      >
                        <Edit className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => handleDelete(defect.id)}
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

      {/* Add/Edit Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-dark-800 rounded-xl p-6 w-full max-w-2xl mx-4 max-h-[90vh] overflow-y-auto">
            <h2 className="text-xl font-bold text-white mb-4">
              {editingDefect ? 'Edit Defect' : 'Add New Defect'}
            </h2>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-1">Client</label>
                  <select
                    value={formData.clientId}
                    onChange={(e) => setFormData({...formData, clientId: parseInt(e.target.value), applicationId: ''})}
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
                  <label className="block text-sm font-medium text-gray-300 mb-1">Application</label>
                  <select
                    value={formData.applicationId}
                    onChange={(e) => setFormData({...formData, applicationId: parseInt(e.target.value)})}
                    className="input-field w-full"
                    required
                    disabled={!formData.clientId}
                  >
                    <option value="">Select an application</option>
                    {applications
                      .filter(app => app.client.id === parseInt(formData.clientId))
                      .map(app => (
                        <option key={app.id} value={app.id}>
                          {app.applicationName}
                        </option>
                      ))}
                  </select>
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">Test Plan (Vulnerability)</label>
                <select
                  value={formData.testPlanId}
                  onChange={(e) => setFormData({...formData, testPlanId: parseInt(e.target.value)})}
                  className="input-field w-full"
                  required
                >
                  <option value="">Select a test plan</option>
                  {testPlans.map(plan => (
                    <option key={plan.id} value={plan.id}>
                      {plan.testCaseId} - {plan.vulnerabilityName} ({plan.severity})
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">Description</label>
                <textarea
                  value={formData.description}
                  onChange={(e) => setFormData({...formData, description: e.target.value})}
                  className="input-field w-full h-24 resize-none"
                  placeholder="Detailed description of the defect"
                  required
                  rows={3}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">Testing Procedure</label>
                <textarea
                  value={formData.testingProcedure}
                  onChange={(e) => setFormData({...formData, testingProcedure: e.target.value})}
                  className="input-field w-full h-24 resize-none"
                  placeholder="Steps to reproduce the issue"
                  rows={3}
                />
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-1">Assigned To</label>
                  <select
                    value={formData.assignedToId}
                    onChange={(e) => setFormData({...formData, assignedToId: e.target.value ? parseInt(e.target.value) : ''})}
                    className="input-field w-full"
                    disabled={loadingUsers}
                  >
                    <option value="">Unassigned</option>
                    {users.filter(user => user.role === 'TESTER' || user.role === 'ADMIN').map(user => (
                      <option key={user.id} value={user.id}>
                        {user.firstName} {user.lastName} ({user.username})
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-1">Status</label>
                  <select
                    value={formData.status}
                    onChange={(e) => setFormData({...formData, status: e.target.value})}
                    className="input-field w-full"
                  >
                    <option value="NEW">New</option>
                    <option value="OPEN">Open</option>
                    <option value="IN_PROGRESS">In Progress</option>
                    <option value="RETEST">Retest</option>
                    <option value="CLOSED">Closed</option>
                  </select>
                </div>
              </div>
              {!editingDefect && (
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-1">Proof of Concept File (Optional)</label>
                  <input
                    type="file"
                    accept=".pdf,.jpg,.jpeg,.png,.gif,.mp4,.avi,.mov"
                    onChange={(e) => setFormData({...formData, pocFile: e.target.files[0]})}
                    className="input-field w-full"
                  />
                  <p className="text-xs text-gray-400 mt-1">
                    Supported formats: PDF, Images (JPG, PNG, GIF), Videos (MP4, AVI, MOV)
                  </p>
                </div>
              )}
              <div className="flex space-x-3 pt-4">
                <button type="submit" className="btn-primary flex-1">
                  {editingDefect ? 'Update' : 'Create'}
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

      {/* View Modal */}
      {showViewModal && viewingDefect && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-dark-800 rounded-xl p-6 w-full max-w-4xl mx-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-xl font-bold text-white">Defect Details</h2>
              <button
                onClick={() => setShowViewModal(false)}
                className="text-gray-400 hover:text-white"
              >
                ✕
              </button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-400">Defect ID</label>
                  <p className="text-white font-mono">{viewingDefect.defectId}</p>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-400">Client</label>
                  <p className="text-white">{getClientName(viewingDefect.client.id)}</p>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-400">Application</label>
                  <p className="text-white">{getApplicationName(viewingDefect.application.id)}</p>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-400">Vulnerability</label>
                  <p className="text-white">{getTestPlanName(viewingDefect.testPlan.id)}</p>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-400">Severity</label>
                  <span className={`status-badge ${getSeverityColor(viewingDefect.severity)}`}>
                    {viewingDefect.severity}
                  </span>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-400">Status</label>
                  <span className={`status-badge ${getStatusColor(viewingDefect.status)}`}>
                    {viewingDefect.status.replace('_', ' ')}
                  </span>
                </div>
              </div>

              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-400">Assigned To</label>
                  <p className="text-white">{viewingDefect.assignedTo?.username || 'Unassigned'}</p>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-400">Created By</label>
                  <p className="text-white">{viewingDefect.createdBy?.username || 'System'}</p>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-400">Created Date</label>
                  <p className="text-white">{new Date(viewingDefect.createdAt).toLocaleString()}</p>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-400">Description</label>
                  <p className="text-white">{viewingDefect.description}</p>
                </div>
                {viewingDefect.testingProcedure && (
                  <div>
                    <label className="block text-sm font-medium text-gray-400">Testing Procedure</label>
                    <p className="text-white">{viewingDefect.testingProcedure}</p>
                  </div>
                )}
              </div>
            </div>

            {/* History Timeline */}
            {viewingDefect.history && viewingDefect.history.length > 0 && (
              <div className="mt-8">
                <h3 className="text-lg font-semibold text-white mb-4">Status History</h3>
                <div className="space-y-3">
                  {viewingDefect.history.map((entry, index) => (
                    <div key={index} className="flex items-start space-x-3 p-3 bg-dark-700 rounded-lg">
                      <div className="w-2 h-2 bg-primary-500 rounded-full mt-2"></div>
                      <div className="flex-1">
                        <p className="text-white">
                          Status changed from <span className="font-medium">{entry.oldStatus || 'None'}</span> to{' '}
                          <span className="font-medium">{entry.newStatus}</span>
                        </p>
                        {entry.changeReason && (
                          <p className="text-gray-400 text-sm mt-1">{entry.changeReason}</p>
                        )}
                        <p className="text-gray-500 text-xs mt-1">
                          {new Date(entry.changedAt).toLocaleString()} by {entry.changedBy?.username || 'System'}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

export default Defects