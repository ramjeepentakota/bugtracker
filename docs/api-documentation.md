# Defect Tracker API Documentation

## Overview

The Defect Tracker API provides RESTful endpoints for managing cybersecurity testing defects. All endpoints require JWT authentication except for authentication endpoints.

## Authentication

### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "username": "admin",
    "email": "admin@defecttracker.com",
    "role": "ADMIN",
    "firstName": "System",
    "lastName": "Administrator"
  }
}
```

### Register
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "newuser",
  "email": "user@example.com",
  "passwordHash": "password123",
  "role": "TESTER",
  "firstName": "John",
  "lastName": "Doe"
}
```

### Validate Token
```http
GET /api/auth/validate
Authorization: Bearer <token>
```

## Dashboard

### Get Dashboard Statistics
```http
GET /api/dashboard/stats
Authorization: Bearer <token>
```

**Response:**
```json
{
  "totalClients": 5,
  "totalApplications": 12,
  "totalTestPlans": 25,
  "totalDefects": 45,
  "openDefects": 18,
  "closedDefects": 27,
  "defectsBySeverity": {
    "CRITICAL": 3,
    "HIGH": 8,
    "MEDIUM": 15,
    "LOW": 12,
    "INFO": 7
  },
  "defectsByStatus": {
    "NEW": 5,
    "OPEN": 8,
    "IN_PROGRESS": 5,
    "RETEST": 3,
    "CLOSED": 24
  }
}
```

## Clients

### List Clients
```http
GET /api/clients
Authorization: Bearer <token>
```

### Create Client
```http
POST /api/clients
Authorization: Bearer <token>
Content-Type: application/json

{
  "clientName": "TechCorp Inc.",
  "companyName": "TechCorp",
  "email": "contact@techcorp.com",
  "contactNumber": "+1-555-0123",
  "description": "Leading technology company"
}
```

### Update Client
```http
PUT /api/clients/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "clientName": "Updated TechCorp Inc.",
  "companyName": "TechCorp",
  "email": "contact@techcorp.com",
  "contactNumber": "+1-555-0123",
  "description": "Updated description"
}
```

### Delete Client
```http
DELETE /api/clients/{id}
Authorization: Bearer <token>
```

### Search Clients
```http
GET /api/clients/search?query=tech
Authorization: Bearer <token>
```

## Applications

### List Applications
```http
GET /api/applications
Authorization: Bearer <token>
```

### Create Application
```http
POST /api/applications
Authorization: Bearer <token>
Content-Type: application/json

{
  "applicationName": "E-commerce Platform",
  "clientId": 1,
  "environment": "PRODUCTION",
  "technologyStack": "React, Node.js, MongoDB"
}
```

### Get Applications by Client
```http
GET /api/applications/client/{clientId}
Authorization: Bearer <token>
```

### Update Application
```http
PUT /api/applications/{id}
Authorization: Bearer <token>
```

### Delete Application
```http
DELETE /api/applications/{id}
Authorization: Bearer <token>
```

## Test Plans

### List Test Plans
```http
GET /api/test-plans
Authorization: Bearer <token>
```

### Create Test Plan
```http
POST /api/test-plans
Authorization: Bearer <token>
Content-Type: application/json

{
  "vulnerabilityName": "SQL Injection",
  "severity": "CRITICAL",
  "description": "Database injection vulnerability",
  "testProcedure": "Test input fields with SQL payloads: ' OR 1=1 --"
}
```

### Get Test Plan by Test Case ID
```http
GET /api/test-plans/test-case/{testCaseId}
Authorization: Bearer <token>
```

### Update Test Plan
```http
PUT /api/test-plans/{id}
Authorization: Bearer <token>
```

### Delete Test Plan
```http
DELETE /api/test-plans/{id}
Authorization: Bearer <token>
```

### Search Test Plans
```http
GET /api/test-plans/search?query=sql
Authorization: Bearer <token>
```

## Defects

### List Defects
```http
GET /api/defects
Authorization: Bearer <token>
```

### Create Defect
```http
POST /api/defects
Authorization: Bearer <token>
Content-Type: application/json

{
  "clientId": 1,
  "applicationId": 1,
  "testPlanId": 1,
  "description": "SQL injection vulnerability found in login form",
  "testingProcedure": "Injected ' OR 1=1 -- in username field",
  "assignedToId": 2,
  "status": "OPEN"
}
```

### Get Defects by Client
```http
GET /api/defects/client/{clientId}
Authorization: Bearer <token>
```

### Update Defect
```http
PUT /api/defects/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "status": "IN_PROGRESS",
  "assignedToId": 3
}
```

### Delete Defect
```http
DELETE /api/defects/{id}
Authorization: Bearer <token>
```

### Get Defect History
```http
GET /api/defects/{id}/history
Authorization: Bearer <token>
```

**Response:**
```json
[
  {
    "id": 1,
    "defect": { "id": 1, "defectId": "DEF-001" },
    "oldStatus": null,
    "newStatus": "NEW",
    "changedBy": { "id": 1, "username": "admin" },
    "changeReason": "Defect created",
    "changedAt": "2024-01-15T10:30:00Z"
  },
  {
    "id": 2,
    "defect": { "id": 1, "defectId": "DEF-001" },
    "oldStatus": "NEW",
    "newStatus": "OPEN",
    "changedBy": { "id": 2, "username": "tester1" },
    "changeReason": "Status updated",
    "changedAt": "2024-01-15T11:00:00Z"
  }
]
```

## Error Responses

All endpoints return appropriate HTTP status codes:

- `200` - Success
- `201` - Created
- `400` - Bad Request
- `401` - Unauthorized
- `403` - Forbidden
- `404` - Not Found
- `500` - Internal Server Error

**Error Response Format:**
```json
{
  "error": "Error message description",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/defects",
  "status": 400
}
```

## Pagination

Endpoints that return lists support pagination:

```http
GET /api/defects/paged?page=0&size=10
GET /api/clients/paged?page=1&size=20
```

**Paginated Response:**
```json
{
  "content": [...],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "sorted": false,
      "empty": true
    }
  },
  "totalElements": 45,
  "totalPages": 5,
  "first": true,
  "last": false,
  "size": 10,
  "number": 0,
  "numberOfElements": 10,
  "empty": false
}
```

## File Uploads

Defects support file uploads for proof-of-concept files:

```http
POST /api/defects
Authorization: Bearer <token>
Content-Type: multipart/form-data

# Include pocFile in FormData along with other defect data
```

## Rate Limiting

API endpoints are rate limited to prevent abuse:
- Authenticated requests: 1000 requests per hour
- Unauthenticated requests: 100 requests per hour

## CORS

The API supports cross-origin requests from configured origins. All endpoints include appropriate CORS headers.

## Data Validation

All endpoints validate input data:
- Required fields are enforced
- Email formats are validated
- String lengths are checked
- Enum values are validated
- Foreign key relationships are verified

## Security

- All sensitive endpoints require JWT authentication
- Passwords are hashed using BCrypt
- SQL injection protection via parameterized queries
- XSS protection via input sanitization
- CSRF protection enabled
- Security headers included in responses