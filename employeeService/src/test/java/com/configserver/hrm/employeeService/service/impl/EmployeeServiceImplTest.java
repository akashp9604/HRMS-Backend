package com.configserver.hrm.employeeService.service.impl;

import com.configserver.hrm.employeeService.client.AttendanceClient;
import com.configserver.hrm.employeeService.dto.*;
import com.configserver.hrm.employeeService.entity.Employee;
import com.configserver.hrm.employeeService.entity.Role;
import com.configserver.hrm.employeeService.exception.EmployeeException;
import com.configserver.hrm.employeeService.mail.EmailService;
import com.configserver.hrm.employeeService.repository.EmployeeRepository;
import com.configserver.hrm.employeeService.util.PasswordGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordGenerator passwordGenerator;

    @Mock
    private AttendanceClient attendanceClient;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private Employee mockEmployee;
    private EmployeeDTO employeeDTO;
    private Long existingId;
    private Long nonExistentId;

    private final String RAW_PASSWORD = "GeneratedPass123!";
    private final String HASHED_PASSWORD = "$2a$10$hashedPassword123";

    //Register first admin when no employees exist
    @Test
    void registerEmployee_FirstAdmin_Success() {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setEmail("admin@example.com");
        dto.setRole(Role.ADMIN);

        when(repository.findByEmail(dto.getEmail())).thenReturn(Optional.empty());
        when(repository.count()).thenReturn(0L);

        employeeService.registerEmployee(dto);

        verify(repository).save(any(Employee.class));
        verify(emailService).sendPasswordEmail(anyString(), anyString());
    }

    //Register a non-admin employee successfully
    @Test
    void registerEmployee_ValidNonAdmin_Success() {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setEmail("john@example.com");
        dto.setRole(Role.EMPLOYEE);
        dto.setName("John Doe");
        // set other fields...

        when(repository.findByEmail(dto.getEmail())).thenReturn(Optional.empty());
        when(repository.count()).thenReturn(5L); // not first admin
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPass");

        employeeService.registerEmployee(dto);

        verify(repository).save(any(Employee.class));
        verify(emailService).sendPasswordEmail(eq(dto.getEmail()), anyString());
    }

    //Email already exists
    @Test
    void registerEmployee_EmailAlreadyExists_ThrowsException() {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setEmail("existing@example.com");
        dto.setRole(Role.EMPLOYEE);

        Employee emp = new Employee();
        emp.setEmail("existing@example.com");
        emp.setRole(Role.EMPLOYEE);

        when(repository.findByEmail(dto.getEmail()))
                .thenReturn(Optional.of(new Employee()));

        assertThrows(EmployeeException.class, () -> {
            employeeService.registerEmployee(dto);
        });

        verify(repository, never()).save(any());
        verify(emailService, never()).sendPasswordEmail(any(), any());
    }

    //Non-admin tries to register another admin (no existing admins)
    @Test
    void registerEmployee_NonAdminRegisterAdmin_WhenNoAdminExists_ThrowsException() {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setEmail("newadmin@example.com");
        dto.setRole(Role.ADMIN);

        when(repository.findByEmail(dto.getEmail())).thenReturn(Optional.empty());
        when(repository.count()).thenReturn(3L); // employees exist but none are admin (logic flaw?)

        assertThrows(EmployeeException.class, () -> {
            employeeService.registerEmployee(dto);
        });
    }



    //Get all employees when repository has employees
    @Test
    void getAllEmployees_EmployeesExist_ReturnsEmployeeList() {
        // Arrange
        Employee emp1 = new Employee();
        emp1.setId(1L);
        emp1.setName("John Doe");
        emp1.setEmail("john@example.com");

        Employee emp2 = new Employee();
        emp2.setId(2L);
        emp2.setName("Jane Smith");
        emp2.setEmail("jane@example.com");

        List<Employee> expectedEmployees = Arrays.asList(emp1, emp2);

        when(repository.findAll()).thenReturn(expectedEmployees);

        // Act
        List<Employee> actualEmployees = employeeService.getAllEmployees();

        // Assert
        assertNotNull(actualEmployees);
        assertEquals(2, actualEmployees.size());
        assertEquals("John Doe", actualEmployees.get(0).getName());
        assertEquals("Jane Smith", actualEmployees.get(1).getName());
        verify(repository, times(1)).findAll();
    }

    //Get all employees when repository returns empty list
    @Test
    void getAllEmployees_NoEmployeesExist_ReturnsEmptyList() {
        // Arrange
        when(repository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<Employee> actualEmployees = employeeService.getAllEmployees();

        // Assert
        assertNotNull(actualEmployees);
        assertTrue(actualEmployees.isEmpty());
        verify(repository, times(1)).findAll();
    }

    //Repository throws runtime exception
    @Test
    void getAllEmployees_RepositoryThrowsException_PropagatesException() {
        // Arrange
        when(repository.findAll()).thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            employeeService.getAllEmployees();
        });

        assertEquals("Database connection failed", exception.getMessage());
        verify(repository, times(1)).findAll();
    }

    // Repository throws data access exception
    @Test
    void getAllEmployees_DataAccessException_ThrowsException() {
        // Arrange
        when(repository.findAll()).thenThrow(new DataAccessException("Database error") {});

        // Act & Assert
        assertThrows(DataAccessException.class, () -> {
            employeeService.getAllEmployees();
        });
    }


    //Get employee by valid ID - success
    @Test
    void getEmployeeById_ValidId_ReturnsEmployee() {
        // Arrange
        Long employeeId = 100L;
        Employee expectedEmployee = new Employee();
        expectedEmployee.setId(employeeId);
        expectedEmployee.setName("John Doe");
        expectedEmployee.setEmail("john@example.com");
        expectedEmployee.setRole(Role.EMPLOYEE);

        when(repository.findById(employeeId)).thenReturn(Optional.of(expectedEmployee));

        // Act
        Employee actualEmployee = employeeService.getEmployeeById(employeeId);

        // Assert
        assertNotNull(actualEmployee);
        assertEquals(employeeId, actualEmployee.getId());
        assertEquals("John Doe", actualEmployee.getName());
        assertEquals("john@example.com", actualEmployee.getEmail());
        verify(repository, times(1)).findById(employeeId);
    }

    //Get employee with all fields populated
    @Test
    void getEmployeeById_ValidIdWithAllFields_ReturnsCompleteEmployee() {
        // Arrange
        Long employeeId = 1L;
        Employee employee = new Employee();
        employee.setId(employeeId);
        employee.setName("Jane Smith");
        employee.setEmail("jane@company.com");
        employee.setRole(Role.ADMIN);
        employee.setDesignation("Senior Manager");
        employee.setDepartment("IT");
        employee.setAnnualSalary(95000.0);
        employee.setStatus("ACTIVE");
        employee.setDateOfJoining(LocalDate.of(2020, 1, 15));

        when(repository.findById(employeeId)).thenReturn(Optional.of(employee));

        // Act
        Employee result = employeeService.getEmployeeById(employeeId);

        // Assert
        assertAll("Employee properties",
                () -> assertEquals(employeeId, result.getId()),
                () -> assertEquals("Jane Smith", result.getName()),
                () -> assertEquals("jane@company.com", result.getEmail()),
                () -> assertEquals(Role.ADMIN, result.getRole()),
                () -> assertEquals("Senior Manager", result.getDesignation()),
                () -> assertEquals("IT", result.getDepartment()),
                () -> assertEquals(95000.0, result.getAnnualSalary()),
                () -> assertEquals("ACTIVE", result.getStatus()),
                () -> assertEquals(LocalDate.of(2020, 1, 15), result.getDateOfJoining())
        );
    }

    //Employee not found - throws custom exception
    @Test
    void getEmployeeById_EmployeeNotFound_ThrowsEmployeeException() {
        // Arrange
        Long nonExistentId = 999L;

        when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        EmployeeException exception = assertThrows(EmployeeException.class, () -> {
            employeeService.getEmployeeById(nonExistentId);
        });

        assertEquals("Employee not found with id: " + nonExistentId, exception.getMessage());
        verify(repository, times(1)).findById(nonExistentId);
    }

    //Negative ID parameter
    @Test
    void getEmployeeById_NegativeId_ThrowsEmployeeException() {
        // Arrange
        Long negativeId = -1L;

        when(repository.findById(negativeId)).thenReturn(Optional.empty());

        // Act & Assert
        EmployeeException exception = assertThrows(EmployeeException.class, () -> {
            employeeService.getEmployeeById(negativeId);
        });

        assertEquals("Employee not found with id: " + negativeId, exception.getMessage());
    }

    //Zero ID parameter
    @Test
    void getEmployeeById_ZeroId_ThrowsEmployeeException() {
        // Arrange
        Long zeroId = 0L;

        when(repository.findById(zeroId)).thenReturn(Optional.empty());

        // Act & Assert
        EmployeeException exception = assertThrows(EmployeeException.class, () -> {
            employeeService.getEmployeeById(zeroId);
        });

        assertEquals("Employee not found with id: " + zeroId, exception.getMessage());
    }

    //Repository throws database exception
    @Test
    void getEmployeeById_RepositoryThrowsException_PropagatesException() {
        // Arrange
        Long employeeId = 100L;

        when(repository.findById(employeeId)).thenThrow(new RuntimeException("Database connection error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            employeeService.getEmployeeById(employeeId);
        });

        assertEquals("Database connection error", exception.getMessage());
        verify(repository, times(1)).findById(employeeId);
    }


    //Update all employee fields successfully
    @Test
    void updateEmployee_ValidIdAndCompleteDTO_Success() {
        // Arrange
        Long employeeId = 1L;
        Employee existingEmployee = new Employee();
        existingEmployee.setId(employeeId);
        existingEmployee.setName("Old Name");
        existingEmployee.setEmail("old@email.com");

        EmployeeDTO updateDTO = new EmployeeDTO();
        updateDTO.setName("New Name");
        updateDTO.setEmail("new@email.com");
        updateDTO.setRole(Role.ADMIN);
        updateDTO.setDesignation("Senior Manager");
        updateDTO.setDepartment("HR");
        updateDTO.setAnnualSalary(85000.0);
        updateDTO.setStatus("ACTIVE");
        updateDTO.setDateOfJoining(LocalDate.of(2020, 1, 1));
        updateDTO.setPhoneNumber("+1234567890");
        updateDTO.setAddress("123 Main St");
        updateDTO.setPanNumber("ABCDE1234F");
        updateDTO.setPfNumber("PF123456");
        updateDTO.setUanNumber("UAN789012");
        updateDTO.setBankName("Example Bank");
        updateDTO.setBankBranch("Downtown Branch");
        updateDTO.setBankAccountNumber("ACC123456789");
        updateDTO.setVendorCode("VENDOR001");

        when(repository.findById(employeeId)).thenReturn(Optional.of(existingEmployee));
        when(repository.save(any(Employee.class))).thenReturn(existingEmployee);

        // Act
        employeeService.updateEmployee(employeeId, updateDTO);

        // Assert
        verify(repository).findById(employeeId);
        verify(repository).save(existingEmployee);

        assertAll("Updated employee fields",
                () -> assertEquals("New Name", existingEmployee.getName()),
                () -> assertEquals("new@email.com", existingEmployee.getEmail()),
                () -> assertEquals(Role.ADMIN, existingEmployee.getRole()),
                () -> assertEquals("Senior Manager", existingEmployee.getDesignation()),
                () -> assertEquals("HR", existingEmployee.getDepartment()),
                () -> assertEquals(85000.0, existingEmployee.getAnnualSalary()),
                () -> assertEquals("ACTIVE", existingEmployee.getStatus()),
                () -> assertEquals(LocalDate.of(2020, 1, 1), existingEmployee.getDateOfJoining()),
                () -> assertEquals("+1234567890", existingEmployee.getPhoneNumber()),
                () -> assertEquals("123 Main St", existingEmployee.getAddress()),
                () -> assertEquals("ABCDE1234F", existingEmployee.getPanNumber()),
                () -> assertEquals("PF123456", existingEmployee.getPfNumber()),
                () -> assertEquals("UAN789012", existingEmployee.getUanNumber()),
                () -> assertEquals("Example Bank", existingEmployee.getBankName()),
                () -> assertEquals("Downtown Branch", existingEmployee.getBankBranch()),
                () -> assertEquals("ACC123456789", existingEmployee.getBankAccountNumber()),
                () -> assertEquals("VENDOR001", existingEmployee.getVendorCode())
        );
    }

    //Update with null values (explicitly set to null)
    @Test
    void updateEmployee_SetFieldsToNull_Success() {
        // Arrange
        Long employeeId = 1L;
        Employee existingEmployee = new Employee();
        existingEmployee.setId(employeeId);
        existingEmployee.setName("Has Value");
        existingEmployee.setPhoneNumber("123456");
        existingEmployee.setAddress("Old Address");
        existingEmployee.setPanNumber("OLD123");

        EmployeeDTO updateDTO = new EmployeeDTO();
        updateDTO.setName(null);
        updateDTO.setPhoneNumber(null);
        updateDTO.setAddress(null);
        updateDTO.setPanNumber(null);

        when(repository.findById(employeeId)).thenReturn(Optional.of(existingEmployee));

        // Act
        employeeService.updateEmployee(employeeId, updateDTO);

        // Assert
        assertNull(existingEmployee.getName());
        assertNull(existingEmployee.getPhoneNumber());
        assertNull(existingEmployee.getAddress());
        assertNull(existingEmployee.getPanNumber());
    }

    //Employee not found - throws exception
    @Test
    void updateEmployee_EmployeeNotFound_ThrowsEmployeeException() {
        // Arrange
        Long nonExistentId = 999L;
        EmployeeDTO updateDTO = new EmployeeDTO();
        updateDTO.setName("New Name");

        when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        EmployeeException exception = assertThrows(EmployeeException.class, () -> {
            employeeService.updateEmployee(nonExistentId, updateDTO);
        });

        assertEquals("Employee not found with id: " + nonExistentId, exception.getMessage());
        verify(repository, never()).save(any(Employee.class));
    }

    // Repository throws database exception during findById
    @Test
    void updateEmployee_RepositoryThrowsExceptionOnFind_PropagatesException() {
        // Arrange
        Long employeeId = 1L;
        EmployeeDTO updateDTO = new EmployeeDTO();
        updateDTO.setName("New Name");

        when(repository.findById(employeeId)).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            employeeService.updateEmployee(employeeId, updateDTO);
        });

        assertEquals("Database error", exception.getMessage());
        verify(repository, never()).save(any());
    }

    //Repository throws exception during save
    @Test
    void updateEmployee_RepositoryThrowsExceptionOnSave_PropagatesException() {
        // Arrange
        Long employeeId = 1L;
        Employee existingEmployee = new Employee();
        existingEmployee.setId(employeeId);

        EmployeeDTO updateDTO = new EmployeeDTO();
        updateDTO.setName("New Name");

        when(repository.findById(employeeId)).thenReturn(Optional.of(existingEmployee));
        when(repository.save(any(Employee.class))).thenThrow(new RuntimeException("Failed to save"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            employeeService.updateEmployee(employeeId, updateDTO);
        });

        assertEquals("Failed to save", exception.getMessage());
        verify(repository).findById(employeeId);
        verify(repository).save(existingEmployee);
    }


    //Delete existing employee successfully
    @Test
    void deleteEmployee_ValidId_Success() {
        // Arrange
        Long employeeId = 100L;
        Employee existingEmployee = new Employee();
        existingEmployee.setId(employeeId);
        existingEmployee.setName("John Doe");
        existingEmployee.setEmail("john@example.com");

        when(repository.findById(employeeId)).thenReturn(Optional.of(existingEmployee));
        doNothing().when(repository).delete(existingEmployee);

        // Act
        employeeService.deleteEmployee(employeeId);

        // Assert
        verify(repository, times(1)).findById(employeeId);
        verify(repository, times(1)).delete(existingEmployee);
    }

    @Test
    void deleteEmployee_ValidId_VerifiesDeleteCalledOnce() {
        // Arrange
        Long employeeId = 1L;
        Employee employee = new Employee();
        employee.setId(employeeId);

        when(repository.findById(employeeId)).thenReturn(Optional.of(employee));

        // Act
        employeeService.deleteEmployee(employeeId);

        // Assert
        verify(repository).findById(employeeId);
        verify(repository).delete(employee);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void deleteEmployee_EmployeeWithCompleteData_Success() {
        // Arrange
        Long employeeId = 1L;
        Employee employee = new Employee();
        employee.setId(employeeId);
        employee.setName("Jane Smith");
        employee.setEmail("jane@company.com");
        employee.setRole(Role.ADMIN);
        employee.setDesignation("CTO");
        employee.setDepartment("IT");
        employee.setAnnualSalary(150000.0);
        employee.setStatus("ACTIVE");
        employee.setPhoneNumber("+1234567890");
        employee.setPanNumber("ABCDE1234F");

        when(repository.findById(employeeId)).thenReturn(Optional.of(employee));
        doNothing().when(repository).delete(employee);

        // Act
        employeeService.deleteEmployee(employeeId);

        // Assert
        verify(repository).delete(employee);
    }

    //Delete non-existent employee
    @Test
    void deleteEmployee_EmployeeNotFound_ThrowsEmployeeException() {
        // Arrange
        Long nonExistentId = 999L;

        when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        EmployeeException exception = assertThrows(EmployeeException.class, () -> {
            employeeService.deleteEmployee(nonExistentId);
        });

        assertEquals("Employee not found with id: " + nonExistentId, exception.getMessage());
        verify(repository, times(1)).findById(nonExistentId);
        verify(repository, never()).delete(any(Employee.class));
    }

    //Negative ID parameter
    @Test
    void deleteEmployee_NegativeId_ThrowsEmployeeException() {
        // Arrange
        Long negativeId = -1L;

        when(repository.findById(negativeId)).thenReturn(Optional.empty());

        // Act & Assert
        EmployeeException exception = assertThrows(EmployeeException.class, () -> {
            employeeService.deleteEmployee(negativeId);
        });

        assertEquals("Employee not found with id: " + negativeId, exception.getMessage());
        verify(repository, never()).delete(any());
    }

    //Zero ID parameter
    @Test
    void deleteEmployee_ZeroId_ThrowsEmployeeException() {
        // Arrange
        Long zeroId = 0L;

        when(repository.findById(zeroId)).thenReturn(Optional.empty());

        // Act & Assert
        EmployeeException exception = assertThrows(EmployeeException.class, () -> {
            employeeService.deleteEmployee(zeroId);
        });

        assertEquals("Employee not found with id: " + zeroId, exception.getMessage());
    }

    //Repository throws exception during findById
    @Test
    void deleteEmployee_RepositoryThrowsExceptionOnFind_PropagatesException() {
        // Arrange
        Long employeeId = 1L;

        when(repository.findById(employeeId)).thenThrow(new RuntimeException("Database connection error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            employeeService.deleteEmployee(employeeId);
        });

        assertEquals("Database connection error", exception.getMessage());
        verify(repository, never()).delete(any());
    }

    //Repository throws exception during delete
    @Test
    void deleteEmployee_RepositoryThrowsExceptionOnDelete_PropagatesException() {
        // Arrange
        Long employeeId = 1L;
        Employee employee = new Employee();
        employee.setId(employeeId);

        when(repository.findById(employeeId)).thenReturn(Optional.of(employee));
        doThrow(new RuntimeException("Cannot delete due to foreign key constraint"))
                .when(repository).delete(employee);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            employeeService.deleteEmployee(employeeId);
        });

        assertEquals("Cannot delete due to foreign key constraint", exception.getMessage());
        verify(repository).findById(employeeId);
        verify(repository).delete(employee);
    }

    //Delete employee with existing references (foreign key constraint)
    @Test
    void deleteEmployee_EmployeeHasReferences_ThrowsDataIntegrityException() {
        // Arrange
        Long employeeId = 1L;
        Employee employee = new Employee();
        employee.setId(employeeId);

        when(repository.findById(employeeId)).thenReturn(Optional.of(employee));
        doThrow(new DataIntegrityViolationException("Employee has associated records"))
                .when(repository).delete(employee);

        // Act & Assert
        DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class, () -> {
            employeeService.deleteEmployee(employeeId);
        });

        assertTrue(exception.getMessage().contains("associated records"));
    }


    //Register new employees from attendance service successfully
    @Test
    void registerEmployeesFromAttendance_NewEmployees_Success() {
        // Arrange
        List<Map<String, Object>> attendanceEmployees = new ArrayList<>();

        Map<String, Object> emp1 = new HashMap<>();
        emp1.put("email", "john@example.com");
        emp1.put("name", "John Doe");
        emp1.put("employeeId", "100");

        Map<String, Object> emp2 = new HashMap<>();
        emp2.put("email", "jane@example.com");
        emp2.put("name", "Jane Smith");
        emp2.put("employeeId", "101");

        attendanceEmployees.add(emp1);
        attendanceEmployees.add(emp2);

        when(attendanceClient.fetchEmployeesFromAttendance()).thenReturn(attendanceEmployees);
        when(repository.findByEmail("john@example.com")).thenReturn(Optional.empty());
        when(repository.findByEmail("jane@example.com")).thenReturn(Optional.empty());
        when(repository.count()).thenReturn(5L);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPass");
        doNothing().when(emailService).sendPasswordEmail(anyString(), anyString());

        // Act
        employeeService.registerEmployeesFromAttendance();

        // Assert
        verify(repository, times(2)).save(any(Employee.class));
        verify(emailService, times(2)).sendPasswordEmail(anyString(), anyString());
    }

    //Handle mix of existing and new employees
    @Test
    void registerEmployeesFromAttendance_MixOfExistingAndNew_OnlyRegistersNew() {
        // Arrange
        List<Map<String, Object>> attendanceEmployees = new ArrayList<>();

        Map<String, Object> existingEmp = new HashMap<>();
        existingEmp.put("email", "existing@example.com");
        existingEmp.put("name", "Existing User");
        existingEmp.put("employeeId", "200");

        Map<String, Object> newEmp = new HashMap<>();
        newEmp.put("email", "new@example.com");
        newEmp.put("name", "New User");
        newEmp.put("employeeId", "201");

        attendanceEmployees.add(existingEmp);
        attendanceEmployees.add(newEmp);

        when(attendanceClient.fetchEmployeesFromAttendance()).thenReturn(attendanceEmployees);
        when(repository.findByEmail("existing@example.com")).thenReturn(Optional.of(new Employee()));
        when(repository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(repository.count()).thenReturn(5L);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPass");

        // Act
        employeeService.registerEmployeesFromAttendance();

        // Assert
        verify(repository, times(1)).save(any(Employee.class));
        verify(emailService, times(1)).sendPasswordEmail(eq("new@example.com"), anyString());
    }

    //Handle null name by using default "Unknown"
    @Test
    void registerEmployeesFromAttendance_NullName_UsesDefaultUnknown() {
        // Arrange
        List<Map<String, Object>> attendanceEmployees = new ArrayList<>();

        Map<String, Object> emp = new HashMap<>();
        emp.put("email", "test@example.com");
        emp.put("name", null);
        emp.put("employeeId", "123");

        attendanceEmployees.add(emp);

        when(attendanceClient.fetchEmployeesFromAttendance()).thenReturn(attendanceEmployees);
        when(repository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(repository.count()).thenReturn(5L);

        // Act
        employeeService.registerEmployeesFromAttendance();

        // Assert
        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        verify(repository).save(employeeCaptor.capture());
        assertEquals("Unknown", employeeCaptor.getValue().getName());
    }

    //Handle invalid employeeId format
    @Test
    void registerEmployeesFromAttendance_InvalidEmployeeIdFormat_StillRegistersWithNullId() {
        // Arrange
        List<Map<String, Object>> attendanceEmployees = new ArrayList<>();

        Map<String, Object> emp = new HashMap<>();
        emp.put("email", "test@example.com");
        emp.put("name", "Test User");
        emp.put("employeeId", "invalid_number");

        attendanceEmployees.add(emp);

        when(attendanceClient.fetchEmployeesFromAttendance()).thenReturn(attendanceEmployees);
        when(repository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(repository.count()).thenReturn(5L);

        // Act
        employeeService.registerEmployeesFromAttendance();

        // Assert
        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        verify(repository).save(employeeCaptor.capture());
        assertNull(employeeCaptor.getValue().getId());
    }

    //Attendance service returns null
    @Test
    void registerEmployeesFromAttendance_NullResponse_ReturnsEarly() {
        // Arrange
        when(attendanceClient.fetchEmployeesFromAttendance()).thenReturn(null);

        // Act
        employeeService.registerEmployeesFromAttendance();

        // Assert
        verify(repository, never()).findByEmail(anyString());
        verify(repository, never()).save(any(Employee.class));
    }

    //Missing email field in employee data
    @Test
    void registerEmployeesFromAttendance_MissingEmail_SkipsEmployee() {
        // Arrange
        List<Map<String, Object>> attendanceEmployees = new ArrayList<>();

        Map<String, Object> emp = new HashMap<>();
        emp.put("name", "No Email User");
        emp.put("employeeId", "123");
        // email missing

        attendanceEmployees.add(emp);

        when(attendanceClient.fetchEmployeesFromAttendance()).thenReturn(attendanceEmployees);

        // Act
        employeeService.registerEmployeesFromAttendance();

        // Assert
        verify(repository, never()).findByEmail(anyString());
        verify(repository, never()).save(any(Employee.class));
    }

    //Empty email field
    @Test
    void registerEmployeesFromAttendance_EmptyEmail_SkipsEmployee() {
        // Arrange
        List<Map<String, Object>> attendanceEmployees = new ArrayList<>();

        Map<String, Object> emp = new HashMap<>();
        emp.put("email", "");
        emp.put("name", "Empty Email User");
        emp.put("employeeId", "123");

        attendanceEmployees.add(emp);

        when(attendanceClient.fetchEmployeesFromAttendance()).thenReturn(attendanceEmployees);

        // Act
        employeeService.registerEmployeesFromAttendance();

        // Assert
        verify(repository, never()).findByEmail(anyString());
        verify(repository, never()).save(any(Employee.class));
    }

    //Null email field
    @Test
    void registerEmployeesFromAttendance_NullEmail_SkipsEmployee() {
        // Arrange
        List<Map<String, Object>> attendanceEmployees = new ArrayList<>();

        Map<String, Object> emp = new HashMap<>();
        emp.put("email", null);
        emp.put("name", "Null Email User");
        emp.put("employeeId", "123");

        attendanceEmployees.add(emp);

        when(attendanceClient.fetchEmployeesFromAttendance()).thenReturn(attendanceEmployees);

        // Act
        employeeService.registerEmployeesFromAttendance();

        // Assert
        verify(repository, never()).findByEmail(anyString());
        verify(repository, never()).save(any(Employee.class));
    }

    //Attendance service throws exception
    @Test
    void registerEmployeesFromAttendance_AttendanceServiceThrowsException_PropagatesException() {
        // Arrange
        when(attendanceClient.fetchEmployeesFromAttendance())
                .thenThrow(new RuntimeException("Attendance service unavailable"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            employeeService.registerEmployeesFromAttendance();
        });

        assertEquals("Attendance service unavailable", exception.getMessage());
        verify(repository, never()).findByEmail(anyString());
    }



    //Successful login with valid credentials
    @Test
    void login_ValidCredentials_ReturnsLoginResponse() {
        // Arrange
        String email = "john@example.com";
        String password = "correctPassword123";

        Employee employee = new Employee();
        employee.setId(1L);
        employee.setName("John Doe");
        employee.setEmail(email);
        employee.setRole(Role.ADMIN);
        employee.setPassword("encodedPassword");

        when(repository.findByEmail(email)).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches(password, "encodedPassword")).thenReturn(true);

        // Act
        LoginResponse response = employeeService.login(email, password);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("John Doe", response.getName());
        assertEquals(email, response.getEmail());
        assertEquals(Role.ADMIN, response.getRole());
        verify(repository, times(1)).findByEmail(email);
        verify(passwordEncoder, times(1)).matches(password, "encodedPassword");
    }

    //Login with employee role user
    @Test
    void login_EmployeeRole_Success() {
        // Arrange
        String email = "employee@example.com";
        String password = "empPass123";

        Employee employee = new Employee();
        employee.setId(2L);
        employee.setName("Jane Smith");
        employee.setEmail(email);
        employee.setRole(Role.EMPLOYEE);
        employee.setPassword("encodedEmpPass");

        when(repository.findByEmail(email)).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches(password, "encodedEmpPass")).thenReturn(true);

        // Act
        LoginResponse response = employeeService.login(email, password);

        // Assert
        assertEquals(Role.EMPLOYEE, response.getRole());
        assertEquals("Jane Smith", response.getName());
    }

    //Login with email case insensitivity (if implemented)
    @Test
    void login_EmailCaseInsensitive_Success() {
        // Arrange
        String inputEmail = "JOHN@EXAMPLE.COM";
        String storedEmail = "john@example.com";
        String password = "password123";

        Employee employee = new Employee();
        employee.setId(1L);
        employee.setName("John Doe");
        employee.setEmail(storedEmail);
        employee.setRole(Role.ADMIN);
        employee.setPassword("encodedPass");

        when(repository.findByEmail(inputEmail)).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches(password, "encodedPass")).thenReturn(true);

        // Act
        LoginResponse response = employeeService.login(inputEmail, password);

        // Assert
        assertEquals(storedEmail, response.getEmail());
    }

    //Login verifies all response fields
    @Test
    void login_ValidCredentials_ReturnsCompleteResponse() {
        // Arrange
        String email = "complete@example.com";
        String password = "securePass";

        Employee employee = new Employee();
        employee.setId(100L);
        employee.setName("Complete User");
        employee.setEmail(email);
        employee.setRole(Role.ADMIN);
        employee.setPassword("encodedSecurePass");

        when(repository.findByEmail(email)).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches(password, "encodedSecurePass")).thenReturn(true);

        // Act
        LoginResponse response = employeeService.login(email, password);

        // Assert
        assertAll("Login response fields",
                () -> assertEquals(100L, response.getId()),
                () -> assertEquals("Complete User", response.getName()),
                () -> assertEquals(email, response.getEmail()),
                () -> assertEquals(Role.ADMIN, response.getRole())
        );
    }

    //Login with non-existent email
    @Test
    void login_NonExistentEmail_ThrowsEmployeeException() {
        // Arrange
        String email = "nonexistent@example.com";
        String password = "anyPassword";

        when(repository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        EmployeeException exception = assertThrows(EmployeeException.class, () -> {
            employeeService.login(email, password);
        });

        assertEquals("Invalid email or password", exception.getMessage());
        verify(repository, times(1)).findByEmail(email);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    //Login with correct email but wrong password
    @Test
    void login_CorrectEmailWrongPassword_ThrowsEmployeeException() {
        // Arrange
        String email = "john@example.com";
        String wrongPassword = "wrongPassword";
        String correctPassword = "correctPassword";

        Employee employee = new Employee();
        employee.setEmail(email);
        employee.setPassword("encodedCorrectPass");

        when(repository.findByEmail(email)).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches(wrongPassword, "encodedCorrectPass")).thenReturn(false);

        // Act & Assert
        EmployeeException exception = assertThrows(EmployeeException.class, () -> {
            employeeService.login(email, wrongPassword);
        });

        assertEquals("Invalid email or password", exception.getMessage());
        verify(passwordEncoder, times(1)).matches(wrongPassword, "encodedCorrectPass");
    }

    //Login with null email
    @Test
    void login_NullEmail_ThrowsException() {
        // Arrange
        String password = "password123";

        // Act & Assert
        assertThrows(Exception.class, () -> {
            employeeService.login(null, password);
        });

        verify(repository, never()).findByEmail(anyString());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    //Login with empty email
    @Test
    void login_EmptyEmail_ThrowsException() {
        // Arrange
        String email = "";
        String password = "password123";

        // Act & Assert
        assertThrows(Exception.class, () -> {
            employeeService.login(email, password);
        });
    }

    //Login with empty password
    @Test
    void login_EmptyPassword_ThrowsException() {
        // Arrange
        String email = "john@example.com";
        String password = "";

        // Act & Assert
        assertThrows(Exception.class, () -> {
            employeeService.login(email, password);
        });
    }

    //Login with email having invalid format (repository finds none)
    @Test
    void login_InvalidEmailFormat_ThrowsEmployeeException() {
        // Arrange
        String invalidEmail = "invalid-email-format";
        String password = "password123";

        when(repository.findByEmail(invalidEmail)).thenReturn(Optional.empty());

        // Act & Assert
        EmployeeException exception = assertThrows(EmployeeException.class, () -> {
            employeeService.login(invalidEmail, password);
        });

        assertEquals("Invalid email or password", exception.getMessage());
    }

    //Login with whitespace only email
    @Test
    void login_WhitespaceOnlyEmail_ThrowsException() {
        // Arrange
        String email = "   ";
        String password = "password123";

        // Act & Assert
        assertThrows(Exception.class, () -> {
            employeeService.login(email, password);
        });
    }

    //Repository throws database exception
    @Test
    void login_RepositoryThrowsException_PropagatesException() {
        // Arrange
        String email = "john@example.com";
        String password = "password123";

        when(repository.findByEmail(email)).thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            employeeService.login(email, password);
        });

        assertEquals("Database connection failed", exception.getMessage());
    }


    //Get employee package with complete data
    @Test
    void getEmployeePackage_ValidId_ReturnsEmployeePackageDTO() {
        // Arrange
        Long employeeId = 100L;
        Employee employee = new Employee();
        employee.setId(employeeId);
        employee.setName("John Doe");
        employee.setDesignation("Senior Software Engineer");
        employee.setRole(Role.ADMIN);
        employee.setAnnualSalary(95000.0);
        employee.setDateOfJoining(LocalDate.of(2020, 5, 15));

        when(repository.findById(employeeId)).thenReturn(Optional.of(employee));

        // Act
        EmployeePackageDTO result = employeeService.getEmployeePackage(employeeId);

        // Assert
        assertNotNull(result);
        assertEquals(employeeId, result.getEmployeeId()); // ← Changed to getEmployeeId()
        assertEquals("John Doe", result.getName());
        assertEquals("Senior Software Engineer", result.getDesignation());
        assertEquals("ADMIN", result.getRole());
        assertEquals(95000.0, result.getAnnualSalary());
        assertEquals(LocalDate.of(2020, 5, 15), result.getDateOfJoining());
        verify(repository, times(1)).findById(employeeId);
    }

    //Get employee package for EMPLOYEE role
    @Test
    void getEmployeePackage_EmployeeRole_ReturnsCorrectRoleString() {
        // Arrange
        Long employeeId = 1L;
        Employee employee = new Employee();
        employee.setId(employeeId);
        employee.setName("Jane Smith");
        employee.setDesignation("Junior Developer");
        employee.setRole(Role.EMPLOYEE);
        employee.setAnnualSalary(55000.0);
        employee.setDateOfJoining(LocalDate.of(2023, 1, 10));

        when(repository.findById(employeeId)).thenReturn(Optional.of(employee));

        // Act
        EmployeePackageDTO result = employeeService.getEmployeePackage(employeeId);

        // Assert
        assertEquals("EMPLOYEE", result.getRole());
        assertEquals("Junior Developer", result.getDesignation());
    }

    //Employee not found - throws RuntimeException
    @Test
    void getEmployeePackage_EmployeeNotFound_ThrowsRuntimeException() {
        // Arrange
        Long nonExistentId = 999L;

        when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            employeeService.getEmployeePackage(nonExistentId);
        });

        assertEquals("Employee not found with id " + nonExistentId, exception.getMessage());
        verify(repository, times(1)).findById(nonExistentId);
    }

    //Negative ID parameter
    @Test
    void getEmployeePackage_NegativeId_ThrowsRuntimeException() {
        // Arrange
        Long negativeId = -1L;

        when(repository.findById(negativeId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            employeeService.getEmployeePackage(negativeId);
        });

        assertEquals("Employee not found with id " + negativeId, exception.getMessage());
    }

    //Zero ID parameter
    @Test
    void getEmployeePackage_ZeroId_ThrowsRuntimeException() {
        // Arrange
        Long zeroId = 0L;

        when(repository.findById(zeroId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            employeeService.getEmployeePackage(zeroId);
        });

        assertEquals("Employee not found with id " + zeroId, exception.getMessage());
    }

    //Repository throws database exception
    @Test
    void getEmployeePackage_RepositoryThrowsException_PropagatesException() {
        // Arrange
        Long employeeId = 1L;

        when(repository.findById(employeeId)).thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            employeeService.getEmployeePackage(employeeId);
        });

        assertEquals("Database connection failed", exception.getMessage());
    }

    //Employee with null role (role.name() will throw NPE)
    @Test
    void getEmployeePackage_NullRole_ThrowsNullPointerException() {
        // Arrange
        Long employeeId = 1L;
        Employee employee = new Employee();
        employee.setId(employeeId);
        employee.setName("No Role User");
        employee.setDesignation("Developer");
        employee.setRole(null); // Null role
        employee.setAnnualSalary(60000.0);
        employee.setDateOfJoining(LocalDate.now());

        when(repository.findById(employeeId)).thenReturn(Optional.of(employee));

        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            employeeService.getEmployeePackage(employeeId);
        });
    }



    //Update all profile fields successfully
    @Test
    void updateProfile_ValidIdAndCompleteData_Success() {
        // Arrange
        Long employeeId = 1L;
        Employee existingEmployee = new Employee();
        existingEmployee.setId(employeeId);
        existingEmployee.setName("Old Name");
        existingEmployee.setPhoneNumber("1234567890");
        existingEmployee.setAddress("Old Address");
        existingEmployee.setDesignation("Junior Developer");
        existingEmployee.setDepartment("Engineering");

        ProfileUpdateDTO updateDTO = new ProfileUpdateDTO();
        updateDTO.setName("New Name");
        updateDTO.setPhoneNumber("+9876543210");
        updateDTO.setAddress("New Address, City, Country");
        updateDTO.setDesignation("Senior Developer");
        updateDTO.setDepartment("Product Development");

        when(repository.findById(employeeId)).thenReturn(Optional.of(existingEmployee));
        when(repository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Employee result = employeeService.updateProfile(employeeId, updateDTO);

        // Assert
        assertNotNull(result);
        assertEquals("New Name", result.getName());
        assertEquals("+9876543210", result.getPhoneNumber());
        assertEquals("New Address, City, Country", result.getAddress());
        assertEquals("Senior Developer", result.getDesignation());
        assertEquals("Product Development", result.getDepartment());

        verify(repository, times(1)).findById(employeeId);
        verify(repository, times(1)).save(existingEmployee);
    }

    //Update with empty strings
    @Test
    void updateProfile_UpdateWithEmptyStrings_Success() {
        // Arrange
        Long employeeId = 1L;
        Employee existingEmployee = new Employee();
        existingEmployee.setId(employeeId);
        existingEmployee.setName("Original Name");
        existingEmployee.setPhoneNumber("123456");
        existingEmployee.setAddress("Original Address");

        ProfileUpdateDTO updateDTO = new ProfileUpdateDTO();
        updateDTO.setName("");
        updateDTO.setPhoneNumber("");
        updateDTO.setAddress("");

        when(repository.findById(employeeId)).thenReturn(Optional.of(existingEmployee));
        when(repository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Employee result = employeeService.updateProfile(employeeId, updateDTO);

        // Assert
        assertEquals("", result.getName());
        assertEquals("", result.getPhoneNumber());
        assertEquals("", result.getAddress());
    }

    //Update profile and verify save returns updated employee
    @Test
    void updateProfile_ValidUpdate_ReturnsUpdatedEmployee() {
        // Arrange
        Long employeeId = 1L;
        Employee existingEmployee = new Employee();
        existingEmployee.setId(employeeId);
        existingEmployee.setName("Before Update");

        Employee updatedEmployee = new Employee();
        updatedEmployee.setId(employeeId);
        updatedEmployee.setName("After Update");

        ProfileUpdateDTO updateDTO = new ProfileUpdateDTO();
        updateDTO.setName("After Update");

        when(repository.findById(employeeId)).thenReturn(Optional.of(existingEmployee));
        when(repository.save(any(Employee.class))).thenReturn(updatedEmployee);

        // Act
        Employee result = employeeService.updateProfile(employeeId, updateDTO);

        // Assert
        assertEquals("After Update", result.getName());
        assertSame(updatedEmployee, result);
    }

    //Employee not found
    @Test
    void updateProfile_EmployeeNotFound_ThrowsEmployeeException() {
        // Arrange
        Long nonExistentId = 999L;
        ProfileUpdateDTO updateDTO = new ProfileUpdateDTO();
        updateDTO.setName("New Name");

        when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        EmployeeException exception = assertThrows(EmployeeException.class, () -> {
            employeeService.updateProfile(nonExistentId, updateDTO);
        });

        assertEquals("Employee not found with id: " + nonExistentId, exception.getMessage());
        verify(repository, never()).save(any(Employee.class));
    }

    //Negative ID parameter
    @Test
    void updateProfile_NegativeId_ThrowsEmployeeException() {
        // Arrange
        Long negativeId = -1L;
        ProfileUpdateDTO updateDTO = new ProfileUpdateDTO();
        updateDTO.setName("New Name");

        when(repository.findById(negativeId)).thenReturn(Optional.empty());

        // Act & Assert
        EmployeeException exception = assertThrows(EmployeeException.class, () -> {
            employeeService.updateProfile(negativeId, updateDTO);
        });

        assertEquals("Employee not found with id: " + negativeId, exception.getMessage());
    }

    //Zero ID parameter
    @Test
    void updateProfile_ZeroId_ThrowsEmployeeException() {
        // Arrange
        Long zeroId = 0L;
        ProfileUpdateDTO updateDTO = new ProfileUpdateDTO();
        updateDTO.setName("New Name");

        when(repository.findById(zeroId)).thenReturn(Optional.empty());

        // Act & Assert
        EmployeeException exception = assertThrows(EmployeeException.class, () -> {
            employeeService.updateProfile(zeroId, updateDTO);
        });

        assertEquals("Employee not found with id: " + zeroId, exception.getMessage());
    }

    //Repository throws exception during findById
    @Test
    void updateProfile_RepositoryThrowsExceptionOnFind_PropagatesException() {
        // Arrange
        Long employeeId = 1L;
        ProfileUpdateDTO updateDTO = new ProfileUpdateDTO();
        updateDTO.setName("New Name");

        when(repository.findById(employeeId)).thenThrow(new RuntimeException("Database connection error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            employeeService.updateProfile(employeeId, updateDTO);
        });

        assertEquals("Database connection error", exception.getMessage());
        verify(repository, never()).save(any());
    }

    //Repository throws exception during save
    @Test
    void updateProfile_RepositoryThrowsExceptionOnSave_PropagatesException() {
        // Arrange
        Long employeeId = 1L;
        Employee existingEmployee = new Employee();
        existingEmployee.setId(employeeId);

        ProfileUpdateDTO updateDTO = new ProfileUpdateDTO();
        updateDTO.setName("New Name");

        when(repository.findById(employeeId)).thenReturn(Optional.of(existingEmployee));
        when(repository.save(any(Employee.class))).thenThrow(new RuntimeException("Failed to save"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            employeeService.updateProfile(employeeId, updateDTO);
        });

        assertEquals("Failed to save", exception.getMessage());
        verify(repository).findById(employeeId);
        verify(repository).save(existingEmployee);
    }



    //Empty profile image
    @Test
    void updateProfileImage_EmptyImage_ThrowsEmployeeException() {
        // Arrange
        Long employeeId = 1L;
        MultipartFile mockImage = mock(MultipartFile.class);

        when(mockImage.isEmpty()).thenReturn(true);

        // Act & Assert
        EmployeeException exception = assertThrows(EmployeeException.class, () -> {
            employeeService.updateProfileImage(employeeId, mockImage);
        });

        assertEquals("Profile image cannot be empty", exception.getMessage());
        verify(repository, never()).findById(any());
        verify(repository, never()).save(any());
    }

    //Employee not found
    @Test
    void updateProfileImage_EmployeeNotFound_ThrowsEmployeeException() {
        // Arrange
        Long nonExistentId = 999L;
        MultipartFile mockImage = mock(MultipartFile.class);

        when(mockImage.isEmpty()).thenReturn(false);
        when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        EmployeeException exception = assertThrows(EmployeeException.class, () -> {
            employeeService.updateProfileImage(nonExistentId, mockImage);
        });

        assertEquals("Employee not found with id: " + nonExistentId, exception.getMessage());
        verify(repository, never()).save(any());
    }

    //Null MultipartFile
    @Test
    void updateProfileImage_NullMultipartFile_ThrowsException() {
        // Arrange
        Long employeeId = 1L;

        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            employeeService.updateProfileImage(employeeId, null);
        });
    }

    //Null ID
    @Test
    void updateProfileImage_NullId_ThrowsException() {
        // Arrange
        MultipartFile mockImage = mock(MultipartFile.class);

        // Act & Assert
        assertThrows(Exception.class, () -> {
            employeeService.updateProfileImage(null, mockImage);
        });
    }

    //Initiate forgot password for existing employee
    @Test
    void initiateForgotPassword_ValidEmail_Success() {
        // Arrange
        ForgotPasswordDTO forgotPasswordDTO = new ForgotPasswordDTO();
        forgotPasswordDTO.setEmail("john@example.com");

        Employee employee = new Employee();
        employee.setId(1L);
        employee.setEmail("john@example.com");
        employee.setName("John Doe");
        employee.setOtp(null);
        employee.setOtpGeneratedTime(null);

        when(repository.findByEmail(forgotPasswordDTO.getEmail())).thenReturn(Optional.of(employee));
        when(repository.save(any(Employee.class))).thenReturn(employee);
        doNothing().when(emailService).sendOTPEmail(anyString(), anyString());

        // Act
        employeeService.initiateForgotPassword(forgotPasswordDTO);

        // Assert
        assertNotNull(employee.getOtp());
        assertEquals(6, employee.getOtp().length());
        assertTrue(employee.getOtp().matches("\\d{6}"));
        assertNotNull(employee.getOtpGeneratedTime());
        verify(repository).save(employee);
        verify(emailService).sendOTPEmail(eq("john@example.com"), anyString());
    }

    //OTP is exactly 6 digits
    @Test
    void initiateForgotPassword_Generates6DigitOTP() {
        // Arrange
        ForgotPasswordDTO forgotPasswordDTO = new ForgotPasswordDTO();
        forgotPasswordDTO.setEmail("test@example.com");

        Employee employee = new Employee();
        employee.setEmail("test@example.com");

        when(repository.findByEmail(forgotPasswordDTO.getEmail())).thenReturn(Optional.of(employee));
        when(repository.save(any(Employee.class))).thenReturn(employee);
        doNothing().when(emailService).sendOTPEmail(anyString(), anyString());

        // Act
        employeeService.initiateForgotPassword(forgotPasswordDTO);

        // Assert
        String otp = employee.getOtp();
        assertNotNull(otp);
        assertEquals(6, otp.length());
        assertTrue(otp.matches("^[0-9]{6}$"));
    }

    //OTP expiry set to current time
    @Test
    void initiateForgotPassword_SetsOTPExpiryTime() {
        // Arrange
        ForgotPasswordDTO forgotPasswordDTO = new ForgotPasswordDTO();
        forgotPasswordDTO.setEmail("test@example.com");

        Employee employee = new Employee();
        employee.setEmail("test@example.com");

        LocalDateTime beforeTest = LocalDateTime.now();

        when(repository.findByEmail(forgotPasswordDTO.getEmail())).thenReturn(Optional.of(employee));
        when(repository.save(any(Employee.class))).thenReturn(employee);
        doNothing().when(emailService).sendOTPEmail(anyString(), anyString());

        // Act
        employeeService.initiateForgotPassword(forgotPasswordDTO);

        // Assert
        LocalDateTime otpTime = employee.getOtpGeneratedTime();
        assertNotNull(otpTime);
        assertTrue(otpTime.isAfter(beforeTest) || otpTime.equals(beforeTest));
        assertTrue(otpTime.isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    //Employee not found with given email
    @Test
    void initiateForgotPassword_EmailNotFound_ThrowsEmployeeException() {
        // Arrange
        ForgotPasswordDTO forgotPasswordDTO = new ForgotPasswordDTO();
        forgotPasswordDTO.setEmail("nonexistent@example.com");

        when(repository.findByEmail(forgotPasswordDTO.getEmail())).thenReturn(Optional.empty());

        // Act & Assert
        EmployeeException exception = assertThrows(EmployeeException.class, () -> {
            employeeService.initiateForgotPassword(forgotPasswordDTO);
        });

        assertEquals("Employee not found with email: nonexistent@example.com", exception.getMessage());
        verify(repository, never()).save(any(Employee.class));
        verify(emailService, never()).sendOTPEmail(anyString(), anyString());
    }

    //Null email in DTO
    @Test
    void initiateForgotPassword_NullEmail_ThrowsException() {
        // Arrange
        ForgotPasswordDTO forgotPasswordDTO = new ForgotPasswordDTO();
        forgotPasswordDTO.setEmail(null);

        // Act & Assert
        assertThrows(Exception.class, () -> {
            employeeService.initiateForgotPassword(forgotPasswordDTO);
        });

        verify(repository, never()).save(any(Employee.class));
    }

    //Empty email in DTO
    @Test
    void initiateForgotPassword_EmptyEmail_ThrowsException() {
        // Arrange
        ForgotPasswordDTO forgotPasswordDTO = new ForgotPasswordDTO();
        forgotPasswordDTO.setEmail("");

        // Act & Assert
        assertThrows(Exception.class, () -> {
            employeeService.initiateForgotPassword(forgotPasswordDTO);
        });
    }

    //Null DTO
    @Test
    void initiateForgotPassword_NullDTO_ThrowsException() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            employeeService.initiateForgotPassword(null);
        });
    }

    //Email with whitespace only
    @Test
    void initiateForgotPassword_WhitespaceEmail_ThrowsException() {
        // Arrange
        ForgotPasswordDTO forgotPasswordDTO = new ForgotPasswordDTO();
        forgotPasswordDTO.setEmail("   ");

        // Act & Assert
        assertThrows(Exception.class, () -> {
            employeeService.initiateForgotPassword(forgotPasswordDTO);
        });
    }



    //Reset password with valid OTP
    @Test
    void resetPassword_ValidOTP_Success() {
        // Arrange
        PasswordResetDTO resetDTO = new PasswordResetDTO();
        resetDTO.setEmail("john@example.com");
        resetDTO.setOtp("123456");
        resetDTO.setNewPassword("NewSecurePass123!");

        Employee employee = new Employee();
        employee.setId(1L);
        employee.setEmail("john@example.com");
        employee.setOtp("123456");
        employee.setOtpGeneratedTime(LocalDateTime.now().minusMinutes(5));
        employee.setPassword("oldEncodedPassword");

        when(repository.findByEmail(resetDTO.getEmail())).thenReturn(Optional.of(employee));
        when(passwordEncoder.encode(resetDTO.getNewPassword())).thenReturn("newEncodedPassword");
        when(repository.save(any(Employee.class))).thenReturn(employee);
        doNothing().when(emailService).sendPasswordResetConfirmation(anyString());

        // Act
        employeeService.resetPassword(resetDTO);

        // Assert
        assertEquals("newEncodedPassword", employee.getPassword());
        assertNull(employee.getOtp());
        assertNull(employee.getOtpGeneratedTime());
        verify(repository).save(employee);
        verify(emailService).sendPasswordResetConfirmation("john@example.com");
    }

    //Reset password just before expiry
    @Test
    void resetPassword_JustBeforeExpiry_Success() {
        // Arrange
        PasswordResetDTO resetDTO = new PasswordResetDTO();
        resetDTO.setEmail("john@example.com");
        resetDTO.setOtp("123456");
        resetDTO.setNewPassword("NewPassword123");

        Employee employee = new Employee();
        employee.setEmail("john@example.com");
        employee.setOtp("123456");
        employee.setOtpGeneratedTime(LocalDateTime.now().minusMinutes(9).minusSeconds(59));

        when(repository.findByEmail(resetDTO.getEmail())).thenReturn(Optional.of(employee));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(repository.save(any(Employee.class))).thenReturn(employee);

        // Act
        employeeService.resetPassword(resetDTO);

        // Assert
        verify(repository).save(employee);
    }

    //Verify OTP is cleared after successful reset
    @Test
    void resetPassword_ClearsOTPAfterSuccess() {
        // Arrange
        PasswordResetDTO resetDTO = new PasswordResetDTO();
        resetDTO.setEmail("john@example.com");
        resetDTO.setOtp("123456");
        resetDTO.setNewPassword("NewPassword123");

        Employee employee = new Employee();
        employee.setEmail("john@example.com");
        employee.setOtp("123456");
        employee.setOtpGeneratedTime(LocalDateTime.now().minusMinutes(5));

        when(repository.findByEmail(resetDTO.getEmail())).thenReturn(Optional.of(employee));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(repository.save(any(Employee.class))).thenReturn(employee);

        // Act
        employeeService.resetPassword(resetDTO);

        // Assert
        assertNull(employee.getOtp());
        assertNull(employee.getOtpGeneratedTime());
    }

    //Employee not found
    @Test
    void resetPassword_EmployeeNotFound_ThrowsException() {
        // Arrange
        PasswordResetDTO resetDTO = new PasswordResetDTO();
        resetDTO.setEmail("nonexistent@example.com");
        resetDTO.setOtp("123456");
        resetDTO.setNewPassword("NewPassword123");

        when(repository.findByEmail(resetDTO.getEmail())).thenReturn(Optional.empty());

        // Act & Assert
        EmployeeException exception = assertThrows(EmployeeException.class, () -> {
            employeeService.resetPassword(resetDTO);
        });

        assertEquals("Employee not found with email: nonexistent@example.com", exception.getMessage());
        verify(repository, never()).save(any());
        verify(emailService, never()).sendPasswordResetConfirmation(anyString());
    }

    //Invalid OTP
    @Test
    void resetPassword_InvalidOTP_ThrowsException() {
        // Arrange
        PasswordResetDTO resetDTO = new PasswordResetDTO();
        resetDTO.setEmail("john@example.com");
        resetDTO.setOtp("999999"); // Wrong OTP
        resetDTO.setNewPassword("NewPassword123");

        Employee employee = new Employee();
        employee.setEmail("john@example.com");
        employee.setOtp("123456"); // Correct OTP
        employee.setOtpGeneratedTime(LocalDateTime.now().minusMinutes(5));

        when(repository.findByEmail(resetDTO.getEmail())).thenReturn(Optional.of(employee));

        // Act & Assert
        EmployeeException exception = assertThrows(EmployeeException.class, () -> {
            employeeService.resetPassword(resetDTO);
        });

        assertEquals("Invalid OTP", exception.getMessage());
        verify(repository, never()).save(any());
        verify(emailService, never()).sendPasswordResetConfirmation(anyString());
    }

    //Null OTP in database
    @Test
    void resetPassword_NullOTPInDatabase_ThrowsException() {
        // Arrange
        PasswordResetDTO resetDTO = new PasswordResetDTO();
        resetDTO.setEmail("john@example.com");
        resetDTO.setOtp("123456");
        resetDTO.setNewPassword("NewPassword123");

        Employee employee = new Employee();
        employee.setEmail("john@example.com");
        employee.setOtp(null); // No OTP set
        employee.setOtpGeneratedTime(LocalDateTime.now().minusMinutes(5));

        when(repository.findByEmail(resetDTO.getEmail())).thenReturn(Optional.of(employee));

        // Act & Assert
        EmployeeException exception = assertThrows(EmployeeException.class, () -> {
            employeeService.resetPassword(resetDTO);
        });

        assertEquals("Invalid OTP", exception.getMessage());
    }

    //Expired OTP
    @Test
    void resetPassword_ExpiredOTP_ThrowsException() {
        // Arrange
        PasswordResetDTO resetDTO = new PasswordResetDTO();
        resetDTO.setEmail("john@example.com");
        resetDTO.setOtp("123456");
        resetDTO.setNewPassword("NewPassword123");

        Employee employee = new Employee();
        employee.setEmail("john@example.com");
        employee.setOtp("123456");
        employee.setOtpGeneratedTime(LocalDateTime.now().minusMinutes(11)); // 11 minutes ago

        when(repository.findByEmail(resetDTO.getEmail())).thenReturn(Optional.of(employee));

        // Act & Assert
        EmployeeException exception = assertThrows(EmployeeException.class, () -> {
            employeeService.resetPassword(resetDTO);
        });

        assertEquals("OTP has expired", exception.getMessage());
        verify(repository, never()).save(any());
    }

    //OTP expired exactly at 10 minutes 1 second
    @Test
    void resetPassword_ExpiredOTP_JustAfterBoundary_ThrowsException() {
        // Arrange
        PasswordResetDTO resetDTO = new PasswordResetDTO();
        resetDTO.setEmail("john@example.com");
        resetDTO.setOtp("123456");
        resetDTO.setNewPassword("NewPassword123");

        Employee employee = new Employee();
        employee.setEmail("john@example.com");
        employee.setOtp("123456");
        employee.setOtpGeneratedTime(LocalDateTime.now().minusMinutes(10).minusSeconds(1));

        when(repository.findByEmail(resetDTO.getEmail())).thenReturn(Optional.of(employee));

        // Act & Assert
        EmployeeException exception = assertThrows(EmployeeException.class, () -> {
            employeeService.resetPassword(resetDTO);
        });

        assertEquals("OTP has expired", exception.getMessage());
    }



    //Get current user profile with complete data
    @Test
    void getCurrentUserProfile_ValidEmail_ReturnsEmployee() {
        // Arrange
        String email = "john@example.com";
        Employee expectedEmployee = new Employee();
        expectedEmployee.setId(1L);
        expectedEmployee.setName("John Doe");
        expectedEmployee.setEmail(email);
        expectedEmployee.setRole(Role.ADMIN);
        expectedEmployee.setDesignation("Software Architect");
        expectedEmployee.setDepartment("Engineering");
        expectedEmployee.setAnnualSalary(120000.0);
        expectedEmployee.setStatus("Active");
        expectedEmployee.setDateOfJoining(LocalDate.of(2020, 1, 15));
        expectedEmployee.setPhoneNumber("+1234567890");
        expectedEmployee.setAddress("123 Main St, City");
        expectedEmployee.setPanNumber("ABCDE1234F");
        expectedEmployee.setPfNumber("PF123456");
        expectedEmployee.setUanNumber("UAN789012");
        expectedEmployee.setBankName("Example Bank");
        expectedEmployee.setBankBranch("Downtown Branch");
        expectedEmployee.setBankAccountNumber("ACC123456789");
        expectedEmployee.setVendorCode("VENDOR001");

        when(repository.findByEmail(email)).thenReturn(Optional.of(expectedEmployee));

        // Act
        Employee result = employeeService.getCurrentUserProfile(email);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("John Doe", result.getName());
        assertEquals(email, result.getEmail());
        assertEquals(Role.ADMIN, result.getRole());
        assertEquals("Software Architect", result.getDesignation());
        assertEquals("Engineering", result.getDepartment());
        assertEquals(120000.0, result.getAnnualSalary());
        assertEquals("Active", result.getStatus());
        assertEquals(LocalDate.of(2020, 1, 15), result.getDateOfJoining());
        assertEquals("+1234567890", result.getPhoneNumber());
        assertEquals("123 Main St, City", result.getAddress());
        assertEquals("ABCDE1234F", result.getPanNumber());
        assertEquals("PF123456", result.getPfNumber());
        assertEquals("UAN789012", result.getUanNumber());
        assertEquals("Example Bank", result.getBankName());
        assertEquals("Downtown Branch", result.getBankBranch());
        assertEquals("ACC123456789", result.getBankAccountNumber());
        assertEquals("VENDOR001", result.getVendorCode());

        verify(repository, times(1)).findByEmail(email);
    }

    //Get current user profile with minimal employee data
    @Test
    void getCurrentUserProfile_MinimalEmployeeData_ReturnsEmployee() {
        // Arrange
        String email = "minimal@example.com";
        Employee expectedEmployee = new Employee();
        expectedEmployee.setId(2L);
        expectedEmployee.setName("Minimal User");
        expectedEmployee.setEmail(email);
        expectedEmployee.setRole(Role.EMPLOYEE);
        // Other fields are null

        when(repository.findByEmail(email)).thenReturn(Optional.of(expectedEmployee));

        // Act
        Employee result = employeeService.getCurrentUserProfile(email);

        // Assert
        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("Minimal User", result.getName());
        assertEquals(email, result.getEmail());
        assertEquals(Role.EMPLOYEE, result.getRole());
        assertNull(result.getDesignation());
        assertNull(result.getDepartment());
        assertNull(result.getAnnualSalary());
        assertNull(result.getStatus());
        assertNull(result.getDateOfJoining());
        assertNull(result.getPhoneNumber());
        assertNull(result.getAddress());
    }

    //Get current user profile for EMPLOYEE role
    @Test
    void getCurrentUserProfile_EmployeeRole_ReturnsEmployee() {
        // Arrange
        String email = "employee@example.com";
        Employee employee = new Employee();
        employee.setId(3L);
        employee.setName("Jane Smith");
        employee.setEmail(email);
        employee.setRole(Role.EMPLOYEE);
        employee.setDesignation("Junior Developer");

        when(repository.findByEmail(email)).thenReturn(Optional.of(employee));

        // Act
        Employee result = employeeService.getCurrentUserProfile(email);

        // Assert
        assertEquals(Role.EMPLOYEE, result.getRole());
        assertEquals("Jane Smith", result.getName());
    }

    //Email not found
    @Test
    void getCurrentUserProfile_EmailNotFound_ThrowsEmployeeException() {
        // Arrange
        String email = "nonexistent@example.com";

        when(repository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        EmployeeException exception = assertThrows(EmployeeException.class, () -> {
            employeeService.getCurrentUserProfile(email);
        });

        assertEquals("Employee not found with email: " + email, exception.getMessage());
        verify(repository, times(1)).findByEmail(email);
    }

    //Invalid email format (not found)
    @Test
    void getCurrentUserProfile_InvalidEmailFormat_ThrowsEmployeeException() {
        // Arrange
        String invalidEmail = "invalid-email-format";

        when(repository.findByEmail(invalidEmail)).thenReturn(Optional.empty());

        // Act & Assert
        EmployeeException exception = assertThrows(EmployeeException.class, () -> {
            employeeService.getCurrentUserProfile(invalidEmail);
        });

        assertEquals("Employee not found with email: " + invalidEmail, exception.getMessage());
    }

    //Repository throws exception during find
    @Test
    void getCurrentUserProfile_RepositoryThrowsException_PropagatesException() {
        // Arrange
        String email = "john@example.com";

        when(repository.findByEmail(email)).thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            employeeService.getCurrentUserProfile(email);
        });

        assertEquals("Database connection failed", exception.getMessage());
    }

    //Email with leading/trailing spaces (exact match fails)
    @Test
    void getCurrentUserProfile_EmailWithSpaces_ThrowsEmployeeException() {
        // Arrange
        String emailWithSpaces = "  john@example.com  ";

        when(repository.findByEmail(emailWithSpaces)).thenReturn(Optional.empty());

        // Act & Assert
        EmployeeException exception = assertThrows(EmployeeException.class, () -> {
            employeeService.getCurrentUserProfile(emailWithSpaces);
        });

        assertEquals("Employee not found with email: " + emailWithSpaces, exception.getMessage());
    }

    //Case-sensitive email mismatch
    @Test
    void getCurrentUserProfile_CaseSensitiveEmail_ThrowsEmployeeException() {
        // Arrange
        String emailUppercase = "JOHN@EXAMPLE.COM";

        when(repository.findByEmail(emailUppercase)).thenReturn(Optional.empty());

        // Act & Assert
        EmployeeException exception = assertThrows(EmployeeException.class, () -> {
            employeeService.getCurrentUserProfile(emailUppercase);
        });

        assertEquals("Employee not found with email: " + emailUppercase, exception.getMessage());
    }



    //Get total count when employees exist
    @Test
    void getTotalEmployeeCount_EmployeesExist_ReturnsCorrectCount() {
        // Arrange
        long expectedCount = 25L;
        when(repository.count()).thenReturn(expectedCount);

        // Act
        long result = employeeService.getTotalEmployeeCount();

        // Assert
        assertEquals(expectedCount, result);
        verify(repository, times(1)).count();
    }

    //Get total count with zero employees
    @Test
    void getTotalEmployeeCount_NoEmployees_ReturnsZero() {
        // Arrange
        when(repository.count()).thenReturn(0L);

        // Act
        long result = employeeService.getTotalEmployeeCount();

        // Assert
        assertEquals(0L, result);
        verify(repository, times(1)).count();
    }

    //Get total count with one employee
    @Test
    void getTotalEmployeeCount_SingleEmployee_ReturnsOne() {
        // Arrange
        when(repository.count()).thenReturn(1L);

        // Act
        long result = employeeService.getTotalEmployeeCount();

        // Assert
        assertEquals(1L, result);
    }

    //Repository throws database connection exception
    @Test
    void getTotalEmployeeCount_DatabaseConnectionError_PropagatesException() {
        // Arrange
        when(repository.count()).thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            employeeService.getTotalEmployeeCount();
        });

        assertEquals("Database connection failed", exception.getMessage());
        verify(repository, times(1)).count();
    }

    //Repository throws timeout exception
    @Test
    void getTotalEmployeeCount_QueryTimeout_PropagatesException() {
        // Arrange
        when(repository.count()).thenThrow(new RuntimeException("Query timeout"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            employeeService.getTotalEmployeeCount();
        });

        assertEquals("Query timeout", exception.getMessage());
    }

    //Repository throws data access exception
    @Test
    void getTotalEmployeeCount_DataAccessException_PropagatesException() {
        // Arrange
        when(repository.count()).thenThrow(new DataAccessException("Unable to access data") {});

        // Act & Assert
        DataAccessException exception = assertThrows(DataAccessException.class, () -> {
            employeeService.getTotalEmployeeCount();
        });

        assertTrue(exception.getMessage().contains("Unable to access data"));
    }

    //Negative count (should never happen but test defensive)
    @Test
    void getTotalEmployeeCount_NegativeCount_ReturnsNegative() {
        // Note: This should never happen with a proper database
        // But testing defensive programming

        // Arrange
        when(repository.count()).thenReturn(-5L);

        // Act
        long result = employeeService.getTotalEmployeeCount();

        // Assert
        assertEquals(-5L, result); // Method doesn't validate, just returns
    }



    //Email already exists
    @Test
    void createManualEmployee_EmailAlreadyExists_ThrowsException() {
        // Arrange
        EmployeeDTO dto = new EmployeeDTO();
        dto.setName("Duplicate User");
        dto.setEmail("existing@example.com");
        dto.setRole(Role.EMPLOYEE);

        Employee existingEmployee = new Employee();
        existingEmployee.setEmail("existing@example.com");

        when(repository.findByEmail(dto.getEmail())).thenReturn(Optional.of(existingEmployee));

        // Act & Assert
        EmployeeException exception = assertThrows(EmployeeException.class, () -> {
            employeeService.createManualEmployee(dto);
        });

        assertEquals("Email already registered!", exception.getMessage());
        verify(repository, never()).save(any(Employee.class));
        verify(emailService, never()).sendPasswordEmail(anyString(), anyString());
    }

    //Create second admin when admin already exists
    @Test
    void createManualEmployee_SecondAdmin_ThrowsException() {
        // Arrange
        EmployeeDTO dto = new EmployeeDTO();
        dto.setName("Second Admin");
        dto.setEmail("admin2@example.com");
        dto.setRole(Role.ADMIN);

        Employee existingAdmin = new Employee();
        existingAdmin.setRole(Role.ADMIN);

        List<Employee> existingEmployees = Arrays.asList(existingAdmin);

        when(repository.findByEmail(dto.getEmail())).thenReturn(Optional.empty());
        when(repository.findAll()).thenReturn(existingEmployees);

        // Act & Assert
        EmployeeException exception = assertThrows(EmployeeException.class, () -> {
            employeeService.createManualEmployee(dto);
        });

        assertEquals("Only one ADMIN allowed in the system!", exception.getMessage());
        verify(repository, never()).save(any(Employee.class));
        verify(emailService, never()).sendPasswordEmail(anyString(), anyString());
    }

    //Null DTO
    @Test
    void createManualEmployee_NullDTO_ThrowsNullPointerException() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            employeeService.createManualEmployee(null);
        });

        verify(repository, never()).save(any(Employee.class));
    }

    //Repository throws exception during findByEmail
    @Test
    void createManualEmployee_RepositoryThrowsOnFind_PropagatesException() {
        // Arrange
        EmployeeDTO dto = new EmployeeDTO();
        dto.setName("Test User");
        dto.setEmail("test@example.com");
        dto.setRole(Role.EMPLOYEE);

        when(repository.findByEmail(dto.getEmail()))
                .thenThrow(new RuntimeException("Database connection error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            employeeService.createManualEmployee(dto);
        });

        assertEquals("Database connection error", exception.getMessage());
        verify(repository, never()).save(any(Employee.class));
    }

    //Repository throws exception during findAll (admin check)
    @Test
    void createManualEmployee_RepositoryThrowsOnFindAll_PropagatesException() {
        // Arrange
        EmployeeDTO dto = new EmployeeDTO();
        dto.setName("Admin User");
        dto.setEmail("admin@example.com");
        dto.setRole(Role.ADMIN);

        when(repository.findByEmail(dto.getEmail())).thenReturn(Optional.empty());
        when(repository.findAll()).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            employeeService.createManualEmployee(dto);
        });

        assertEquals("Database error", exception.getMessage());
    }

}