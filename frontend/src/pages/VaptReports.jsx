import { useState, useEffect } from 'react'
import { vaptReportsAPI, clientsAPI, applicationsAPI } from '../services/api'
import { Shield, CheckCircle, XCircle, Upload, FileText, Download } from 'lucide-react'
import toast from 'react-hot-toast'

const VaptReports = () => {
  const [clients, setClients] = useState([])
  const [applications, setApplications] = useState([])
  const [filteredApplications, setFilteredApplications] = useState([])
  const [selectedClient, setSelectedClient] = useState('')
  const [selectedApplication, setSelectedApplication] = useState('')
  const [vaptReport, setVaptReport] = useState(null)
  const [testCases, setTestCases] = useState([])
  const [selectedTestCases, setSelectedTestCases] = useState([])
  const [loading, setLoading] = useState(false)
  const [showDetailView, setShowDetailView] = useState(false)
  const [currentTestCaseIndex, setCurrentTestCaseIndex] = useState(0)
  const [currentTestCase, setCurrentTestCase] = useState(null)
  const [pocFiles, setPocFiles] = useState([])
  const [testCaseData, setTestCaseData] = useState({})
  const [pocData, setPocData] = useState({})

  useEffect(() => {
    fetchClients()
  }, [])

  useEffect(() => {
    if (selectedClient) {
      fetchApplicationsByClient(selectedClient)
    } else {
      setFilteredApplications([])
      setSelectedApplication('')
    }
  }, [selectedClient])

  const fetchClients = async () => {
    try {
      const response = await clientsAPI.getAll()
      setClients(response.data)
    } catch (error) {
      console.error('Failed to fetch clients:', error)
      toast.error('Failed to fetch clients')
    }
  }

  const fetchApplicationsByClient = async (clientId) => {
    try {
      const response = await applicationsAPI.getByClient(clientId)
      setFilteredApplications(response.data)
    } catch (error) {
      console.error('Failed to fetch applications:', error)
      toast.error('Failed to fetch applications')
    }
  }

  const handleInitializeReport = async () => {
    if (!selectedClient || !selectedApplication) {
      toast.error('Please select both client and application')
      return
    }

    setLoading(true)
    try {
      const response = await vaptReportsAPI.initialize(parseInt(selectedClient), parseInt(selectedApplication))
      setVaptReport(response.data)
      await fetchTestCases(response.data.id)
      toast.success('VAPT report initialized successfully')
    } catch (error) {
      console.error('Failed to initialize VAPT report:', error)
      if (error.response?.status === 403) {
        toast.error('Access denied. Please check your permissions.')
      } else {
        toast.error('Failed to initialize VAPT report')
      }
    } finally {
      setLoading(false)
    }
  }

  const fetchTestCases = async (reportId) => {
    try {
      const response = await vaptReportsAPI.getTestCases(reportId)
      setTestCases(response.data)
    } catch (error) {
      console.error('Failed to fetch test cases:', error)
      toast.error('Failed to fetch test cases')
    }
  }


  const handlePrepareReport = () => {
    if (selectedTestCases.length === 0) {
      toast.error('Please select at least one test case')
      return
    }
    setShowDetailView(true)
    setCurrentTestCaseIndex(0)
    setCurrentTestCase(selectedTestCases[0])
    fetchPocs(selectedTestCases[0].id)
  }

  const fetchPocs = async (testCaseId) => {
    try {
      const response = await vaptReportsAPI.getPocs(testCaseId)
      setPocFiles(response.data)
    } catch (error) {
      console.error('Failed to fetch PoCs:', error)
      setPocFiles([])
    }
  }

  const handleNextTestCase = async () => {
    if (currentTestCaseIndex < selectedTestCases.length - 1) {
      const nextIndex = currentTestCaseIndex + 1
      setCurrentTestCaseIndex(nextIndex)
      setCurrentTestCase(selectedTestCases[nextIndex])
      await fetchPocs(selectedTestCases[nextIndex].id)
    }
  }

  const handlePreviousTestCase = async () => {
    if (currentTestCaseIndex > 0) {
      const prevIndex = currentTestCaseIndex - 1
      setCurrentTestCaseIndex(prevIndex)
      setCurrentTestCase(selectedTestCases[prevIndex])
      await fetchPocs(selectedTestCases[prevIndex].id)
    }
  }

  const handleUpdateTestCase = async () => {
    try {
      const description = document.getElementById('description').value
      const testProcedure = document.getElementById('testProcedure').value
      const remediation = document.getElementById('remediation').value

      // Save current test case data to local state
      setTestCaseData(prev => ({
        ...prev,
        [currentTestCase.id]: {
          status: currentTestCase.status,
          description,
          testProcedure,
          remediation
        }
      }))

      await vaptReportsAPI.updateTestCase(currentTestCase.id, {
        status: currentTestCase.status,
        description,
        testProcedure,
        remediation
      })

      toast.success('Test case updated successfully')
      // Don't auto-advance, let user decide when to move to next
    } catch (error) {
      console.error('Failed to update test case:', error)
      toast.error('Failed to update test case')
    }
  }

  const handleFileUpload = async (event) => {
    const file = event.target.files[0]
    if (!file) return

    const description = document.getElementById('pocDescription').value

    try {
      await vaptReportsAPI.uploadPoc(currentTestCase.id, file, description)
      toast.success('PoC uploaded successfully')
      await fetchPocs(currentTestCase.id)
      document.getElementById('pocDescription').value = ''
      event.target.value = ''
    } catch (error) {
      console.error('Failed to upload PoC:', error)
      toast.error('Failed to upload PoC')
    }
  }

  const handleEvidenceUpload = async (event, pocId) => {
    const file = event.target.files[0]
    if (!file) return

    const description = document.getElementById(`evidence-description-${pocId}`).value || 'Evidence'

    try {
      await vaptReportsAPI.uploadPoc(currentTestCase.id, file, description)
      toast.success('Evidence uploaded successfully')
      await fetchPocs(currentTestCase.id)
      document.getElementById(`evidence-description-${pocId}`).value = ''
      event.target.value = ''
    } catch (error) {
      console.error('Failed to upload evidence:', error)
      toast.error('Failed to upload evidence')
    }
  }

  const handleSubmitAll = async () => {
    try {
      console.log('Starting handleSubmitAll...')
      console.log('Current test case:', currentTestCase)
      console.log('Selected test cases:', selectedTestCases)
      console.log('Test case data:', testCaseData)

      // First, save current test case data if not already saved
      const currentDescription = document.getElementById('description')?.value || ''
      const currentTestProcedure = document.getElementById('testProcedure')?.value || ''
      const currentRemediation = document.getElementById('remediation')?.value || ''

      console.log('Current form values:', { currentDescription, currentTestProcedure, currentRemediation })

      if (currentDescription || currentTestProcedure || currentRemediation) {
        console.log('Saving current test case data...')
        await vaptReportsAPI.updateTestCase(currentTestCase.id, {
          status: currentTestCase.status || 'OPEN',
          description: currentDescription,
          testProcedure: currentTestProcedure,
          remediation: currentRemediation
        })
        console.log('Current test case saved successfully')
      }

      // Submit all test cases data - iterate through all selected test cases
      console.log('Saving all selected test cases...')
      for (const testCase of selectedTestCases) {
        console.log('Processing test case:', testCase.id)
        const data = testCaseData[testCase.id]

        if (data) {
          console.log('Using saved data for test case:', testCase.id)
          await vaptReportsAPI.updateTestCase(testCase.id, {
            status: data.status || 'OPEN',
            description: data.description || '',
            testProcedure: data.testProcedure || '',
            remediation: data.remediation || ''
          })
        } else {
          console.log('Using existing data for test case:', testCase.id)
          // For test cases without saved data, use their existing data
          await vaptReportsAPI.updateTestCase(testCase.id, {
            status: testCase.status || 'OPEN',
            description: testCase.description || '',
            testProcedure: testCase.testProcedure || '',
            remediation: testCase.remediation || ''
          })
        }
      }
      console.log('All test cases saved successfully')

      // Generate report
      console.log('Generating report...')
      const response = await vaptReportsAPI.generateReport(vaptReport.id)
      console.log('Report generation response:', response)
      toast.success('All data submitted and report generated successfully')
      setShowDetailView(false)
      // Reports are now available for download
    } catch (error) {
      console.error('Failed to submit all data:', error)
      console.error('Error details:', error.response?.data || error.message)
      toast.error('Failed to submit all data: ' + (error.response?.data?.message || error.message))
    }
  }

  if (showDetailView && currentTestCase) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-bold text-white">VAPT Report - Test Case Details</h1>
          <button
            onClick={() => setShowDetailView(false)}
            className="btn-secondary"
          >
            Back to Overview
          </button>
        </div>

        <div className="card">
          <div className="mb-4">
            <h2 className="text-xl font-semibold text-white mb-2">
              {currentTestCase.testPlan.vulnerabilityName} (TP-{currentTestCase.testPlan.testCaseId})
            </h2>
            <div className="flex items-center space-x-2">
              <span className={`status-badge ${
                currentTestCase.status === 'OPEN' ? 'bg-red-500/20 text-red-400' : 'bg-green-500/20 text-green-400'
              }`}>
                {currentTestCase.status}
              </span>
              <span className="text-gray-400">Test Case {currentTestCaseIndex + 1} of {selectedTestCases.length}</span>
            </div>
          </div>

          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">Description</label>
              <textarea
                id="description"
                defaultValue={testCaseData[currentTestCase.id]?.description || currentTestCase.description || ''}
                className="input-field w-full h-24"
                placeholder="Enter test case description..."
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">Test Procedure</label>
              <textarea
                id="testProcedure"
                defaultValue={testCaseData[currentTestCase.id]?.testProcedure || currentTestCase.testProcedure || ''}
                className="input-field w-full h-24"
                placeholder="Enter test procedure..."
              />
            </div>


            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">Remediation</label>
              <textarea
                id="remediation"
                defaultValue={testCaseData[currentTestCase.id]?.remediation || currentTestCase.remediation || ''}
                className="input-field w-full h-24"
                placeholder="Enter remediation steps..."
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">Proof of Concept (PoC)</label>
              <div className="space-y-4">
                {pocFiles.map((poc, index) => (
                  <details key={poc.id} className="bg-dark-700 rounded-lg">
                    <summary className="p-4 cursor-pointer hover:bg-dark-600 rounded-lg flex items-center justify-between">
                      <div className="flex items-center space-x-2">
                        <FileText className="w-4 h-4 text-gray-400" />
                        <span className="text-white font-medium">PoC {index + 1}</span>
                      </div>
                      <button
                        onClick={async (e) => {
                          e.preventDefault()
                          try {
                            await vaptReportsAPI.deletePoc(poc.id)
                            toast.success('PoC deleted successfully')
                            await fetchPocs(currentTestCase.id)
                          } catch (error) {
                            console.error('Failed to delete PoC:', error)
                            toast.error('Failed to delete PoC')
                          }
                        }}
                        className="text-red-400 hover:text-red-300 text-sm"
                      >
                        Remove
                      </button>
                    </summary>
                    <div className="p-4 pt-0 space-y-2">
                      <div>
                        <label className="block text-sm font-medium text-gray-300 mb-1">PoC Description</label>
                        <textarea
                          id={`poc-description-${poc.id}`}
                          placeholder="Enter PoC description..."
                          defaultValue={poc.description || ''}
                          onBlur={async (e) => {
                            const newDescription = e.target.value
                            if (newDescription !== poc.description) {
                              try {
                                await vaptReportsAPI.updatePoc(poc.id, { description: newDescription })
                                toast.success('PoC description updated')
                                // Refresh PoCs to show updated data
                                await fetchPocs(currentTestCase.id)
                              } catch (error) {
                                console.error('Failed to update PoC description:', error)
                                toast.error('Failed to update PoC description')
                              }
                            }
                          }}
                          className="input-field w-full h-20"
                        />
                      </div>
                      <div className="space-y-2">
                        <label className="text-sm text-gray-300">Evidence Files:</label>
                        {poc.fileName && (
                          <div className="bg-dark-600 p-2 rounded text-sm text-gray-300 flex items-center justify-between">
                            <span>{poc.fileName}</span>
                            <span className="text-green-400">✓ Uploaded</span>
                          </div>
                        )}
                        <div className="border-2 border-dashed border-gray-600 rounded-lg p-3">
                          <input
                            type="file"
                            onChange={(e) => handleEvidenceUpload(e, poc.id)}
                            className="hidden"
                            id={`evidence-upload-${poc.id}`}
                            accept="image/*,.pdf,.doc,.docx"
                          />
                          <label htmlFor={`evidence-upload-${poc.id}`} className="cursor-pointer flex flex-col items-center">
                            <Upload className="w-6 h-6 text-gray-400 mb-1" />
                            <span className="text-gray-400 text-sm">Click to upload additional evidence</span>
                          </label>
                        </div>
                      </div>
                    </div>
                  </details>
                ))}

                <div className="border-2 border-dashed border-gray-600 rounded-lg p-4">
                  <input
                    type="file"
                    onChange={handleFileUpload}
                    className="hidden"
                    id="poc-upload"
                    accept="image/*,.pdf,.doc,.docx"
                  />
                  <label htmlFor="poc-upload" className="cursor-pointer flex flex-col items-center">
                    <Upload className="w-8 h-8 text-gray-400 mb-2" />
                    <span className="text-gray-400">Click to upload PoC file</span>
                  </label>
                  <input
                    id="pocDescription"
                    type="text"
                    placeholder="PoC description (optional)"
                    className="input-field w-full mt-2"
                  />
                </div>

                <button className="btn-secondary w-full">
                  Add Another PoC
                </button>
              </div>
            </div>
          </div>

          <div className="flex justify-between mt-6">
           <div className="flex space-x-2">
             <button
               onClick={() => setShowDetailView(false)}
               className="btn-secondary"
             >
               Back
             </button>
             {currentTestCaseIndex > 0 && (
               <button
                 onClick={handlePreviousTestCase}
                 className="btn-secondary"
               >
                 Previous
               </button>
             )}
             <button
               onClick={handleUpdateTestCase}
               className="btn-secondary"
             >
               Save Changes
             </button>
           </div>
           <div className="flex space-x-2">
             {currentTestCaseIndex < selectedTestCases.length - 1 ? (
               <button
                 onClick={handleNextTestCase}
                 className="btn-primary"
               >
                 Next Test Case
               </button>
             ) : (
               <button
                 onClick={handleSubmitAll}
                 className="btn-primary"
               >
                 Submit All & Generate Report
               </button>
             )}
           </div>
         </div>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-white flex items-center">
          <Shield className="w-6 h-6 mr-2" />
          VAPT Report
        </h1>
      </div>

      {/* Selection Form */}
      <div className="card">
        <h2 className="text-lg font-semibold text-white mb-4">Select Client & Application</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-300 mb-1">Client</label>
            <select
              value={selectedClient}
              onChange={(e) => setSelectedClient(e.target.value)}
              className="input-field w-full"
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
              value={selectedApplication}
              onChange={(e) => setSelectedApplication(e.target.value)}
              className="input-field w-full"
              disabled={!selectedClient}
            >
              <option value="">Select an application</option>
              {filteredApplications.map(app => (
                <option key={app.id} value={app.id}>
                  {app.applicationName} ({app.environment})
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="mt-4">
          <button
            onClick={handleInitializeReport}
            disabled={loading || !selectedClient || !selectedApplication}
            className="btn-primary disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {loading ? 'Loading...' : 'Submit'}
          </button>
        </div>
      </div>

      {/* Test Cases Overview */}
      {testCases.length > 0 && (
        <div className="card">
          <h2 className="text-lg font-semibold text-white mb-4">Test Cases Overview</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {testCases.map((testCase, index) => (
              <div key={testCase.id} className="bg-dark-700 p-4 rounded-lg">
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center space-x-2">
                    <input
                      type="checkbox"
                      checked={selectedTestCases.some(tc => tc.id === testCase.id)}
                      onChange={(e) => {
                        if (e.target.checked) {
                          setSelectedTestCases([...selectedTestCases, testCase])
                        } else {
                          setSelectedTestCases(selectedTestCases.filter(tc => tc.id !== testCase.id))
                        }
                      }}
                      className="w-4 h-4 text-blue-600 bg-gray-100 border-gray-300 rounded focus:ring-blue-500"
                    />
                    <span className="text-gray-400 font-mono text-sm">#{index + 1}</span>
                  </div>
                  <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                    testCase.status === 'OPEN'
                      ? 'bg-red-500/20 text-red-400'
                      : 'bg-green-500/20 text-green-400'
                  }`}>
                    {testCase.status}
                  </span>
                </div>
                <div>
                  <h3 className="text-white font-medium text-sm">{testCase.testPlan.vulnerabilityName}</h3>
                  <p className="text-gray-400 text-xs">TP-{testCase.testPlan.testCaseId}</p>
                </div>
              </div>
            ))}
          </div>

          <div className="mt-6">
            <button
              onClick={handlePrepareReport}
              className="btn-primary"
            >
              Prepare Report
            </button>
          </div>
        </div>
      )}

      {/* Report Generation */}
      {vaptReport && (
        <div className="card">
          <h2 className="text-lg font-semibold text-white mb-4">Download Final Report</h2>
          <div className="flex space-x-4">
            <button
              onClick={async () => {
                try {
                  const response = await vaptReportsAPI.downloadDocxReport(vaptReport.id)
                  const url = window.URL.createObjectURL(new Blob([response.data]))
                  const link = document.createElement('a')
                  link.href = url
                  link.setAttribute('download', `vapt-report-${vaptReport.id}.docx`)
                  document.body.appendChild(link)
                  link.click()
                  link.remove()
                  toast.success('DOCX report downloaded successfully')
                } catch (error) {
                  console.error('Failed to download DOCX report:', error)
                  toast.error('Failed to download DOCX report')
                }
              }}
              className="btn-secondary flex items-center"
            >
              <Download className="w-4 h-4 mr-2" />
              Download DOCX
            </button>
            <button
              onClick={async () => {
                try {
                  const response = await vaptReportsAPI.downloadPdfReport(vaptReport.id)
                  const url = window.URL.createObjectURL(new Blob([response.data]))
                  const link = document.createElement('a')
                  link.href = url
                  link.setAttribute('download', `vapt-report-${vaptReport.id}.pdf`)
                  document.body.appendChild(link)
                  link.click()
                  link.remove()
                  toast.success('PDF report downloaded successfully')
                } catch (error) {
                  console.error('Failed to download PDF report:', error)
                  toast.error('Failed to download PDF report')
                }
              }}
              className="btn-secondary flex items-center"
            >
              <Download className="w-4 h-4 mr-2" />
              Download PDF
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

export default VaptReports