package orm.persistence;

import orm.annotations.Column;
import orm.annotations.Table;
import orm.utils.OrmConnection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Persistence {

    public <C> void createTable(C c) throws IllegalAccessException {
        var map = new HashMap<String, Object>();
        Class<C> clazz = (Class<C>) c.getClass();
        var tableClass = clazz.getAnnotation(Table.class);
        String tableName = null;
        if (tableClass != null) {
            tableName = tableClass.value();
        }

        var fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            var varInstance = field.getAnnotation(Column.class);
            if (varInstance != null) {
                field.setAccessible(true);
                map.put(varInstance.value(), field.get(c));
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

        var annotation = clazz.getAnnotation(Table.class);
        var query = "";
        if (annotation != null) {
            query = String.format("SELECT * FROM %s", annotation.value());
        }

        return executeQuery(query, clazz);
    }

    public <C> void createInsert(C c) throws IllegalAccessException {
        Class<C> clazz = (Class<C>) c.getClass();

        var annotation = clazz.getAnnotation(Table.class);
        var query = "";
        if (annotation != null) {
            query = String.format("INSERT INTO %s (%s) VALUES (%s)", annotation.value(), "%s", "%s");
        }

        var fields = clazz.getDeclaredFields();
        var map = new HashMap<String, String>();
        for (Field field : fields) {
            field.setAccessible(true);
            var ant = field.getAnnotation(Column.class);
            if (ant != null && !ant.value().contains("id")) {
                map.put(ant.value(), field.get(c).toString());
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
                        var member = field.getAnnotation(Column.class);
                        Object o = rs.getObject(member.value());
                        if(o instanceof Date d){
                            o = LocalDateTime.of(d.toLocalDate(), LocalTime.now());
                        }
                        return o;
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }).toArray();

                Constructor[] cons = null;
                cons = clazz.getConstructors();

                Class<?>[] parameterTypes = cons[0].getParameterTypes();

                Object[] args = new Object[parameterTypes.length];

                for (int i = 0; i < args.length; i++) {
                    args[i] = parameterTypes[i].cast(initargs[i]);
                }
                result.add((C) cons[0].newInstance(args));
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
