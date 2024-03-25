package orm.persistence;

import orm.annotations.Column;
import orm.annotations.Table;
import orm.model.Persona;
import orm.utils.OrmConnection;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class Persistence {

    public void createTable(Persona persona) throws IllegalAccessException {
        var map = new HashMap<String, Object>();
        Class<Persona> personaClass = Persona.class;
        var tableClass = personaClass.getAnnotation(Table.class);
        String tableName = null;
        if (tableClass != null) {
            tableName = tableClass.value();
        }

        var fields = personaClass.getDeclaredFields();
        for (Field field : fields) {
            var varInstance = field.getAnnotation(Column.class);
            if (varInstance != null) {
                field.setAccessible(true);
                map.put(varInstance.value(), field.get(persona));
            }
        }
        var sb = String.format("CREATE TABLE %s (%s);", tableName, "%s");
        var cols = new StringBuilder();
        map.keySet().forEach(s -> cols.append(columnDeclaration(s, map.get(s))).append(","));
        String q1 = String.format(sb, cols.delete(cols.length() - 1, cols.length()));
        execute(q1, Map.of(), Boolean.TRUE);
    }

    private String columnDeclaration(String name, Object value) {
        if (value instanceof Integer v) {
            return String.format("%s INT", name);
        } else if (value instanceof Long v) {
            return String.format("%s INT PRIMARY KEY AUTO_INCREMENT", name);
        } else if (value instanceof String v) {
            return String.format("%s VARCHAR(50)", name);
        } else if (value instanceof LocalDateTime v) {
            return String.format("%s DATE", name);
        } else {
            return "";
        }
    }


    public void createQuery(Persona persona) throws IllegalAccessException {
        Class<Persona> personaClass = Persona.class;

        var annotation = personaClass.getAnnotation(Table.class);
        var query = "";
        if (annotation != null) {
            query = String.format("INSERT INTO %s (%s) VALUES (%s)", annotation.value(), "%s", "%s");
        }

        var fields = personaClass.getDeclaredFields();
        var map = new HashMap<String, String>();
        for (Field field : fields) {
            field.setAccessible(true);
            var ant = field.getAnnotation(Column.class);
            if (ant != null && !ant.value().contains("id")) {
                map.put(ant.value(), field.get(persona).toString());
            }
        }

        var cols = new StringBuilder();
        var values = new StringBuilder();
        map.keySet().forEach(s -> {
            cols.append(String.format("%s,", s));
            values.append("?,");
        });
        cols.delete(cols.length() - 1, cols.length());
        values.delete(values.length() - 1, values.length());
        String q2 = String.format(query, cols, values);
        execute(q2, map, Boolean.FALSE);
    }

    private void execute(String query, Map<String, String> map, boolean isDDL) {
        Connection connection;
        try {
            connection = OrmConnection.getConnection();
            if (isDDL) {
                Statement statement = connection.createStatement();
                statement.execute(query);
            } else {
                PreparedStatement statement = connection.prepareStatement(query);
                var keys = map.keySet();
                int i = 1;
                for (String key : keys) {
                    statement.setObject(i++, map.get(key));
                }
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
