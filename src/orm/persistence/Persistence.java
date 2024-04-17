package orm.persistence;

import orm.annotations.Column;
import orm.annotations.Table;
import orm.model.Persona;
import orm.utils.OrmConnection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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

    public <C> List<C> executeQuery(Class<C> clazz) throws IllegalAccessException {
        Class<Persona> personaClass = Persona.class;

        var annotation = personaClass.getAnnotation(Table.class);
        var query = "";
        if (annotation != null) {
            query = String.format("SELECT * FROM %s", annotation.value());
        }

        return executeQuery(query, clazz);
    }

    public void createInsert(Persona persona) throws IllegalAccessException {
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

    private <C> List<C> executeQuery(final String query, final Class<C> clazz) {
        Connection connection;
        final List<C> result = new LinkedList<>();
        try {
            connection = OrmConnection.getConnection();
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(query);

            while (rs.next()) {

                var fields = clazz.getDeclaredFields();


                Object[] initargs = Arrays.stream(fields).map(field -> {
                    try {
                        return rs.getObject(field.getName());
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }).toArray();

                Constructor[] cons = null;
                cons = clazz.getConstructors();
                result.add((C) cons[1].newInstance(initargs));


            }

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        return result;
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
