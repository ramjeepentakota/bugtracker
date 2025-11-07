import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { vaptReportsAPI, clientsAPI, applicationsAPI } from '../services/api'
import { Shield, CheckCircle, XCircle, Upload, FileText, Download } from 'lucide-react'
import toast from 'react-hot-toast'

const VaptReports = () => {
  const navigate = useNavigate()
  const [clients, setClients] = useState([])
  const [applications, setApplications] = useState([])
  const [filteredApplications, setFilteredApplications] = useState([])
  const [selectedClient, setSelectedClient] = useState('')
  const [selectedApplication, setSelectedApplication] = useState('')
  const [availableTestPlans, setAvailableTestPlans] = useState([])
  const [selectedTestPlanIds, setSelectedTestPlanIds] = useState([])
  const [vaptReport, setVaptReport] = useState(null)
  const [testCases, setTestCases] = useState([])
  const [selectedTestCases, setSelectedTestCases] = useState([])
  const [loading, setLoading] = useState(false)
  const [showDetailView, setShowDetailView] = useState(false)
  const [currentTestCaseIndex, setCurrentTestCaseIndex] = useState(0)
  // Removed currentTestCase state - use selectedTestCases[currentTestCaseIndex] directly
  const [pocFiles, setPocFiles] = useState([])
  const [testCaseData, setTestCaseData] = useState({})
  const [pocData, setPocData] = useState({})
  const [reportGenerated, setReportGenerated] = useState(false)
  const [showReportConfig, setShowReportConfig] = useState(false)
  const [isModifyMode, setIsModifyMode] = useState(false)
  const [reportConfig, setReportConfig] = useState({
    assessmentDate: '',
    reportVersion: '',
    preparedBy: '',
    reviewedBy: '',
    submittedTo: '',
    objective: '',
    scope: '',
    approach: '',
    keyHighlights: '',
    assetType: '',
    urls: '',
    startDate: '',
    endDate: '',
    testers: '',
    recommendations: '',
    nextSteps: '',
    approvedBy: ''
  })
  const [isReportExpired, setIsReportExpired] = useState(false)
  const [lastUpdate, setLastUpdate] = useState(Date.now())

  useEffect(() => {
    fetchClients()
    fetchTestPlans()
  }, [])

  // Get current user role for UI controls
  const currentUserRole = useAuth().user?.role

  // Check if user has download-only access to VAPT reports
  const hasDownloadOnlyAccess = currentUserRole === 'PM_DM'

  useEffect(() => {
    if (selectedClient) {
      fetchApplicationsByClient(selectedClient)
    } else {
      setFilteredApplications([])
      setSelectedApplication('')
    }
  }, [selectedClient])

  // Removed useEffect for currentTestCase - use selectedTestCases[currentTestCaseIndex] directly

  // Auto-refresh mechanism for real-time sync - disabled in detail view
  useEffect(() => {
    if (vaptReport && testCases.length > 0 && !showDetailView) {
      const interval = setInterval(async () => {
        try {
          // Always refresh to ensure real-time sync - no change detection needed
          const freshTestCases = await vaptReportsAPI.getTestCases(vaptReport.id)
          console.log('Auto-refreshing test cases...')
          setTestCases(freshTestCases.data)
          setSelectedTestCases(freshTestCases.data) // Also update selectedTestCases to keep them in sync
          setLastUpdate(Date.now())
        } catch (error) {
          console.warn('Failed to auto-refresh test cases:', error)
        }
      }, 1000) // Check every 1 second for real-time updates

      return () => clearInterval(interval)
    }
  }, [vaptReport, testCases, showDetailView])

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

  const fetchTestPlans = async () => {
    try {
      const response = await vaptReportsAPI.getTestPlans()
      setAvailableTestPlans(response.data)
    } catch (error) {
      console.error('Failed to fetch test plans:', error)
      toast.error('Failed to fetch test plans')
    }
  }

  const handleInitializeReport = async () => {
    if (!selectedClient || !selectedApplication) {
      toast.error('Please select both client and application')
      return
    }

    if (selectedTestPlanIds.length === 0) {
      toast.error('Please select at least one test case')
      return
    }

    setLoading(true)
    try {
      console.log('Initializing VAPT report with:', { selectedClient, selectedApplication, selectedTestPlanIds })
      const response = await vaptReportsAPI.initialize(selectedClient, selectedApplication, selectedTestPlanIds)
      console.log('Initialize response:', response.data)

      const { report, testCases, isExisting } = response.data

      setVaptReport(report)
      setReportGenerated(report.status === 'COMPLETED') // Set based on report status
      setIsModifyMode(isExisting) // Set modify mode if this is an existing report

      toast.success(isExisting ? 'Existing VAPT report loaded successfully' : 'VAPT report initialized successfully')

      // Use test cases directly from the response
      if (testCases && testCases.length > 0) {
        // Automatically select all test cases and show overview
        setSelectedTestCases(testCases)
        setTestCases(testCases) // Also update the testCases state
        setCurrentTestCaseIndex(0) // Set the first test case as current
        setShowDetailView(false) // Show overview page instead of jumping to detail
        console.log('Showing overview with', testCases.length, 'test cases')
      } else {
        console.error('No test cases returned from server:', { report, testCases })
        toast.error('No test cases were created for this report. Please check the server logs and try again.')
      }
    } catch (error) {
      console.error('Failed to initialize VAPT report:', error)
      console.error('Error details:', error.response?.data)
      if (error.response?.status === 403) {
        toast.error('Access denied. Please check your permissions.')
      } else if (error.response?.status === 500) {
        toast.error('Server error occurred. Please check server logs.')
      } else if (error.response?.data?.message) {
        toast.error(`Failed to initialize VAPT report: ${error.response.data.message}`)
      } else {
        toast.error('Failed to initialize VAPT report')
      }
    } finally {
      setLoading(false)
    }
  }

  const handleConfigureReport = () => {
    // Pre-populate with existing data if available
    if (vaptReport) {
      setReportConfig({
        assessmentDate: vaptReport.assessmentDate || '',
        reportVersion: vaptReport.reportVersion || '',
        preparedBy: vaptReport.preparedBy || '',
        reviewedBy: vaptReport.reviewedBy || '',
        submittedTo: vaptReport.submittedTo || '',
        objective: vaptReport.objective || '',
        scope: vaptReport.scope || '',
        approach: vaptReport.approach || '',
        keyHighlights: vaptReport.keyHighlights || '',
        assetType: vaptReport.assetType || '',
        urls: vaptReport.urls || '',
        startDate: vaptReport.startDate || '',
        endDate: vaptReport.endDate || '',
        testers: vaptReport.testers || '',
        recommendations: vaptReport.recommendations || '',
        nextSteps: vaptReport.nextSteps || '',
        approvedBy: vaptReport.approvedBy || ''
      })
    }
    setShowReportConfig(true)
  }

  const handleModifyReport = async (reportId) => {
    setLoading(true)
    try {
      console.log('Loading report for modification:', reportId)
      const response = await vaptReportsAPI.getReportForModification(reportId)
      console.log('Modify response:', response.data)

      const { report, testCases, availableTestPlans, selectedTestPlanIds } = response.data

      setVaptReport(report)
      setTestCases(testCases)
      setSelectedTestCases(testCases)
      setCurrentTestCaseIndex(0) // Set the first test case as current
      setAvailableTestPlans(availableTestPlans)
      setSelectedTestPlanIds(selectedTestPlanIds)
      setReportGenerated(report.status === 'COMPLETED')
      setIsModifyMode(true)

      // Check if report is expired
      const reportResponse = await vaptReportsAPI.getReport(reportId)
      setIsReportExpired(reportResponse.data.isExpired)

      // Set client and application from report
      setSelectedClient(report.client?.id)
      setSelectedApplication(report.application?.id)

      toast.success('Report loaded for modification')
    } catch (error) {
      console.error('Failed to load report for modification:', error)
      toast.error('Failed to load report for modification')
    } finally {
      setLoading(false)
    }
  }

  const handleAddTestCases = async () => {
    if (selectedTestPlanIds.length === 0) {
      toast.error('Please select at least one test case to add')
      return
    }

    setLoading(true)
    try {
      console.log('Adding test cases:', selectedTestPlanIds)
      const response = await vaptReportsAPI.addTestCases(vaptReport.id, selectedTestPlanIds)
      console.log('Add test cases response:', response.data)

      const { testCases: updatedTestCases, addedCount, message } = response.data

      setTestCases(updatedTestCases)
      setSelectedTestCases(updatedTestCases)
      setCurrentTestCaseIndex(0) // Set the first test case as current

      toast.success(message || `Successfully added ${addedCount} test cases`)

      // Clear selection after adding
      setSelectedTestPlanIds([])
    } catch (error) {
      console.error('Failed to add test cases:', error)
      toast.error('Failed to add test cases: ' + (error.response?.data?.error || error.message))
    } finally {
      setLoading(false)
    }
  }

  const handleSaveReportConfig = async () => {
    try {
      console.log('Saving report config:', reportConfig)
      await vaptReportsAPI.updateReportConfig(vaptReport.id, reportConfig)
      toast.success('Report configuration saved successfully')
      setShowReportConfig(false)
      // Refresh report data
      const response = await vaptReportsAPI.getReport(vaptReport.id)
      setVaptReport(response.data.report)
      setIsReportExpired(response.data.isExpired)
    } catch (error) {
      console.error('Failed to save report configuration:', error)
      console.error('Error response:', error.response?.data)
      toast.error('Failed to save report configuration: ' + (error.response?.data?.message || error.message))
    }
  }

  const fetchTestCases = async (reportId) => {
    try {
      const response = await vaptReportsAPI.getTestCases(reportId)
      setTestCases(response.data)
      // Force re-render of summary counts by updating state
      setTestCases(prev => [...response.data])
      return response.data
    } catch (error) {
      console.error('Failed to fetch test cases:', error)
      toast.error('Failed to fetch test cases')
      throw error // Re-throw to allow caller to handle
    }
  }


  const handlePrepareReport = async () => {
    if (selectedTestCases.length === 0) {
      toast.error('Please select at least one test case')
      return
    }
    // Fetch fresh data before entering detail view
    await fetchTestCases(vaptReport.id)
    setShowDetailView(true)
    setCurrentTestCaseIndex(0)
    try {
      await fetchPocs(selectedTestCases[0].id)
    } catch (error) {
      console.warn('Failed to fetch PoCs for first test case:', error)
      setPocFiles([]) // Set empty array on error
    }
  }

  const fetchPocs = async (testCaseId) => {
    try {
      const response = await vaptReportsAPI.getPocs(testCaseId)
      setPocFiles(response.data || [])
    } catch (error) {
      console.error('Failed to fetch PoCs:', error)
      setPocFiles([])
      throw error // Re-throw to allow caller to handle
    }
  }

  const handleNextTestCase = async () => {
    if (currentTestCaseIndex < selectedTestCases.length - 1) {
      const currentTestCase = selectedTestCases[currentTestCaseIndex]
      // Auto-save current test case data before navigating
      try {
        const description = document.getElementById('description').value
        const testProcedure = document.getElementById('testProcedure').value

        // Save current test case data to local state
        setTestCaseData(prev => ({
          ...prev,
          [currentTestCase.id]: {
            status: currentTestCase.status,
            description,
            testProcedure,
            vulnerabilityStatus: currentTestCase.vulnerabilityStatus
          }
        }))

        // Update the test case in backend
        const updateData = {
          // Remove explicit status - let backend determine based on vulnerabilityStatus
          description,
          testProcedure,
          vulnerabilityStatus: currentTestCase.vulnerabilityStatus
        };
        console.log('Manual save - updating test case with data:', updateData);
        await vaptReportsAPI.updateTestCase(currentTestCase.id, updateData)

        console.log('Auto-saved test case data before navigation')
      } catch (error) {
        console.warn('Failed to auto-save test case data:', error)
        // Continue with navigation even if save fails
      }

      const nextIndex = currentTestCaseIndex + 1
      setCurrentTestCaseIndex(nextIndex)
      try {
        await fetchPocs(selectedTestCases[nextIndex].id)
      } catch (error) {
        console.warn('Failed to fetch PoCs for next test case:', error)
        setPocFiles([]) // Set empty array on error
      }
    }
  }

  const handlePreviousTestCase = async () => {
    if (currentTestCaseIndex > 0) {
      const currentTestCase = selectedTestCases[currentTestCaseIndex]
      // Auto-save current test case data before navigating
      try {
        const description = document.getElementById('description').value
        const testProcedure = document.getElementById('testProcedure').value

        // Save current test case data to local state
        setTestCaseData(prev => ({
          ...prev,
          [currentTestCase.id]: {
            status: currentTestCase.status,
            description,
            testProcedure,
            vulnerabilityStatus: currentTestCase.vulnerabilityStatus
          }
        }))

        // Update the test case in backend - remove explicit status
        const updateData = {
          description,
          testProcedure,
          vulnerabilityStatus: currentTestCase.vulnerabilityStatus
        };
        console.log('Updating test case with data:', updateData);
        await vaptReportsAPI.updateTestCase(currentTestCase.id, updateData)

        console.log('Auto-saved test case data before navigation')
      } catch (error) {
        console.warn('Failed to auto-save test case data:', error)
        // Continue with navigation even if save fails
      }

      const prevIndex = currentTestCaseIndex - 1
      setCurrentTestCaseIndex(prevIndex)
      try {
        await fetchPocs(selectedTestCases[prevIndex].id)
      } catch (error) {
        console.warn('Failed to fetch PoCs for previous test case:', error)
        setPocFiles([]) // Set empty array on error
      }
    }
  }

  const handleUpdateTestCase = async () => {
    try {
      const currentTestCase = selectedTestCases[currentTestCaseIndex]
      const description = document.getElementById('description').value
      const testProcedure = document.getElementById('testProcedure').value

      // Save current test case data to local state
      setTestCaseData(prev => ({
        ...prev,
        [currentTestCase.id]: {
          status: currentTestCase.status,
          description,
          testProcedure,
          vulnerabilityStatus: currentTestCase.vulnerabilityStatus
        }
      }))

      const updateData = {
        description,
        testProcedure,
        vulnerabilityStatus: currentTestCase.vulnerabilityStatus
      };
      console.log('Updating test case with data:', updateData);
      await vaptReportsAPI.updateTestCase(currentTestCase.id, updateData)

      // Immediately refresh test cases to ensure UI shows current database state
      await fetchTestCases(vaptReport.id);

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

    const currentTestCase = selectedTestCases[currentTestCaseIndex]
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

    const currentTestCase = selectedTestCases[currentTestCaseIndex]
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
      console.log('Selected test cases:', selectedTestCases)
      console.log('Test case data:', testCaseData)

      // First, save current test case data if not already saved
      const currentTestCase = selectedTestCases[currentTestCaseIndex]
      console.log('Current test case:', currentTestCase)
      const currentDescription = document.getElementById('description')?.value || ''
      const currentTestProcedure = document.getElementById('testProcedure')?.value || ''

      console.log('Current form values:', { currentDescription, currentTestProcedure })

      if (currentDescription || currentTestProcedure) {
        console.log('Saving current test case data...')
        const updateData = {
          description: currentDescription,
          testProcedure: currentTestProcedure,
          vulnerabilityStatus: currentTestCase.vulnerabilityStatus || 'OPEN'
        };
        console.log('Updating current test case with data:', updateData);
        await vaptReportsAPI.updateTestCase(currentTestCase.id, updateData)
        console.log('Current test case saved successfully')
      }

      // Submit all test cases data - iterate through all selected test cases
      console.log('Saving all selected test cases...')
      for (const testCase of selectedTestCases) {
        console.log('Processing test case:', testCase.id)
        const data = testCaseData[testCase.id]

        if (data) {
          console.log('Using saved data for test case:', testCase.id)
          const updateData = {
            description: data.description || '',
            testProcedure: data.testProcedure || '',
            vulnerabilityStatus: data.vulnerabilityStatus || 'OPEN'
          };
          console.log('Updating test case', testCase.id, 'with data:', updateData);
          await vaptReportsAPI.updateTestCase(testCase.id, updateData)
        } else {
          console.log('Using existing data for test case:', testCase.id)
          // For test cases without saved data, use their existing data
          const updateData = {
            description: testCase.description || '',
            testProcedure: testCase.testProcedure || '',
            vulnerabilityStatus: testCase.vulnerabilityStatus || 'OPEN'
          };
          console.log('Updating test case', testCase.id, 'with data:', updateData);
          await vaptReportsAPI.updateTestCase(testCase.id, updateData)
        }
      }
      console.log('All test cases saved successfully')

      // Immediately refresh test cases to ensure UI shows final database state
      await fetchTestCases(vaptReport.id);

      // Generate report with validation
      console.log('Generating report...')
      const response = await vaptReportsAPI.generateReport(vaptReport.id)
      console.log('Report generation response:', response)

      if (response.data.message && response.data.message.includes('Validation failed')) {
        toast.error(response.data.message)
        return
      }

      toast.success('All data submitted and report generated successfully')
      setReportGenerated(true)
      setShowDetailView(false)
      // Reports are now available for download
    } catch (error) {
      console.error('Failed to submit all data:', error)
      console.error('Error details:', error.response?.data || error.message)

      if (error.response?.data?.message && error.response.data.message.includes('Validation failed')) {
        toast.error(error.response.data.message)
      } else {
        toast.error('Failed to submit all data: ' + (error.response?.data?.message || error.message))
      }
    }
  }

  if (showReportConfig) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-bold text-white">VAPT Report Configuration</h1>
          <button
            onClick={() => setShowReportConfig(false)}
            className="btn-secondary"
          >
            Back to Report
          </button>
        </div>

        {isReportExpired && (
          <div className="bg-red-500/10 border border-red-500/20 rounded-lg p-4">
            <div className="flex items-center space-x-2">
              <span className="text-red-400 font-semibold">⚠️ Report Expired</span>
            </div>
            <p className="text-red-300 text-sm mt-1">
              This report has expired. Please re-initiate the VAPT assessment.
            </p>
          </div>
        )}

        <div className="card">
          <h2 className="text-lg font-semibold text-white mb-4">Report Details</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">
                Assessment Date
                {isReportExpired && <span className="text-red-400 text-xs ml-2">(Frozen - Report Expired)</span>}
                {!isReportExpired && vaptReport?.status !== 'INITIALIZED' && <span className="text-yellow-400 text-xs ml-2">(Frozen - Report In Progress)</span>}
              </label>
              <input
                type="date"
                value={reportConfig.assessmentDate}
                onChange={(e) => setReportConfig({...reportConfig, assessmentDate: e.target.value})}
                className={`input-field w-full ${isReportExpired || vaptReport?.status !== 'INITIALIZED' ? 'opacity-50 cursor-not-allowed' : ''}`}
                disabled={isReportExpired || vaptReport?.status !== 'INITIALIZED'}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">Report Version</label>
              <input
                type="text"
                value={reportConfig.reportVersion}
                onChange={(e) => setReportConfig({...reportConfig, reportVersion: e.target.value})}
                className="input-field w-full"
                placeholder="e.g., 1.0.20241103"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">Prepared By</label>
              <input
                type="text"
                value={reportConfig.preparedBy}
                onChange={(e) => setReportConfig({...reportConfig, preparedBy: e.target.value})}
                className="input-field w-full"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">Reviewed By</label>
              <input
                type="text"
                value={reportConfig.reviewedBy}
                onChange={(e) => setReportConfig({...reportConfig, reviewedBy: e.target.value})}
                className="input-field w-full"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">Submitted To</label>
              <input
                type="text"
                value={reportConfig.submittedTo}
                onChange={(e) => setReportConfig({...reportConfig, submittedTo: e.target.value})}
                className="input-field w-full"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">Asset Type</label>
              <select
                value={reportConfig.assetType}
                onChange={(e) => setReportConfig({...reportConfig, assetType: e.target.value})}
                className="input-field w-full"
              >
                <option value="">Select asset type</option>
                <option value="Web Application">Web Application</option>
                <option value="Mobile Application">Mobile Application</option>
                <option value="API">API</option>
                <option value="Network">Network</option>
                <option value="Infrastructure">Infrastructure</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">Start Date</label>
              <input
                type="date"
                value={reportConfig.startDate}
                onChange={(e) => setReportConfig({...reportConfig, startDate: e.target.value})}
                className="input-field w-full"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">End Date</label>
              <input
                type="date"
                value={reportConfig.endDate}
                onChange={(e) => setReportConfig({...reportConfig, endDate: e.target.value})}
                className="input-field w-full"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">Testers</label>
              <input
                type="text"
                value={reportConfig.testers}
                onChange={(e) => setReportConfig({...reportConfig, testers: e.target.value})}
                className="input-field w-full"
                placeholder="Comma-separated tester names"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">Approved By</label>
              <input
                type="text"
                value={reportConfig.approvedBy}
                onChange={(e) => setReportConfig({...reportConfig, approvedBy: e.target.value})}
                className="input-field w-full"
              />
            </div>
          </div>

          <div className="mt-6 space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">Objective</label>
              <textarea
                value={reportConfig.objective}
                onChange={(e) => setReportConfig({...reportConfig, objective: e.target.value})}
                className="input-field w-full h-20"
                placeholder="Assessment objective..."
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">Scope</label>
              <textarea
                value={reportConfig.scope}
                onChange={(e) => setReportConfig({...reportConfig, scope: e.target.value})}
                className="input-field w-full h-20"
                placeholder="Assessment scope..."
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">Approach</label>
              <textarea
                value={reportConfig.approach}
                onChange={(e) => setReportConfig({...reportConfig, approach: e.target.value})}
                className="input-field w-full h-20"
                placeholder="Testing approach and methodology..."
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">Key Highlights</label>
              <textarea
                value={reportConfig.keyHighlights}
                onChange={(e) => setReportConfig({...reportConfig, keyHighlights: e.target.value})}
                className="input-field w-full h-20"
                placeholder="Key findings and highlights..."
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">URLs / IPs</label>
              <textarea
                value={reportConfig.urls}
                onChange={(e) => setReportConfig({...reportConfig, urls: e.target.value})}
                className="input-field w-full h-20"
                placeholder="Target URLs, IPs, or endpoints..."
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">Recommendations</label>
              <textarea
                value={reportConfig.recommendations}
                onChange={(e) => setReportConfig({...reportConfig, recommendations: e.target.value})}
                className="input-field w-full h-20"
                placeholder="Security recommendations..."
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">Next Steps</label>
              <textarea
                value={reportConfig.nextSteps}
                onChange={(e) => setReportConfig({...reportConfig, nextSteps: e.target.value})}
                className="input-field w-full h-20"
                placeholder="Next steps..."
              />
            </div>
          </div>

          <div className="flex justify-end mt-6">
            <button
              onClick={handleSaveReportConfig}
              className={`btn-primary ${isReportExpired ? 'opacity-50 cursor-not-allowed' : ''}`}
              disabled={isReportExpired}
            >
              {isReportExpired ? 'Cannot Save - Report Expired' : 'Save Configuration'}
            </button>
          </div>
        </div>
      </div>
    )
  }

  if (showDetailView && selectedTestCases[currentTestCaseIndex]) {
    const currentTestCase = selectedTestCases[currentTestCaseIndex]
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
                currentTestCase.status === 'NOT_STARTED' ? 'bg-red-500/20 text-red-400' : 'bg-green-500/20 text-green-400'
              }`}>
                {currentTestCase.status}
              </span>
              <div className="flex items-center space-x-2">
                <button
                  onClick={async () => {
                    const newStatus = 'OPEN';
                    // Update the selectedTestCases array directly for instant UI feedback
                    const updatedTestCases = [...selectedTestCases];
                    updatedTestCases[currentTestCaseIndex] = {...currentTestCase, vulnerabilityStatus: newStatus};
                    setSelectedTestCases(updatedTestCases);
                    try {
                      await vaptReportsAPI.updateTestCase(currentTestCase.id, {
                        description: document.getElementById('description').value,
                        testProcedure: document.getElementById('testProcedure').value,
                        vulnerabilityStatus: newStatus
                      });
                      toast.success('Vulnerability status updated to Open');
                      // Immediately fetch fresh data from server to ensure consistency
                      await fetchTestCases(vaptReport.id);
                    } catch (error) {
                      console.error('Failed to update vulnerability status:', error);
                      toast.error('Failed to update vulnerability status');
                      // Revert on error
                      const revertedTestCases = [...selectedTestCases];
                      revertedTestCases[currentTestCaseIndex] = {...currentTestCase, vulnerabilityStatus: currentTestCase.vulnerabilityStatus};
                      setSelectedTestCases(revertedTestCases);
                    }
                  }}
                  className={`px-4 py-2 rounded-lg font-medium transition-all duration-200 ${
                    currentTestCase.vulnerabilityStatus === 'OPEN'
                      ? 'bg-red-500 text-white shadow-lg transform scale-105'
                      : 'bg-red-500/20 text-red-400 hover:bg-red-500/30 border border-red-500/50'
                  }`}
                >
                  🔴 OPEN
                </button>
                <button
                  onClick={async () => {
                    const newStatus = 'CLOSED';
                    // Update the selectedTestCases array directly for instant UI feedback
                    const updatedTestCases = [...selectedTestCases];
                    updatedTestCases[currentTestCaseIndex] = {...currentTestCase, vulnerabilityStatus: newStatus};
                    setSelectedTestCases(updatedTestCases);
                    try {
                      await vaptReportsAPI.updateTestCase(currentTestCase.id, {
                        description: document.getElementById('description').value,
                        testProcedure: document.getElementById('testProcedure').value,
                        vulnerabilityStatus: newStatus
                      });
                      toast.success('Vulnerability status updated to Closed');
                      // Immediately fetch fresh data from server to ensure consistency
                      await fetchTestCases(vaptReport.id);
                    } catch (error) {
                      console.error('Failed to update vulnerability status:', error);
                      toast.error('Failed to update vulnerability status');
                      // Revert on error
                      const revertedTestCases = [...selectedTestCases];
                      revertedTestCases[currentTestCaseIndex] = {...currentTestCase, vulnerabilityStatus: currentTestCase.vulnerabilityStatus};
                      setSelectedTestCases(revertedTestCases);
                    }
                  }}
                  className={`px-4 py-2 rounded-lg font-medium transition-all duration-200 ${
                    currentTestCase.vulnerabilityStatus === 'CLOSED'
                      ? 'bg-green-500 text-white shadow-lg transform scale-105'
                      : 'bg-green-500/20 text-green-400 hover:bg-green-500/30 border border-green-500/50'
                  }`}
                >
                  🟢 CLOSED
                </button>
              </div>
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
        <div className="flex items-center space-x-4">
          <button
            onClick={() => navigate('/dashboard')}
            className="btn-secondary flex items-center"
          >
            ← Back to Dashboard
          </button>
          <h1 className="text-2xl font-bold text-white flex items-center">
            <Shield className="w-6 h-6 mr-2" />
            VAPT Report
            {hasDownloadOnlyAccess && <span className="text-sm text-yellow-400 ml-2">(Download Only)</span>}
          </h1>
        </div>
      </div>

      {/* Selection Form */}
      {!hasDownloadOnlyAccess && (
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

        {/* Test Plan Selection */}
        {selectedClient && selectedApplication && availableTestPlans.length > 0 && (
          <div className="mt-6">
            <h3 className="text-lg font-semibold text-white mb-4">
              {isModifyMode ? 'Add More Test Cases' : 'Select Test Cases'}
              {isModifyMode && <span className="text-sm text-blue-400 ml-2">(Modify Mode - Changes will be appended)</span>}
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 max-h-96 overflow-y-auto">
              {availableTestPlans.map((testPlan) => {
                const isSelected = selectedTestPlanIds.includes(testPlan.id)
                const isAlreadyAdded = testCases.some(tc => tc.testPlan.id === testPlan.id)
                return (
                  <div key={testPlan.id} className={`p-4 rounded-lg border-2 transition-all ${
                    isSelected ? 'bg-dark-600 border-blue-500' :
                    isAlreadyAdded ? 'bg-gray-600 border-gray-500 opacity-60' : 'bg-dark-700 border-transparent'
                  }`}>
                    <div className="flex items-center justify-between mb-2">
                      <div className="flex items-center space-x-2">
                        <input
                          type="checkbox"
                          checked={isSelected}
                          disabled={isAlreadyAdded}
                          onChange={(e) => {
                            if (e.target.checked) {
                              setSelectedTestPlanIds([...selectedTestPlanIds, testPlan.id])
                            } else {
                              setSelectedTestPlanIds(selectedTestPlanIds.filter(id => id !== testPlan.id))
                            }
                          }}
                          className="w-4 h-4 text-blue-600 bg-gray-100 border-gray-300 rounded focus:ring-blue-500 disabled:opacity-50"
                        />
                        <span className="text-gray-400 font-mono text-sm">{testPlan.testCaseId}</span>
                        {isAlreadyAdded && <span className="text-green-400 text-xs">✓ Added</span>}
                      </div>
                      <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                        testPlan.severity === 'CRITICAL' ? 'bg-red-500/20 text-red-400' :
                        testPlan.severity === 'HIGH' ? 'bg-orange-500/20 text-orange-400' :
                        testPlan.severity === 'MEDIUM' ? 'bg-yellow-500/20 text-yellow-400' :
                        testPlan.severity === 'LOW' ? 'bg-blue-500/20 text-blue-400' :
                        'bg-gray-500/20 text-gray-400'
                      }`}>
                        {testPlan.severity}
                      </span>
                    </div>
                    <div>
                      <h4 className="text-white font-medium text-sm">{testPlan.vulnerabilityName}</h4>
                      <p className="text-gray-400 text-xs mt-1">{testPlan.description}</p>
                    </div>
                  </div>
                )
              })}
            </div>
            {isModifyMode && selectedTestPlanIds.length > 0 && (
              <div className="mt-4">
                <button
                  onClick={handleAddTestCases}
                  disabled={loading || isReportExpired}
                  className="btn-primary disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {loading ? 'Adding...' : `Add ${selectedTestPlanIds.length} Test Case${selectedTestPlanIds.length > 1 ? 's' : ''}`}
                </button>
              </div>
            )}
          </div>
        )}

        <div className="mt-4 flex space-x-4">
          <button
            onClick={handleInitializeReport}
            disabled={loading || !selectedClient || !selectedApplication || selectedTestPlanIds.length === 0}
            className="btn-primary disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {loading ? 'Loading...' : isModifyMode ? 'Update Report Configuration' : 'Submit & Start Testing'}
          </button>
          {isModifyMode && (
            <button
              onClick={() => {
                setIsModifyMode(false)
                setVaptReport(null)
                setTestCases([])
                setSelectedTestCases([])
                setSelectedTestPlanIds([])
                setSelectedClient('')
                setSelectedApplication('')
                setReportGenerated(false)
              }}
              className="btn-secondary"
            >
              Start New Report
            </button>
          )}
        </div>
        </div>
        )}

      {/* Vulnerability Summary */}
      {testCases.length > 0 && !showDetailView && !hasDownloadOnlyAccess && (
        <div className="card">
          <h2 className="text-lg font-semibold text-white mb-4">
            Vulnerability Summary
            {isModifyMode && <span className="text-sm text-blue-400 ml-2">(Modify Mode)</span>}
            {isReportExpired && <span className="text-sm text-red-400 ml-2">(Report Expired)</span>}
          </h2>

          {isReportExpired && (
            <div className="bg-red-500/10 border border-red-500/20 rounded-lg p-4 mb-4">
              <div className="flex items-center space-x-2">
                <span className="text-red-400 font-semibold">⚠️ Report Expired</span>
              </div>
              <p className="text-red-300 text-sm mt-1">
                "This report has expired. Please re-initiate the VAPT assessment."
              </p>
            </div>
          )}

          {/* Dynamic Summary Counts - Real-time updates */}
          <div className="flex items-center justify-between mb-4">
            <span className="text-sm text-gray-400">
              Last updated: {new Date(lastUpdate).toLocaleTimeString()}
            </span>
            <button
              onClick={async () => {
                if (vaptReport) {
                  await fetchTestCases(vaptReport.id)
                  setLastUpdate(Date.now())
                  toast.success('Data refreshed')
                }
              }}
              className="btn-secondary text-sm"
            >
              Refresh
            </button>
          </div>
          <div className="grid grid-cols-2 md:grid-cols-5 gap-4 mb-6">
            {(() => {
              const openTestCases = testCases.filter(tc => tc.vulnerabilityStatus === 'OPEN');
              const counts = {
                CRITICAL: openTestCases.filter(tc => tc.testPlan.severity === 'CRITICAL').length,
                HIGH: openTestCases.filter(tc => tc.testPlan.severity === 'HIGH').length,
                MEDIUM: openTestCases.filter(tc => tc.testPlan.severity === 'MEDIUM').length,
                LOW: openTestCases.filter(tc => tc.testPlan.severity === 'LOW').length,
                INFO: openTestCases.filter(tc => tc.testPlan.severity === 'INFO').length
              };

              return (
                <>
                  <div className="bg-red-500/10 border border-red-500/20 rounded-lg p-4 text-center">
                    <div className="text-2xl font-bold text-red-400">{counts.CRITICAL}</div>
                    <div className="text-sm text-gray-300">Critical</div>
                  </div>
                  <div className="bg-orange-500/10 border border-orange-500/20 rounded-lg p-4 text-center">
                    <div className="text-2xl font-bold text-orange-400">{counts.HIGH}</div>
                    <div className="text-sm text-gray-300">High</div>
                  </div>
                  <div className="bg-yellow-500/10 border border-yellow-500/20 rounded-lg p-4 text-center">
                    <div className="text-2xl font-bold text-yellow-400">{counts.MEDIUM}</div>
                    <div className="text-sm text-gray-300">Medium</div>
                  </div>
                  <div className="bg-blue-500/10 border border-blue-500/20 rounded-lg p-4 text-center">
                    <div className="text-2xl font-bold text-blue-400">{counts.LOW}</div>
                    <div className="text-sm text-gray-300">Low</div>
                  </div>
                  <div className="bg-gray-500/10 border border-gray-500/20 rounded-lg p-4 text-center">
                    <div className="text-2xl font-bold text-gray-400">{counts.INFO}</div>
                    <div className="text-sm text-gray-300">Info</div>
                  </div>
                </>
              );
            })()}
          </div>
        </div>
      )}

      {/* Test Cases Overview */}
      {testCases.length > 0 && !showDetailView && !hasDownloadOnlyAccess && (
        <div className="card">
          <h2 className="text-lg font-semibold text-white mb-4">
            Test Case Details
            {isModifyMode && <span className="text-sm text-blue-400 ml-2">(Modify Mode)</span>}
            {isReportExpired && <span className="text-sm text-red-400 ml-2">(Report Expired)</span>}
          </h2>

          {isReportExpired && (
            <div className="bg-red-500/10 border border-red-500/20 rounded-lg p-4 mb-4">
              <div className="flex items-center space-x-2">
                <span className="text-red-400 font-semibold">⚠️ Report Expired</span>
              </div>
              <p className="text-red-300 text-sm mt-1">
                "This report has expired. Please re-initiate the VAPT assessment."
              </p>
            </div>
          )}
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
                  <div className="flex items-center space-x-2">
                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                      testCase.vulnerabilityStatus === 'OPEN'
                        ? 'bg-red-500/20 text-red-400'
                        : 'bg-green-500/20 text-green-400'
                    }`}>
                      {testCase.vulnerabilityStatus === 'OPEN' ? 'Open' : 'Closed'}
                    </span>
                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                      testCase.status === 'NOT_STARTED'
                        ? 'bg-gray-500/20 text-gray-400'
                        : testCase.status === 'PASSED'
                        ? 'bg-green-500/20 text-green-400'
                        : 'bg-yellow-500/20 text-yellow-400'
                    }`}>
                      {testCase.status}
                    </span>
                  </div>
                </div>
                <div>
                  <h3 className="text-white font-medium text-sm">{testCase.testPlan.vulnerabilityName}</h3>
                  <p className="text-gray-400 text-xs">TP-{testCase.testPlan.testCaseId}</p>
                </div>
              </div>
            ))}
          </div>

          <div className="mt-6 flex space-x-4">
            <button
              onClick={handleConfigureReport}
              className={`btn-secondary ${isReportExpired ? 'opacity-50 cursor-not-allowed' : ''}`}
              disabled={isReportExpired}
            >
              Configure Report Details
            </button>
            <button
              onClick={handlePrepareReport}
              className={`btn-primary ${isReportExpired ? 'opacity-50 cursor-not-allowed' : ''}`}
              disabled={isReportExpired}
            >
              Prepare Report
            </button>
          </div>
        </div>
      )}

      {/* Report Generation */}
      {vaptReport && (
        <div className="card">
          <h2 className="text-lg font-semibold text-white mb-4">
            {hasDownloadOnlyAccess ? 'Download Reports' : 'Download Final Report'}
            {isModifyMode && <span className="text-sm text-blue-400 ml-2">(Modify Mode)</span>}
            {isReportExpired && <span className="text-sm text-red-400 ml-2">(Report Expired)</span>}
          </h2>

          {isReportExpired && (
            <div className="bg-red-500/10 border border-red-500/20 rounded-lg p-4 mb-4">
              <div className="flex items-center space-x-2">
                <span className="text-red-400 font-semibold">⚠️ Report Expired</span>
              </div>
              <p className="text-red-300 text-sm mt-1">
                "This report has expired. Please re-initiate the VAPT assessment."
              </p>
            </div>
          )}
          {!reportGenerated && (
            <p className="text-gray-400 mb-4">Please complete the test case details and generate the report first.</p>
          )}
          {reportGenerated && (
            <div className="bg-green-500/10 border border-green-500/20 rounded-lg p-3 mb-4">
              <p className="text-green-400 text-sm">✓ Report has been generated and is ready for download</p>
            </div>
          )}
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
                  if (error.response?.status === 403) {
                    toast.error('Access denied: You are not assigned to this client/application')
                  } else {
                    toast.error('Failed to download DOCX report')
                  }
                }
              }}
              disabled={!reportGenerated || isReportExpired}
              className={`btn-secondary flex items-center disabled:opacity-50 disabled:cursor-not-allowed ${isReportExpired ? 'cursor-not-allowed' : ''}`}
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
                  if (error.response?.status === 403) {
                    toast.error('Access denied: You are not assigned to this client/application')
                  } else {
                    toast.error('Failed to download PDF report')
                  }
                }
              }}
              disabled={!reportGenerated || isReportExpired}
              className={`btn-secondary flex items-center disabled:opacity-50 disabled:cursor-not-allowed ${isReportExpired ? 'cursor-not-allowed' : ''}`}
            >
              <Download className="w-4 h-4 mr-2" />
              Download PDF
            </button>
            {(currentUserRole === 'ADMIN' || currentUserRole === 'TESTER') && !hasDownloadOnlyAccess && (
              <button
                onClick={async () => {
                  try {
                    const response = await vaptReportsAPI.getHtmlReport(vaptReport.id)
                    const htmlContent = response.data.html
                    // Open HTML in new tab
                    const newWindow = window.open('', '_blank')
                    newWindow.document.write(htmlContent)
                    newWindow.document.close()
                    toast.success('HTML report opened in new tab')
                  } catch (error) {
                    console.error('Failed to get HTML report:', error)
                    toast.error('Failed to get HTML report')
                  }
                }}
                disabled={!reportGenerated || isReportExpired}
                className={`btn-secondary flex items-center disabled:opacity-50 disabled:cursor-not-allowed ${isReportExpired ? 'cursor-not-allowed' : ''}`}
              >
                <FileText className="w-4 h-4 mr-2" />
                View HTML Report
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

export default VaptReports