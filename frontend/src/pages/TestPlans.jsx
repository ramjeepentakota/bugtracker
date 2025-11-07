import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { testPlansAPI } from '../services/api'
import { Plus, Search, Edit, Trash2, FileText } from 'lucide-react'
import toast from 'react-hot-toast'

const TestPlans = () => {
  const navigate = useNavigate()
  const [testPlans, setTestPlans] = useState([])
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState('')
  const [showModal, setShowModal] = useState(false)
  const [editingTestPlan, setEditingTestPlan] = useState(null)
  const [formData, setFormData] = useState({
    vulnerabilityName: '',
    severity: 'MEDIUM',
    description: '',
    testProcedure: ''
  })

  useEffect(() => {
    fetchTestPlans()
  }, [])

  const fetchTestPlans = async () => {
    try {
      const response = await testPlansAPI.getAll()
      setTestPlans(response.data)
    } catch (error) {
      console.error('Failed to fetch test plans:', error)
      toast.error('Failed to fetch test plans')
      setTestPlans([])
    } finally {
      setLoading(false)
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      if (editingTestPlan) {
        const updatedTestPlan = await testPlansAPI.update(editingTestPlan.id, formData)
        setTestPlans(testPlans.map(plan =>
          plan.id === editingTestPlan.id ? updatedTestPlan.data : plan
        ))
        toast.success('Test plan updated successfully')
      } else {
        const newTestPlan = await testPlansAPI.create(formData)
        setTestPlans([...testPlans, newTestPlan.data])
        toast.success('Test plan created successfully')
      }
      setShowModal(false)
      resetForm()
    } catch (error) {
      toast.error('Failed to save test plan')
    }
  }

  const handleEdit = (testPlan) => {
    setEditingTestPlan(testPlan)
    setFormData({
      vulnerabilityName: testPlan.vulnerabilityName,
      severity: testPlan.severity,
      description: testPlan.description || '',
      testProcedure: testPlan.testProcedure
    })
    setShowModal(true)
  }

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this test plan?')) {
      try {
        await testPlansAPI.delete(id)
        toast.success('Test plan deleted successfully')
        fetchTestPlans()
      } catch (error) {
        toast.error('Failed to delete test plan')
      }
    }
  }

  const resetForm = () => {
    setFormData({
      vulnerabilityName: '',
      severity: 'MEDIUM',
      description: '',
      testProcedure: ''
    })
    setEditingTestPlan(null)
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

  const filteredTestPlans = testPlans.filter(plan =>
    plan.vulnerabilityName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    plan.description?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    plan.testCaseId.toLowerCase().includes(searchTerm.toLowerCase())
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
          <h1 className="text-2xl font-bold text-white">Test Plans</h1>
        </div>
        <button
          onClick={() => setShowModal(true)}
          className="btn-primary flex items-center"
        >
          <Plus className="w-4 h-4 mr-2" />
          Add New Test Case
        </button>
      </div>

      {/* Search */}
      <div className="flex items-center space-x-4">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
          <input
            type="text"
            placeholder="Search test plans..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="input-field pl-10 w-full"
          />
        </div>
      </div>

      {/* Test Plans Table */}
      <div className="card">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-dark-700">
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-300">Test Case ID</th>
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-300">Vulnerability</th>
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-300">Severity</th>
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-300">Description</th>
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-300">Added By</th>
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-300">Date</th>
                <th className="text-right py-3 px-4 text-sm font-medium text-gray-300">Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredTestPlans.map((plan) => (
                <tr key={plan.id} className="border-b border-dark-700 hover:bg-dark-700/50">
                  <td className="py-3 px-4 text-sm text-white font-mono">
                    {plan.testCaseId}
                  </td>
                  <td className="py-3 px-4 text-sm text-white flex items-center">
                    <FileText className="w-4 h-4 mr-2 text-gray-400" />
                    {plan.vulnerabilityName}
                  </td>
                  <td className="py-3 px-4 text-sm">
                    <span className={`status-badge ${getSeverityColor(plan.severity)}`}>
                      {plan.severity}
                    </span>
                  </td>
                  <td className="py-3 px-4 text-sm text-gray-300 max-w-xs truncate">
                    {plan.description || '-'}
                  </td>
                  <td className="py-3 px-4 text-sm text-gray-300">
                    {plan.addedBy?.username || 'System'}
                  </td>
                  <td className="py-3 px-4 text-sm text-gray-300">
                    {new Date(plan.createdAt).toLocaleDateString()}
                  </td>
                  <td className="py-3 px-4 text-right">
                    <div className="flex items-center justify-end space-x-2">
                      <button
                        onClick={() => handleEdit(plan)}
                        className="text-blue-400 hover:text-blue-300 p-1"
                      >
                        <Edit className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => handleDelete(plan.id)}
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
          <div className="bg-dark-800 rounded-xl p-6 w-full max-w-lg mx-4">
            <h2 className="text-xl font-bold text-white mb-4">
              {editingTestPlan ? 'Edit Test Plan' : 'Add New Test Case'}
            </h2>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">Vulnerability Name</label>
                <input
                  type="text"
                  value={formData.vulnerabilityName}
                  onChange={(e) => setFormData({...formData, vulnerabilityName: e.target.value})}
                  className="input-field w-full"
                  placeholder="e.g., SQL Injection, XSS Attack"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">Severity</label>
                <select
                  value={formData.severity}
                  onChange={(e) => setFormData({...formData, severity: e.target.value})}
                  className="input-field w-full"
                >
                  <option value="CRITICAL">Critical</option>
                  <option value="HIGH">High</option>
                  <option value="MEDIUM">Medium</option>
                  <option value="LOW">Low</option>
                  <option value="INFO">Info</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">Description</label>
                <textarea
                  value={formData.description}
                  onChange={(e) => setFormData({...formData, description: e.target.value})}
                  className="input-field w-full h-24 resize-none"
                  placeholder="Detailed description of the vulnerability"
                  rows={3}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">Test Procedure</label>
                <textarea
                  value={formData.testProcedure}
                  onChange={(e) => setFormData({...formData, testProcedure: e.target.value})}
                  className="input-field w-full h-32 resize-none"
                  placeholder="Step-by-step testing procedure"
                  required
                  rows={4}
                />
              </div>
              <div className="flex space-x-3 pt-4">
                <button type="submit" className="btn-primary flex-1">
                  {editingTestPlan ? 'Update' : 'Create'}
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

export default TestPlans