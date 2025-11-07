# Defect Tracker - VAPT Cybersecurity Portal

A comprehensive full-stack web application for managing cybersecurity testing defects, built with modern technologies and enterprise-grade features.

## 🚀 Features

### Core Functionality
- **Multi-role Authentication**: Admin, Tester, and Client roles with JWT-based security
- **Client Management**: Add and manage client organizations
- **Application Management**: Track applications per client with environment details
- **Test Plan Management**: Create and manage vulnerability test cases
- **Defect Tracking**: Comprehensive defect lifecycle management
- **Dashboard Analytics**: Real-time statistics and charts
- **Status History**: Complete audit trail for defect changes

### Technical Features
- **Modern UI/UX**: Dark cyber-themed interface with glass-morphism effects
- **Responsive Design**: Mobile-first approach with adaptive layouts
- **Real-time Charts**: Interactive data visualization with Recharts
- **File Uploads**: Support for proof-of-concept files and documentation
- **Search & Filter**: Advanced filtering across all modules
- **Pagination**: Efficient data loading and navigation
- **RESTful APIs**: Well-documented API endpoints

## 🛠️ Technology Stack

### Backend
- **Java 17** with **Spring Boot 3.2**
- **Spring Security** with JWT authentication
- **Spring Data JPA** for database operations
- **MySQL 8.0** database
- **Maven** for dependency management
- **Swagger/OpenAPI** for API documentation

### Frontend
- **React 18** with modern hooks
- **Vite** for fast development and building
- **Tailwind CSS** for styling
- **React Router** for navigation
- **Axios** for API communication
- **Recharts** for data visualization
- **Framer Motion** for animations
- **React Hot Toast** for notifications

### DevOps
- **Nginx** for production serving
- **Multi-stage builds** for optimized images

## 📁 Project Structure

```
defect-tracker/
├── backend/                 # Spring Boot application
│   ├── src/main/java/com/defecttracker/
│   │   ├── config/         # Security and configuration
│   │   ├── controller/     # REST API endpoints
│   │   ├── entity/         # JPA entities
│   │   ├── repository/     # Data access layer
│   │   ├── service/        # Business logic
│   │   └── util/           # Utility classes
│   ├── src/main/resources/  # Application properties
│   └── pom.xml            # Maven configuration
├── frontend/               # React application
│   ├── src/
│   │   ├── components/     # Reusable UI components
│   │   ├── pages/         # Page components
│   │   ├── hooks/         # Custom React hooks
│   │   ├── services/      # API service functions
│   │   └── utils/         # Utility functions
│   ├── package.json       # Dependencies
│   └── vite.config.js     # Vite configuration
├── database/              # Database schema and docs
│   ├── schema.sql         # MySQL schema
│   └── er_diagram.md      # Entity relationships
└── docs/                  # Documentation
    └── README.md          # This file
```

## 🚀 Quick Start

### Prerequisites
- **Node.js 18+** (for frontend development)
- **Java 17** (for backend development)
- **MySQL 8.0** (installed locally)

### Local Development Setup (Recommended)

1. **Install MySQL 8.0 locally**
   - Download and install MySQL 8.0 from https://dev.mysql.com/downloads/mysql/
   - During installation, set root password to: `MounikaRam@939512`
   - Create database: `defect_tracker`

2. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd defect-tracker
   ```

3. **Initialize database (optional - Spring Boot will auto-create tables)**
   ```bash
   # If you want to manually initialize the schema
   mysql -u root -p defect_tracker < database/schema.sql
   mysql -u root -p defect_tracker < backend/src/main/resources/data.sql
   ```

4. **Backend setup**
   ```bash
   cd backend
   mvn clean install
   mvn spring-boot:run
   ```

5. **Frontend setup**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

6. **Access the application**
   - Frontend: http://localhost:3001
   - Backend API: http://localhost:8081/api
   - API Documentation: http://localhost:8081/api/swagger-ui.html


## 🔐 Default Credentials

- **Username**: admin
- **Password**: password
- **Role**: Administrator

## 📊 Database Schema

The application uses MySQL with the following main entities:

- **Users**: Authentication and role management
- **Clients**: Client organizations
- **Applications**: Client applications with environment details
- **Test Plans**: Vulnerability test cases
- **Defects**: Security findings with full lifecycle tracking
- **Defect History**: Audit trail for status changes
- **Reports**: Generated analytics reports

See `database/er_diagram.md` for detailed entity relationships.

## 🔧 API Endpoints

### Authentication
- `POST /api/auth/login` - User login
- `POST /api/auth/register` - User registration
- `GET /api/auth/validate` - Token validation

### Dashboard
- `GET /api/dashboard/stats` - Dashboard statistics
- `GET /api/dashboard/defects-by-application` - Application-wise defects
- `GET /api/dashboard/monthly-trends` - Monthly trends
- `GET /api/dashboard/clients-most-defects` - Client defect rankings

### Clients
- `GET /api/clients` - List all clients
- `POST /api/clients` - Create client
- `PUT /api/clients/{id}` - Update client
- `DELETE /api/clients/{id}` - Delete client

### Applications
- `GET /api/applications` - List all applications
- `POST /api/applications` - Create application
- `PUT /api/applications/{id}` - Update application
- `DELETE /api/applications/{id}` - Delete application

### Test Plans
- `GET /api/test-plans` - List all test plans
- `POST /api/test-plans` - Create test plan
- `PUT /api/test-plans/{id}` - Update test plan
- `DELETE /api/test-plans/{id}` - Delete test plan

### Defects
- `GET /api/defects` - List all defects
- `POST /api/defects` - Create defect
- `PUT /api/defects/{id}` - Update defect
- `DELETE /api/defects/{id}` - Delete defect
- `GET /api/defects/{id}/history` - Defect history

## 🎨 UI/UX Design

### Design System
- **Dark Theme**: Cyber-inspired color palette
- **Glass Morphism**: Modern backdrop blur effects
- **Responsive Grid**: Adaptive layouts for all screen sizes
- **Smooth Animations**: Framer Motion transitions
- **Status Indicators**: Color-coded severity and status badges

### Navigation
- **Left Sidebar**: Collapsible navigation with icons
- **Top Header**: User info and notifications
- **Breadcrumb**: Clear navigation context

### Data Tables
- **Search & Filter**: Real-time filtering capabilities
- **Pagination**: Efficient data loading
- **Sort Options**: Column-based sorting
- **Action Menus**: Context-aware operations

## 🔒 Security Features

- **JWT Authentication**: Stateless token-based auth
- **Role-based Access**: Admin, Tester, Client permissions
- **Password Encryption**: BCrypt hashing
- **CORS Configuration**: Secure cross-origin requests
- **Input Validation**: Comprehensive data validation
- **SQL Injection Protection**: Parameterized queries

## 📈 Performance Optimizations

- **Database Indexing**: Optimized queries with proper indexing
- **Lazy Loading**: Efficient data fetching
- **Pagination**: Server-side pagination for large datasets
- **Caching**: Strategic caching for frequently accessed data
- **Compression**: Gzip compression for responses

## 🚀 Deployment

### Production Deployment

1. **Build and deploy the application**
   - Configure your production environment with MySQL database
   - Set appropriate environment variables for database connection, JWT secrets, etc.
   - Deploy backend and frontend to your preferred hosting platform

### Environment Variables

Create `.env` file for production:

```env
# Database
MYSQL_ROOT_PASSWORD=your_secure_password
MYSQL_USER=defectuser
MYSQL_PASSWORD=your_secure_password

# JWT
JWT_SECRET=your_256_bit_secret_key_here
JWT_EXPIRATION=86400000

# Email (optional)
SPRING_MAIL_USERNAME=your-email@gmail.com
SPRING_MAIL_PASSWORD=your-app-password
```

## 🔮 Future Enhancements

### AI Integration
- **Auto Classification**: ML-based defect severity prediction
- **Smart Recommendations**: AI-powered test case suggestions
- **Trend Analysis**: Predictive analytics for vulnerability patterns

### Advanced Features
- **Real-time Notifications**: WebSocket-based updates
- **Advanced Reporting**: PDF/Excel export with charts
- **API Integration**: Third-party security tool integration
- **Audit Logging**: Comprehensive security audit trails
- **Multi-tenancy**: Organization-based data isolation

### Mobile App
- **React Native**: Cross-platform mobile application
- **Offline Support**: Local data synchronization
- **Push Notifications**: Real-time alerts

### DevOps Enhancements
- **CI/CD Pipeline**: Automated testing and deployment
- **Monitoring**: Application performance monitoring
- **Backup Strategy**: Automated database backups
- **Load Balancing**: Horizontal scaling support

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 📞 Support

For support and questions:
- Create an issue in the repository
- Contact the development team
- Check the documentation in the `docs/` folder

## 🙏 Acknowledgments

- Spring Boot and React communities
- Open source contributors
- Cybersecurity professionals for domain expertise

---

**Built with ❤️ for the cybersecurity community**