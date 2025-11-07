# Post-Deploy Verification Checklist for RBAC Implementation

## Database Migration
- [ ] Run `migration_enhance_rbac.sql` to create user_client_assignments table and sample data
- [ ] Verify PM_DM role exists in users table
- [ ] Confirm user_client_assignments table has proper foreign key constraints
- [ ] Check that indexes are created for performance

## Backend Verification
- [ ] Start the Spring Boot application
- [ ] Check application logs for any AuthorizationFilter errors
- [ ] Verify SecurityConfig loads without issues

## Frontend Verification
- [ ] Start the React development server
- [ ] Login with different user roles and verify menu visibility
- [ ] Test route access restrictions

## Permission Testing
- [ ] Run the test script: `chmod +x test_rbac_permissions.sh && ./test_rbac_permissions.sh`
- [ ] Verify all tests pass as expected

## Manual Testing Checklist

### ADMIN User Tests
- [ ] Can access all pages: Dashboard, User Registration, User Management, Clients, Applications, Test Plans, VAPT Reports, Defects
- [ ] Can perform all CRUD operations on all entities
- [ ] Can download all VAPT reports

### PM_DM User Tests
- [ ] Can access: Dashboard, Clients (read-only), Applications (read-only), VAPT Reports (list + download)
- [ ] Cannot access: User Registration, User Management, Test Plans, Defects
- [ ] Cannot create/edit clients or applications
- [ ] Can only download VAPT reports for assigned clients/applications
- [ ] Gets 403 when trying to download unassigned reports

### TESTER User Tests
- [ ] Can access: Dashboard, Applications (read-only), Test Plans (full), VAPT Reports (full)
- [ ] Cannot access: User Registration, User Management, Clients, Defects
- [ ] Can create, modify, and generate VAPT reports
- [ ] Can view HTML reports in browser

### CLIENT User Tests
- [ ] Can only access: Dashboard
- [ ] Cannot access any other pages (should redirect to dashboard)
- [ ] No access to any management or testing features

## Security Verification
- [ ] Verify that direct API calls respect role permissions
- [ ] Test that unauthorized users get 403 responses
- [ ] Confirm that sensitive data is properly protected
- [ ] Check that JWT tokens contain correct role information

## Rollback Plan
If issues are found:
1. Run `rollback_rbac_changes.sql` to remove database changes
2. Revert code changes in AuthorizationFilter.java
3. Revert frontend changes in Sidebar.jsx, App.jsx, and VaptReports.jsx
4. Restart both backend and frontend services

## Performance Check
- [ ] Verify that authorization checks don't significantly impact response times
- [ ] Check database query performance with user_client_assignments joins
- [ ] Monitor memory usage with additional authorization logic

## Final Sign-off
- [ ] All automated tests pass
- [ ] Manual testing completed successfully
- [ ] No security vulnerabilities introduced
- [ ] Performance is acceptable
- [ ] Rollback procedures documented and tested