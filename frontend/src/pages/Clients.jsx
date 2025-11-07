import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { clientsAPI } from '../services/api'
import { Plus, Search, Edit, Trash2 } from 'lucide-react'
import toast from 'react-hot-toast'

const Clients = () => {
  const navigate = useNavigate()
  const [clients, setClients] = useState([])
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState('')
  const [showModal, setShowModal] = useState(false)
  const [editingClient, setEditingClient] = useState(null)
  const [formData, setFormData] = useState({
    clientName: '',
    email: '',
    contactNumber: ''
  })

  useEffect(() => {
    fetchClients()
  }, [])

  const fetchClients = async () => {
    try {
      const response = await clientsAPI.getAll()
      setClients(response.data)
    } catch (error) {
      console.error('Failed to fetch clients:', error)
      toast.error('Failed to fetch clients')
      setClients([])
    } finally {
      setLoading(false)
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      if (editingClient) {
        const updatedClient = await clientsAPI.update(editingClient.id, formData)
        setClients(clients.map(client =>
          client.id === editingClient.id ? updatedClient.data : client
        ))
        toast.success('Client updated successfully')
      } else {
        const newClient = await clientsAPI.create(formData)
        setClients([...clients, newClient.data])
        toast.success('Client created successfully')
      }
      setShowModal(false)
      resetForm()
    } catch (error) {
      toast.error('Failed to save client')
    }
  }

  const handleEdit = (client) => {
    setEditingClient(client)
    setFormData({
      clientName: client.clientName,
      email: client.email,
      contactNumber: client.contactNumber || ''
    })
    setShowModal(true)
  }

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this client?')) {
      try {
        await clientsAPI.delete(id)
        toast.success('Client deleted successfully')
        fetchClients()
      } catch (error) {
        toast.error('Failed to delete client')
      }
    }
  }

  const resetForm = () => {
    setFormData({
      clientName: '',
      email: '',
      contactNumber: ''
    })
    setEditingClient(null)
  }

  const filteredClients = clients.filter(client =>
    client.clientName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    client.email.toLowerCase().includes(searchTerm.toLowerCase())
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
          <h1 className="text-2xl font-bold text-white">Clients</h1>
        </div>
        <button
          onClick={() => setShowModal(true)}
          className="btn-primary flex items-center"
        >
          <Plus className="w-4 h-4 mr-2" />
          Add New Client
        </button>
      </div>

      {/* Search */}
      <div className="flex items-center space-x-4">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
          <input
            type="text"
            placeholder="Search clients..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="input-field pl-10 w-full"
          />
        </div>
      </div>

      {/* Clients Table */}
      <div className="card">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-dark-700">
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-300">Client Name</th>
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-300">Email</th>
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-300">Contact</th>
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-300">Added Date</th>
                <th className="text-right py-3 px-4 text-sm font-medium text-gray-300">Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredClients.map((client) => (
                <tr key={client.id} className="border-b border-dark-700 hover:bg-dark-700/50">
                  <td className="py-3 px-4 text-sm text-white">{client.clientName}</td>
                  <td className="py-3 px-4 text-sm text-gray-300">{client.email}</td>
                  <td className="py-3 px-4 text-sm text-gray-300">{client.contactNumber || '-'}</td>
                  <td className="py-3 px-4 text-sm text-gray-300">
                    {new Date(client.createdAt).toLocaleDateString()}
                  </td>
                  <td className="py-3 px-4 text-right">
                    <div className="flex items-center justify-end space-x-2">
                      <button
                        onClick={() => handleEdit(client)}
                        className="text-blue-400 hover:text-blue-300 p-1"
                      >
                        <Edit className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => handleDelete(client.id)}
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
              {editingClient ? 'Edit Client' : 'Add New Client'}
            </h2>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">Client Name</label>
                <input
                  type="text"
                  value={formData.clientName}
                  onChange={(e) => setFormData({...formData, clientName: e.target.value})}
                  className="input-field w-full"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">Email</label>
                <input
                  type="email"
                  value={formData.email}
                  onChange={(e) => setFormData({...formData, email: e.target.value})}
                  className="input-field w-full"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">Contact Number</label>
                <input
                  type="text"
                  value={formData.contactNumber}
                  onChange={(e) => setFormData({...formData, contactNumber: e.target.value})}
                  className="input-field w-full"
                />
              </div>
              <div className="flex space-x-3 pt-4">
                <button type="submit" className="btn-primary flex-1">
                  {editingClient ? 'Update' : 'Create'}
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

export default Clients