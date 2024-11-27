package com.clinic;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/data")
public class Controller {

    @Autowired
    private DataSource dataSource;

    private String parseAndExecuteCommand(Connection connection, String query) throws SQLException {
        Pattern insertPattern = Pattern.compile("insert (\\w+)\\((.*)\\)", Pattern.CASE_INSENSITIVE);
        Pattern deletePattern = Pattern.compile("delete (\\w+)\\((.*)\\)", Pattern.CASE_INSENSITIVE);
        Pattern updatePattern = Pattern.compile("update (\\w+)\\((.*)\\)", Pattern.CASE_INSENSITIVE);
        Pattern selectPattern = Pattern.compile("select (\\w+)", Pattern.CASE_INSENSITIVE);

        Matcher insertMatcher = insertPattern.matcher(query);
        Matcher deleteMatcher = deletePattern.matcher(query);
        Matcher updateMatcher = updatePattern.matcher(query);
        Matcher selectMatcher = selectPattern.matcher(query);

        if (insertMatcher.matches()) {
            return handleInsert(connection, insertMatcher.group(1), insertMatcher.group(2));
        } else if (deleteMatcher.matches()) {
            return handleDelete(connection, deleteMatcher.group(1), deleteMatcher.group(2));
        } else if (updateMatcher.matches()) {
            return handleUpdate(connection, updateMatcher.group(1), updateMatcher.group(2));
        } else if (selectMatcher.matches()) {
            return handleSelect(connection, selectMatcher.group(1));
        }
        return "Wrong command.";
    }

    private String handleInsert(Connection connection, String tableName, String values) throws SQLException {
        String sql;
        PreparedStatement preparedStatement;
    
        switch (tableName.toLowerCase()) {
            case "doctor":
                sql = "INSERT INTO doctor (name, speciality, phone_number) VALUES (?, ?, ?)";
                preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, extractValue(values, "name"));
                preparedStatement.setString(2, extractValue(values, "speciality"));
                preparedStatement.setString(3, extractValue(values, "phone_number"));
                break;
    
            case "patient":
                sql = "INSERT INTO patient (name, date_of_birth, phone_number) VALUES (?, ?, ?)";
                preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, extractValue(values, "name"));
                preparedStatement.setDate(2, Date.valueOf(extractValue(values, "date_of_birth")));
                preparedStatement.setString(3, extractValue(values, "phone_number"));
                break;
    
            case "prescription":
                sql = "INSERT INTO prescription (doctor_id, patient_id, medication, dosage, issued_date) VALUES (?, ?, ?, ?, ?)";
                preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setInt(1, Integer.parseInt(extractValue(values, "doctor_id")));
                preparedStatement.setInt(2, Integer.parseInt(extractValue(values, "patient_id")));
                preparedStatement.setString(3, extractValue(values, "medication"));
                preparedStatement.setString(4, extractValue(values, "dosage"));
                preparedStatement.setDate(5, Date.valueOf(extractValue(values, "issued_date")));
                break;
    
            default:
                return "Wrong table name!";
        }
    
        int rowsAffected = preparedStatement.executeUpdate();
        return "Inserted " + rowsAffected + " row(s) into " + tableName;
    }


    private String handleDelete(Connection connection, String tableName, String values) throws SQLException {
        String sql;
        PreparedStatement preparedStatement;
    
        switch (tableName.toLowerCase()) {
            case "doctor":
            case "patient":
            case "prescription":
                sql = "DELETE FROM " + tableName + " WHERE id = ?";
                preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setInt(1, Integer.parseInt(extractValue(values, "id")));
                break;
    
            default:
                return "Wrong table name!";
        }
        int rowsAffected = preparedStatement.executeUpdate();
        return "Deleted " + rowsAffected + " row(s) from " + tableName;
    }

    private String handleUpdate(Connection connection, String tableName, String values) throws SQLException {
        String sql;
        PreparedStatement preparedStatement;
    
        switch (tableName.toLowerCase()) {
            case "doctor":
                sql = "UPDATE doctor SET name = ?, speciality = ?, phone_number = ? WHERE id = ?";
                preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, extractValue(values, "name"));
                preparedStatement.setString(2, extractValue(values, "speciality"));
                preparedStatement.setString(3, extractValue(values, "phone_number"));
                preparedStatement.setInt(4, Integer.parseInt(extractValue(values, "id")));
                break;
    
            case "patient":
                sql = "UPDATE patient SET name = ?, date_of_birth = ?, phone_number = ? WHERE id = ?";
                preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, extractValue(values, "name"));
                preparedStatement.setDate(2, Date.valueOf(extractValue(values, "date_of_birth")));
                preparedStatement.setString(3, extractValue(values, "phone_number"));
                preparedStatement.setInt(4, Integer.parseInt(extractValue(values, "id")));
                break;
    
            case "prescription":
                sql = "UPDATE prescription SET doctor_id = ?, patient_id = ?, medication = ?, dosage = ?, issued_date = ? WHERE id = ?";
                preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setInt(1, Integer.parseInt(extractValue(values, "doctor_id")));
                preparedStatement.setInt(2, Integer.parseInt(extractValue(values, "patient_id")));
                preparedStatement.setString(3, extractValue(values, "medication"));
                preparedStatement.setString(4, extractValue(values, "dosage"));
                preparedStatement.setDate(5, Date.valueOf(extractValue(values, "issued_date")));
                preparedStatement.setInt(6, Integer.parseInt(extractValue(values, "id")));
                break;
    
            default:
                return "Wrong table name!";
        }
    
        int rowsAffected = preparedStatement.executeUpdate();
        return "Updated " + rowsAffected + " row(s) in " + tableName;
    }

    private String handleSelect(Connection connection, String tableName) throws SQLException {
        StringBuilder result = new StringBuilder();
        String sql = "SELECT * FROM " + tableName;

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                result.append(metaData.getColumnName(i)).append("\t");
            }
            result.append("<br>");

            while (resultSet.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    result.append(resultSet.getString(i)).append("\t");
                }
                result.append("<br>");
            }
        }
        return result.toString();
    }

    private String extractValue(String values, String fieldName) {
        Pattern fieldPattern = Pattern.compile(fieldName + "=\\'([^']+)\\'", Pattern.CASE_INSENSITIVE);
        Matcher fieldMatcher = fieldPattern.matcher(values);

        if (fieldMatcher.find()) return fieldMatcher.group(1);
        return "";
    }

private void validateDoctorInput(String values) {
    String name = extractValue(values, "name");
    String speciality = extractValue(values, "speciality");
    String phoneNumber = extractValue(values, "phone_number");

    if (!name.matches("[a-zA-Z ]+"))  throw new IllegalArgumentException("Doctor name must contain only letters and spaces.");
    if (!speciality.matches("[a-zA-Z ]+"))  throw new IllegalArgumentException("Doctor speciality must contain only letters and spaces.");
    if (!phoneNumber.matches("\\+?\\d{10,15}")) throw new IllegalArgumentException("Doctor phone number must be a valid phone number.");
}

private void validatePatientInput(String values) {
    String name = extractValue(values, "name");
    String dateOfBirth = extractValue(values, "date_of_birth");
    String phoneNumber = extractValue(values, "phone_number");

    if (!name.matches("[a-zA-Z ]+")) throw new IllegalArgumentException("Patient name must contain only letters and spaces.");

    try {
        LocalDate.parse(dateOfBirth); 
    } catch (DateTimeParseException e) {
        throw new IllegalArgumentException("Patient date of birth must be in the format YYYY-MM-DD.");
    }

    if (!phoneNumber.matches("\\+?\\d{10,15}")) throw new IllegalArgumentException("Patient phone number must be a valid phone number.");
}

private void validatePrescriptionInput(String values) {
    String doctorId = extractValue(values, "doctor_id");
    String patientId = extractValue(values, "patient_id");
    String medication = extractValue(values, "medication");
    String dosage = extractValue(values, "dosage");
    String issuedDate = extractValue(values, "issued_date");

    if (!doctorId.matches("\\d+") || !patientId.matches("\\d+")) throw new IllegalArgumentException("Doctor ID and Patient ID must be numeric.");
    if (!medication.matches("[a-zA-Z ]+")) throw new IllegalArgumentException("Medication must contain only letters and spaces.");
    if (!dosage.matches("[0-9]+(\\.[0-9]+)?")) throw new IllegalArgumentException("Dosage must be a valid number.");

    try {
        LocalDate.parse(issuedDate); 
    } catch (DateTimeParseException e) {
        throw new IllegalArgumentException("Issued date must be in the format YYYY-MM-DD.");
    }
}

    @PostMapping("/execute")
    public String executeQuery(@RequestParam String query) {
        StringBuilder result = new StringBuilder();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            if (query.trim().toLowerCase().startsWith("select")) {
                try (ResultSet resultSet = statement.executeQuery(query)) {
                    int columnCount = resultSet.getMetaData().getColumnCount();
                    while (resultSet.next()) {
                        for (int i = 1; i <= columnCount; i++) {
                            result.append(resultSet.getString(i)).append(" ");
                        }
                        result.append("<br>");
                    }
                }
            } else {
                int rowsAffected = statement.executeUpdate(query);
                result.append("Rows affected: ").append(rowsAffected);
            }
        } catch (SQLException e) {
            result.append("Error: ").append(e.getMessage());
        }
        return result.toString();
    }
    }