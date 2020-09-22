/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.michael.framework;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 *
 * @author michael
 */
public final class GenericDao {

    private static JdbcFactory jdbcFactory;
    private static Map<Class<?>, List<?>> cache;

    private final String databaseId;
    private CriteriaBuilder criteriaBuilder;

    private String getFieldNameInDatabase(Field field) {
        String result = null;
        if (field.isAnnotationPresent(Column.class)) {
            result = ((Column) (field.getAnnotation(Column.class))).name();
        } else if (field.isAnnotationPresent(JoinColumn.class)) {
            result = ((JoinColumn) (field.getAnnotation(JoinColumn.class))).name();
        } else if (field.isAnnotationPresent(ManyToMany.class)) {
            result = ((ManyToMany) field.getAnnotation(ManyToMany.class)).joinColumn();
        } else {
            result = field.getName();
        }
        return result;
    }

    private String getClassTableName(Class<?> classe) {
        String result = null;
        if (classe.isAnnotationPresent(Table.class)) {
            result = ((Table) (classe.getAnnotation(Table.class))).name();
        } else {
            result = classe.getSimpleName();
        }
        return result;
    }

    public int delete(Object instance) throws SQLException, IllegalArgumentException, IllegalAccessException {
        Connection connection = getConnection();
        connection.setAutoCommit(false);
        try {
            int rowsUpdated = delete(connection, instance, null, null);
            connection.commit();
            return rowsUpdated;
        } catch (IllegalAccessException | IllegalArgumentException | SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.close();
        }
    }

    public Object selectById(Object id) throws SQLException, InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, NoSuchFieldException {
        String tableName = getClassTableName(criteriaBuilder.getClazz());
        Field fieldHoldingId = getIdField(criteriaBuilder.getClazz());
        String idColumnName = fieldHoldingId.isAnnotationPresent(Column.class)
                ? fieldHoldingId.getAnnotation(Column.class).name()
                : fieldHoldingId.getName();
        criteriaBuilder.add(Restrictions.eq(tableName, idColumnName, id), LogicalOperator.NONE);
        Connection c = getConnection();
        try {
            List<Object> objects = select(c, criteriaBuilder.getClazz(), criteriaBuilder, null, null);
            if(objects.isEmpty()) {
                return null;
            }
            return objects.get(0);
        } catch (SQLException | InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchFieldException e) {
            throw e;
        } finally {
            c.close();
        }
    }

    // manualSql and psArgs are exclusively for ManyToMany
    private int delete(Connection connection, Object instance, String manualSql, Object[] psArgs)
            throws IllegalArgumentException, IllegalAccessException, SQLException {
        Class<?> classe = instance.getClass();
        String tableName = getClassTableName(classe);
        List<Field> fields = getAllFields(classe);
        Field fieldHoldingId = null;
        List<Field> oneToManyFields = new ArrayList<Field>();
        List<Field> manyToManyFields = new ArrayList<Field>();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Id.class)) {
                fieldHoldingId = field;
            }
            if (field.isAnnotationPresent(OneToMany.class)) {
                oneToManyFields.add(field);
            }
            if (field.isAnnotationPresent(ManyToMany.class)) {
                manyToManyFields.add(field);
            }
        }

        if (fieldHoldingId == null) {
            throw new RuntimeException("L'id de la classe" + classe.getName() + " doit être annoté par @Id");
        }

        String fieldHoldingIdName = getFieldNameInDatabase(fieldHoldingId);
        String sql = null;
        if (manualSql != null) {
            sql = manualSql;
        } else {
            sql = "DELETE FROM " + tableName + " WHERE " + fieldHoldingIdName + " = ?";
        }

        System.out.println(sql);

        for (Field oneToManyField : oneToManyFields) {
            oneToManyField.setAccessible(true);
            List<?> list = (List<?>) oneToManyField.get(instance);
            if (list != null) {
                for (Object object : list) {
                    delete(connection, object, null, null);
                }
            }
        }

        for (Field manyToManyField : manyToManyFields) {
            ParameterizedType genericType = (ParameterizedType) manyToManyField.getGenericType();
            Class<?> genericTypeClass = (Class<?>) genericType.getActualTypeArguments()[0];
            String joinTable = ((ManyToMany) manyToManyField.getAnnotation(ManyToMany.class)).joinTable();
            String joinColumn = ((ManyToMany) manyToManyField.getAnnotation(ManyToMany.class)).joinColumn();
            String inverseJoinColumn = ((ManyToMany) manyToManyField.getAnnotation(ManyToMany.class))
                    .inverseJoinColumn();

            Field mnyToManyFieldId = null;

            for (Field field : getAllFields(genericTypeClass)) {
                if (field.isAnnotationPresent(Id.class)) {
                    mnyToManyFieldId = field;
                    break;
                }
            }

            if (mnyToManyFieldId == null) {
                throw new RuntimeException("no Id found inside " + genericTypeClass);
            }

            manyToManyField.setAccessible(true);
            mnyToManyFieldId.setAccessible(true);
            fieldHoldingId.setAccessible(true);
            List<?> list = (List<?>) manyToManyField.get(instance);
            String manSql = "DELETE FROM " + joinTable + " WHERE " + joinTable + "." + joinColumn + " = ?";
            PreparedStatement mnyMnyPs = connection.prepareStatement(manSql);
            mnyMnyPs.setObject(1, fieldHoldingId.get(instance));  
            mnyMnyPs.executeUpdate();
        }

        PreparedStatement ps = connection.prepareStatement(sql);
        fieldHoldingId.setAccessible(true);
        if (manualSql != null) {
            for (int i = 0; i < psArgs.length; i++) {
                ps.setObject(i + 1, psArgs[i]);
            }
        } else {
            ps.setObject(1, fieldHoldingId.get(instance));
        }

        return ps.executeUpdate();
    }

    private Field getIdField(Class<?> clazz) {
        for (Field field : getAllFields(clazz)) {
            if (field.isAnnotationPresent(Id.class)) {
                return field;
            }
        }
        return null;
    }

    // manualSql and psArgs are exclusively for ManyToMany
    private Integer insert(Connection connection, Object instance, String manualSql, Object[] psArgs)
            throws IllegalArgumentException, IllegalAccessException, SQLException, InstantiationException,
            InvocationTargetException, NoSuchFieldException {
        Class<?> classe = instance.getClass();

        String tableName = getClassTableName(classe);

        List<Field> fields = getAllFields(classe);
        Field fieldHoldingId = null;
        List<Field> oneToManyFields = new ArrayList<Field>();
        List<Field> manyToOneFields = new ArrayList<Field>();
        List<Field> syncFields = new ArrayList<Field>();
        List<Field> manyToManyFields = new ArrayList<Field>();
        Map<Field, Integer> generatedKeys = new HashMap<Field, Integer>();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Id.class)) {
                fieldHoldingId = field;
                if (field.getAnnotation(Id.class).autoGenerated() == false) {
                    syncFields.add(field);
                }
                continue;
            }
            if (field.isAnnotationPresent(OneToMany.class)) {
                oneToManyFields.add(field);
            }
            if (field.isAnnotationPresent(ManyToOne.class)) {
                manyToOneFields.add(field);
            }
            if (field.isAnnotationPresent(ManyToMany.class)) {
                manyToManyFields.add(field);
            }
            if (!field.isAnnotationPresent(Transient.class) && !field.isAnnotationPresent(OneToMany.class)
                    && !field.isAnnotationPresent(ManyToOne.class) && !field.isAnnotationPresent(ManyToMany.class)) {
                syncFields.add(field);
            }
        }

        if (fieldHoldingId == null) {
            throw new RuntimeException("L'id de la classe" + classe.getName() + " doit être annoté par @Id");
        }

        for (Field manyToOneField : manyToOneFields) {
            manyToOneField.setAccessible(true);
            Object manyToOneInstance = manyToOneField.get(instance);
            if (manyToOneInstance == null) {
                continue;
            }
            CriteriaBuilder cb = new CriteriaBuilder(manyToOneField.getType());
            String embeddedFieldTableName = getClassTableName(manyToOneField.getType());
            Field fieldId = getIdField(manyToOneField.getType());
            fieldId.setAccessible(true);
            String fieldNameInQuery = getFieldNameInDatabase(fieldId);
            cb.add(Restrictions.eq(embeddedFieldTableName, fieldNameInQuery, fieldId.get(manyToOneInstance)),
                    LogicalOperator.NONE);
            List<?> list = select(connection, manyToOneField.getType(), cb, null, null);
            if (list.isEmpty()) {
                generatedKeys.put(manyToOneField, insert(connection, manyToOneInstance, null, null));
            }
        }

        String sql = "INSERT INTO " + tableName + " (";
        String sqlFieldName;
        for (int i = 0; i < syncFields.size(); i++) {
            sqlFieldName = getFieldNameInDatabase(syncFields.get(i));
            sql += sqlFieldName;
            if (i < syncFields.size() - 1) {
                sql += ",";
            }
        }

        for (Field manyToOneField : manyToOneFields) {
            manyToOneField.setAccessible(true);
            if (manyToOneField.get(instance) != null) {
                sqlFieldName = getFieldNameInDatabase(manyToOneField);
                sql += "," + sqlFieldName;
                sql += ",";
            }
        }

        if (sql.charAt(sql.length() - 1) == ',') {
            sql = sql.substring(0, sql.length() - 1);
        }

        sql += ") VALUES(";

        for (int i = 0; i < syncFields.size(); i++) {
            sql += "?";
            if (i < syncFields.size() - 1) {
                sql += ",";
            }
        }

        for (Field manyToOneField : manyToOneFields) {
            manyToOneField.setAccessible(true);
            if (manyToOneField.get(instance) != null) {
                sql += ",?";
                sql += ",";
            }
        }

        if (sql.charAt(sql.length() - 1) == ',') {
            sql = sql.substring(0, sql.length() - 1);
        }

        sql += ")";

        if (manualSql != null) {
            sql = manualSql;
        }

        PreparedStatement preparedStatement = null;
        DatabaseInformation dbInfo = jdbcFactory.getDatabasesInformations().get(databaseId);
        String database = dbInfo.getUrl().split(":")[1];
        if (database.equalsIgnoreCase("oracle")) {
            preparedStatement = connection.prepareStatement(sql);
        } else {
            preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        }

        if (manualSql == null) {
            int index = 0;
            for (Field syncField : syncFields) {
                syncField.setAccessible(true);
                preparedStatement.setObject(++index, syncField.get(instance));
            }

            for (Field manyToOneField : manyToOneFields) {
                manyToOneField.setAccessible(true);
                Object manyToOneInstance = manyToOneField.get(instance);
                if (manyToOneInstance == null) {
                    continue;
                }
                Field fieldId = getIdField(manyToOneField.getType());
                fieldId.setAccessible(true);
                if (generatedKeys.get(manyToOneField) != null) {
                    fieldId.set(manyToOneInstance, generatedKeys.get(manyToOneField));
                }
                preparedStatement.setObject(++index, fieldId.get(manyToOneInstance));
            }
        } else {
            for (int i = 0; i < psArgs.length; i++) {
                preparedStatement.setObject(i + 1, psArgs[i]);
            }
        }

        System.out.println(sql);

        preparedStatement.executeUpdate();
        Integer generatedKey = null;
        ResultSet generatedKeyResultSet = null;
        if (database.equalsIgnoreCase("oracle")) {
            preparedStatement = connection.prepareCall(
                    "SELECT " + ((Table) classe.getAnnotation(Table.class)).sequence() + ".CURRVAL FROM DUAL");
            generatedKeyResultSet = preparedStatement.executeQuery();
        } else {
            generatedKeyResultSet = preparedStatement.getGeneratedKeys();
        }

        if (generatedKeyResultSet.next()) {
            try {
                generatedKey = ((Integer) generatedKeyResultSet.getObject(1)).intValue();
            } catch (ClassCastException e) {
                try {
                    generatedKey = ((BigDecimal) generatedKeyResultSet.getObject(1)).intValue();
                } catch (ClassCastException ex) {
                    generatedKey = ((BigInteger) generatedKeyResultSet.getObject(1)).intValue();
                }
            }
            fieldHoldingId.setAccessible(true);
            fieldHoldingId.set(instance, generatedKey);
        }
        generatedKeyResultSet.close();

        for (Field oneToManyField : oneToManyFields) {
            oneToManyField.setAccessible(true);
            List<?> list = (List<?>) oneToManyField.get(instance);
            if (list != null) {
                for (Object object : list) {
                    insert(connection, object, null, null);
                }
            }
        }

        for (Field manyToManyField : manyToManyFields) {
            ParameterizedType genericType = (ParameterizedType) manyToManyField.getGenericType();
            Class<?> genericTypeClass = (Class<?>) genericType.getActualTypeArguments()[0];
            String joinTable = ((ManyToMany) manyToManyField.getAnnotation(ManyToMany.class)).joinTable();
            String joinColumn = ((ManyToMany) manyToManyField.getAnnotation(ManyToMany.class)).joinColumn();
            String inverseJoinColumn = ((ManyToMany) manyToManyField.getAnnotation(ManyToMany.class))
                    .inverseJoinColumn();

            Field mnyToManyFieldId = null;

            for (Field field : getAllFields(genericTypeClass)) {
                if (field.isAnnotationPresent(Id.class)) {
                    mnyToManyFieldId = field;
                    break;
                }
            }

            if (mnyToManyFieldId == null) {
                throw new RuntimeException("no Id found inside " + genericTypeClass);
            }

            manyToManyField.setAccessible(true);
            mnyToManyFieldId.setAccessible(true);
            fieldHoldingId.setAccessible(true);
            List<?> list = (List<?>) manyToManyField.get(instance);
            String manSql = "INSERT INTO " + joinTable + "(" + joinColumn + "," + inverseJoinColumn + ") VALUES (?, ?)";
            Object[] prepArgs = new Object[2];
            prepArgs[0] = fieldHoldingId.get(instance);
            if (list != null) {
                for (Object object : list) {
                    // Check if object exists in db
                    CriteriaBuilder tempCb = new CriteriaBuilder(genericTypeClass);
                    tempCb.add(
                            Restrictions.eq(getClassTableName(genericTypeClass),
                                    getFieldNameInDatabase(mnyToManyFieldId), mnyToManyFieldId.get(object)),
                            LogicalOperator.NONE);
                    if (select(connection, genericTypeClass, tempCb, null, null).isEmpty()) {
                        prepArgs[1] = insert(connection, object, null, null);
                    } else {
                        prepArgs[1] = mnyToManyFieldId.get(object);
                    }
                    insert(connection, object, manSql, prepArgs);
                }
            }
        }

        return generatedKey;
    }

    public void insert(Object instance) throws SQLException, IllegalArgumentException, IllegalAccessException,
            InstantiationException, InvocationTargetException, NoSuchFieldException {
        Connection connection = getConnection();
        connection.setAutoCommit(false);
        try {
            insert(connection, instance, null, null);
            connection.commit();
        } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchFieldException
                | InvocationTargetException | SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.close();
        }
    }

    private void update(Connection connection, Object instance)
            throws SQLException, IllegalArgumentException, IllegalAccessException {
        Class<?> classe = instance.getClass();

        String tableName = getClassTableName(classe);

        String sql = "UPDATE " + tableName + " SET ";

        List<Field> fields = getAllFields(classe);
        Field fieldHoldingId = null;
        List<Field> oneToManyFields = new ArrayList<Field>();
        List<Field> manyToOneFields = new ArrayList<Field>();
        List<Field> syncFields = new ArrayList<Field>();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Id.class)) {
                fieldHoldingId = field;
            }
            if (field.isAnnotationPresent(OneToMany.class)) {
                oneToManyFields.add(field);
            }
            if (field.isAnnotationPresent(ManyToOne.class)) {
                manyToOneFields.add(field);
            }
            if (!field.isAnnotationPresent(Transient.class) && !field.isAnnotationPresent(OneToMany.class)
                    && !field.isAnnotationPresent(ManyToOne.class) && !field.isAnnotationPresent(ManyToMany.class)) {
                syncFields.add(field);
            }
        }

        if (fieldHoldingId == null) {
            throw new RuntimeException("L'id de la classe" + classe.getName() + " doit être annoté par @Id");
        }

        String sqlFieldName;
        for (int i = 0; i < syncFields.size(); i++) {
            sqlFieldName = getFieldNameInDatabase(syncFields.get(i));
            sql += sqlFieldName + " = ?";
            if (i < syncFields.size() - 1) {
                sql += ", ";
            }
        }

        if (!manyToOneFields.isEmpty()) {
            sql += ",";
        }

        if (sql.charAt(sql.length() - 1) == ',') {
            sql = sql.substring(0, sql.length() - 1);
        }

        String idColumnName = getFieldNameInDatabase(fieldHoldingId);
        sql += " WHERE " + idColumnName + " = ?";

        System.out.println(sql);
        PreparedStatement preparedStatement = connection.prepareStatement(sql);

        int index = 1;
        for (Field syncField : syncFields) {
            syncField.setAccessible(true);
            try {
                preparedStatement.setObject(index++, syncField.get(instance));
            } catch (SQLException e) {
                throw e;
            }
        }

        fieldHoldingId.setAccessible(true);
        preparedStatement.setObject(index, fieldHoldingId.get(instance));

        preparedStatement.executeUpdate();
        for (Field manyToOneField : manyToOneFields) {
            manyToOneField.setAccessible(true);
            Object manyToOneInstance = manyToOneField.get(instance);
            if(manyToOneInstance != null) {
                update(connection, manyToOneField.get(instance));
            }
        }

        for (Field oneToManyField : oneToManyFields) {
            oneToManyField.setAccessible(true);
            List<?> list = (List<?>) oneToManyField.get(instance);
            if (list != null) {
                for (Object object : list) {
                    update(connection, object);
                }
            }
        }

    }

    public void update(Object instance) throws SQLException, IllegalArgumentException, IllegalAccessException {
        Connection connection = getConnection();
        connection.setAutoCommit(false);
        try {
            update(connection, instance);
            connection.commit();
        } catch (IllegalAccessException | IllegalArgumentException | SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.close();
        }
    }

    public List<?> getInCache(Class<?> clazz) throws NotCachedException {
        if (!clazz.isAnnotationPresent(Cache.class)) {
            throw new NotCachableException(clazz);
        }
        List<?> result = cache.get(clazz);
        if (result == null) {
            throw new NotCachedException(clazz);
        }
        return result;
    }

    public void registerIntoCache(Class<?> clazz) throws SQLException, InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, NoSuchFieldException {
        if (!clazz.isAnnotationPresent(Cache.class)) {
            throw new NotCachableException(clazz);
        }
        CriteriaBuilder cb = new CriteriaBuilder(clazz);
        cache.put(clazz, select(getConnection(), clazz, cb, null, null));
    }

    public GenericDao(String id) throws ParserConfigurationException, IOException, SAXException {
        if (jdbcFactory == null) {
            GenericDao.jdbcFactory = new JdbcFactory();
        }
        if (cache == null) {
            cache = new HashMap<Class<?>, List<?>>();
        }

        this.databaseId = id;
    }

    public CriteriaBuilder getCriteriaBuilder(Class<?> clazz) {
        this.criteriaBuilder = new CriteriaBuilder(clazz);
        return this.criteriaBuilder;
    }

    public Connection getConnection() throws SQLException {
        return jdbcFactory.getConnection(databaseId);
    }

    /*
     * @Param directJoinSql and @Param manyToManyParentClass are used inside
     * ManyToMany logic otherwise its not used
     */
    private List<Object> select(Connection connection, Class<?> instanceClass, CriteriaBuilder criteriaBuilder,
            String directJoinSql, Class<?> manyToManyParentClass) throws SQLException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchFieldException {
        Class<?> classe = instanceClass;
        List<Object> result = new ArrayList<Object>();
        String tableName = getClassTableName(classe);
        String sql = "SELECT ";

        boolean paginationOn = criteriaBuilder.getPagination() != null;
        String database = "";

        if (paginationOn) {
            DatabaseInformation dbInfo = jdbcFactory.getDatabasesInformations().get(databaseId);
            database = dbInfo.getUrl().split(":")[1];
            if (database.equalsIgnoreCase("oracle")) {
                sql += "* FROM (SELECT ";
            }
        }

        List<Field> fields = getAllFields(classe);
        Field fieldHoldingId = null;
        List<Field> oneToManyFields = new ArrayList<Field>();
        List<Field> manyToOneFields = new ArrayList<Field>();
        List<Field> syncFields = new ArrayList<Field>();
        List<Field> manyToManyFields = new ArrayList<Field>();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Id.class)) {
                fieldHoldingId = field;
            }
            if (field.isAnnotationPresent(OneToMany.class)) {
                oneToManyFields.add(field);
            }
            if (field.isAnnotationPresent(ManyToOne.class)) {
                manyToOneFields.add(field);
            }
            if (field.isAnnotationPresent(ManyToMany.class)) {
                manyToManyFields.add(field);
            }
            if (!field.isAnnotationPresent(Transient.class) && !field.isAnnotationPresent(OneToMany.class)
                    && !field.isAnnotationPresent(ManyToOne.class) && !field.isAnnotationPresent(ManyToMany.class)) {
                syncFields.add(field);
            }
        }

        if (fieldHoldingId == null) {
            throw new RuntimeException("L'id de classe doit être annoté par @Id");
        }

        String tableNameNotEscaped = classe.isAnnotationPresent(Table.class)
                ? ((Table) classe.getAnnotation(Table.class)).name()
                : classe.getSimpleName();

        String sqlFieldName;
        for (int i = 0; i < syncFields.size(); i++) {
            sqlFieldName = syncFields.get(i).isAnnotationPresent(Column.class)
                    ? syncFields.get(i).getAnnotation(Column.class).name()
                    : syncFields.get(i).getName();
            if (syncFields.get(i).isAnnotationPresent(Id.class)) {
                sql += "distinct ";
            }
            sql += tableName + "." + sqlFieldName + " as \"" + tableNameNotEscaped + "." + sqlFieldName + "\"";
            if (i < syncFields.size() - 1) {
                sql += ", ";
            }
        }

        if (!manyToOneFields.isEmpty()) {
            sql += ",";
        }

        for (Field field : manyToOneFields) {
            sql = getFieldSqlManyToOne(field.getType(), sql);
        }

        if (sql.charAt(sql.length() - 1) == ',') {
            sql = sql.substring(0, sql.length() - 1);
        }

        if (paginationOn) {
            DatabaseInformation dbInfo = jdbcFactory.getDatabasesInformations().get(databaseId);
            database = dbInfo.getUrl().split(":")[1];
            if (database.equalsIgnoreCase("oracle")) {
                sql += ", rownum as rn";
            }
        }

        sql += " FROM " + tableName;

        if (directJoinSql != null && !directJoinSql.isEmpty()) {
            sql += " " + directJoinSql;
        }

        Map<String, InfoForLeftJoin> correspBetweenTableAndInfo = new HashMap<String, InfoForLeftJoin>();
        correspBetweenTableAndInfo.put(tableName, new InfoForLeftJoin(classe, null, null, null));

        StringBuilder sb = new StringBuilder(1000);
        sb.append(sql);
        sql = getLeftJoinSql(classe, sb, correspBetweenTableAndInfo).toString();

        Criteria criteria = null;
        for (int i = 0; i < criteriaBuilder.getCriterias().size(); i++) {
            if (i == 0) {
                sql += " WHERE";
            }
            criteria = criteriaBuilder.getCriterias().get(i);
            sql += " " + criteria.toSqlString();
            sql += " " + criteriaBuilder.getLogicalOperators().get(i);
        }

        if (paginationOn) {
            int page = criteriaBuilder.getPagination().getPage();
            int nombreResultat = criteriaBuilder.getPagination().getNombreResultat();
            if (database.equalsIgnoreCase("mysql") || database.equalsIgnoreCase("mariadb")) {
                sql += " LIMIT " + ((page - 1) * nombreResultat) + ", " + nombreResultat;
            } else if (database.equalsIgnoreCase("postgresql")) {
                sql += " OFFSET " + ((page - 1) * nombreResultat) + " LIMIT " + nombreResultat;
            } else if (database.equalsIgnoreCase("oracle")) {
                sql += ") WHERE rn BETWEEN " + ((page * nombreResultat) - nombreResultat + 1) + " AND "
                        + (nombreResultat * page);
            } else {
                throw new RuntimeException("Base de donnée non supportée");
            }
        }

        System.out.println(sql);

        PreparedStatement ps = null;
        ResultSet resultSet = null;
        try {
            ps = connection.prepareStatement(sql);
            int index = 1;
            for (int i = 0; i < criteriaBuilder.getCriterias().size(); i++) {
                criteria = criteriaBuilder.getCriterias().get(i);
                if (criteria instanceof SimpleExpression) {
                    SimpleExpression se = (SimpleExpression) criteria;
                    if(se.getOperator() == "LIKE") {
                        ps.setObject(index++, "%"+(String.valueOf(se.getValue()))+"%");
                    } else {
                        ps.setObject(index++, se.getValue());
                    }
                } else if (criteria instanceof IntervalExpression) {
                    IntervalExpression ie = (IntervalExpression) criteria;
                    if(ie.getMin() instanceof java.util.Date) {
                        Date min = (java.util.Date)ie.getMin();
                        Date max = (java.util.Date)ie.getMax();
                        ps.setDate(index++, new java.sql.Date(min.getTime()));
                        ps.setDate(index++, new java.sql.Date(max.getTime()));
                    } else {
                        ps.setObject(index++, ie.getMin());
                        ps.setObject(index++, ie.getMax());
                    }
                }
            }
            resultSet = ps.executeQuery();
            ResultSetMetaData rsmd = resultSet.getMetaData();
            Map<String, Set<String>> tablesInvolved = getTablesInvolvedAndItsFields(rsmd);

            while (resultSet.next()) {
                // Create an instance and set its field for each table in tablesInvolved, and
                // eventually set the parent field container as the instance is the parent
                // field value
                for (Map.Entry<String, Set<String>> entry : tablesInvolved.entrySet()) {
                    String table = entry.getKey();
                    // Skip if Rownum
                    if (table.equalsIgnoreCase("RN")) {
                        continue;
                    }
                    InfoForLeftJoin matchingInfo = correspBetweenTableAndInfo.get(table);
                    int i = 0;
                    Class<?> currentClass = matchingInfo.getClazz();
                    /**
                     * Table => Instance | Set each Field for the instance
                     */
                    for (String fieldName : entry.getValue()) {
                        if (i == 0) {
                            try {
                                matchingInfo.setInstance(currentClass.getDeclaredConstructor().newInstance());
                            } catch (NoSuchMethodException e) {
                                throw new RuntimeException("No default constructor found inside " + currentClass);
                            }
                        }
                        Field matchingField = getFieldInsideClass(currentClass, fieldName);
                        matchingField.setAccessible(true);
                        // objectFromDb represents a field value
                        Object objectFromDb = resultSet.getObject(table + "." + fieldName);
                        if (objectFromDb instanceof java.math.BigDecimal) {
                            String fieldTypeName = matchingField.getType().getSimpleName();
                            if (fieldTypeName.equals("int") || fieldTypeName.equals("Integer")) {
                                objectFromDb = ((BigDecimal) objectFromDb).intValue();
                            } else if (fieldTypeName.equals("short") || fieldTypeName.equals("Short")) {
                                objectFromDb = ((BigDecimal) objectFromDb).shortValue();
                            } else if (fieldTypeName.equals("long") || fieldTypeName.equals("Long")) {
                                objectFromDb = ((BigDecimal) objectFromDb).longValue();
                            }
                        }
                        matchingField.set(matchingInfo.getInstance(), objectFromDb);
                        i++;
                    }
                    // If current Instance has a parent container
                    Field fieldConteneur = matchingInfo.getFieldConteneur();
                    if (fieldConteneur != null) {
                        fieldConteneur.setAccessible(true);
                        String conteneurTableName = fieldConteneur.getDeclaringClass().getSimpleName();
                        if (fieldConteneur.getDeclaringClass().isAnnotationPresent(Table.class)) {
                            conteneurTableName = ((Table) fieldConteneur.getDeclaringClass().getAnnotation(Table.class))
                                    .name();
                        }

                        fieldConteneur.set(correspBetweenTableAndInfo.get(conteneurTableName).getInstance(),
                                matchingInfo.getInstance());
                    }
                }

                for (Field field : oneToManyFields) {
                    ParameterizedType genericType = (ParameterizedType) field.getGenericType();
                    Class<?> genericTypeClass = (Class<?>) genericType.getActualTypeArguments()[0];
                    CriteriaBuilder cb = new CriteriaBuilder(genericTypeClass);
                    System.out.println(genericTypeClass);
                    Field embeddedField = genericTypeClass
                            .getDeclaredField(field.getAnnotation(OneToMany.class).mappedBy());
                    fieldHoldingId.setAccessible(true);
                    String embeddedFieldTableName = getClassTableName(genericTypeClass);
                    // WHERE embeddedFieldTableName.getFieldNameInDatabase(embeddedField,
                    // this.dbCharacter) =
                    // fieldHoldingId.get(correspBetweenTableAndInfo.get(tableName).getInstance())
                    cb.add(Restrictions.eq(embeddedFieldTableName, getFieldNameInDatabase(embeddedField),
                            fieldHoldingId.get(correspBetweenTableAndInfo.get(tableName).getInstance())),
                            LogicalOperator.NONE);
                    List<?> list = select(connection, genericTypeClass, cb, null, null);
                    field.setAccessible(true);
                    field.set(correspBetweenTableAndInfo.get(tableName).getInstance(), list);
                }

                for (Field field : manyToManyFields) {
                    ParameterizedType genericType = (ParameterizedType) field.getGenericType();
                    Class<?> genericTypeClass = (Class<?>) genericType.getActualTypeArguments()[0];
                    Field mnyToManyFieldId = null;

                    for (Field f : getAllFields(genericTypeClass)) {
                        if (f.isAnnotationPresent(Id.class)) {
                            mnyToManyFieldId = f;
                            break;
                        }
                    }

                    if (mnyToManyFieldId == null) {
                        throw new RuntimeException("no Id found inside " + genericTypeClass);
                    }

                    mnyToManyFieldId.setAccessible(true);

                    // Avoid infinite recursion
                    if (genericTypeClass.equals(manyToManyParentClass)) {
                        continue;
                    }

                    CriteriaBuilder cb = new CriteriaBuilder(genericTypeClass);
                    String joinTable = ((ManyToMany) field.getAnnotation(ManyToMany.class)).joinTable();
                    String joinColumn = ((ManyToMany) field.getAnnotation(ManyToMany.class)).joinColumn();
                    String inverseJoinColumn = ((ManyToMany) field.getAnnotation(ManyToMany.class)).inverseJoinColumn();

                    fieldHoldingId.setAccessible(true);
                    cb.add(Restrictions.eq(joinTable, joinColumn,
                            fieldHoldingId.get(correspBetweenTableAndInfo.get(tableName).getInstance())),
                            LogicalOperator.NONE);

                    directJoinSql = "JOIN " + joinTable + " ON " + joinTable + "." + inverseJoinColumn + " = "
                            + getClassTableName(genericTypeClass) + "." + getFieldNameInDatabase(mnyToManyFieldId);
                    List<?> list = select(connection, genericTypeClass, cb, directJoinSql, instanceClass);
                    field.setAccessible(true);
                    field.set(correspBetweenTableAndInfo.get(tableName).getInstance(), list);
                }

                result.add(correspBetweenTableAndInfo.get(tableName).getInstance());
            }
        } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchFieldException
                | SecurityException | SQLException e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (ps != null) {
                ps.close();
            }
        }

        return result;
    }

    private Field getFieldInsideClass(Class<?> clazz, String searchName) {
        String currentFieldName;
        for (Field f : getAllFields(clazz)) {
            currentFieldName = f.isAnnotationPresent(Column.class) ? f.getAnnotation(Column.class).name() : f.getName();
            if (searchName.equals(currentFieldName)) {
                return f;
            }
        }
        throw new RuntimeException("L'attribut " + searchName + " n'a pas été trouvé dans la classe" + clazz.getName());
    }

    private Map<String, Set<String>> getTablesInvolvedAndItsFields(ResultSetMetaData rsmd) throws SQLException {
        Set<String> tablesInvolved = new HashSet<String>();
        for (int i = 1; i <= rsmd.getColumnCount(); i++) {
            tablesInvolved.add(rsmd.getColumnLabel(i).split("[.]")[0]);
        }
        Map<String, Set<String>> tableAndFieldNames = new HashMap<String, Set<String>>();
        tablesInvolved.forEach((s) -> {
            tableAndFieldNames.put(s, new HashSet<String>());
        });

        for (int i = 1; i <= rsmd.getColumnCount(); i++) {
            if (rsmd.getColumnLabel(i).equalsIgnoreCase("RN")) {
                continue;
            }
            tableAndFieldNames.get(rsmd.getColumnLabel(i).split("[.]")[0]).add(rsmd.getColumnLabel(i).split("[.]")[1]);
        }
        return tableAndFieldNames;
    }

    public List<?> select() throws InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchFieldException, SQLException {
        Class<?> classe = criteriaBuilder.getClazz();
        Connection c = getConnection();
        try {
            return select(c, classe, this.criteriaBuilder, null, null);
        } catch (IllegalArgumentException e) {
            throw e;
        } finally {
            this.criteriaBuilder = null;
            c.close();
        }
    }

    private static List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<Field>();
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            fields.addAll(Arrays.asList(c.getDeclaredFields()));
        }
        return fields;
    }

    private String getFieldSqlManyToOne(Class<?> clazz, String sql) {
        String tableName = getClassTableName(clazz);
        for (Field field : getAllFields(clazz)) {
            if (!field.isAnnotationPresent(Transient.class) && !field.isAnnotationPresent(OneToMany.class)
                    && !field.isAnnotationPresent(ManyToOne.class) && !field.isAnnotationPresent(ManyToMany.class)) {
                String fieldName = getFieldNameInDatabase(field);
                sql += tableName + "." + fieldName + " as " + "\"" + tableName + "." + field.getName() + "\",";
            }
            if (field.isAnnotationPresent(ManyToOne.class)) {
                sql = getFieldSqlManyToOne(field.getType(), sql);
            }
        }
        return sql;
    }

    private StringBuilder getLeftJoinSql(Class<?> clazz, StringBuilder sql, Map<String, InfoForLeftJoin> info)
            throws InstantiationException, IllegalAccessException {
        String manyToOneTableName;
        String tableName = getClassTableName(clazz);
        List<Field> allFields = getAllFields(clazz);

        for (Field field : allFields) {
            if (field.isAnnotationPresent(ManyToOne.class)) {
                InfoForLeftJoin temp = new InfoForLeftJoin(field.getType(), null, null, field);
                manyToOneTableName = getClassTableName(field.getType());
                info.put(manyToOneTableName, temp);
                if (!field.isAnnotationPresent(JoinColumn.class)) {
                    throw new RuntimeException("Vous avez oublié d'annoter l'attribut " + field.getName()
                            + " de la classe " + clazz + " avec @JoinColumn");
                }
                String fieldName = getFieldNameInDatabase(field);
                sql.append(" LEFT JOIN ").append(manyToOneTableName).append(" ON ").append(tableName).append(".")
                        .append(fieldName).append(" = ").append(manyToOneTableName).append(".");
                for (Field subField : getAllFields(field.getType())) {
                    if (subField.isAnnotationPresent(Id.class)) {
                        String subFieldName = getFieldNameInDatabase(subField);
                        sql.append(subFieldName);
                    }
                    if (subField.isAnnotationPresent(JoinColumn.class)
                            || subField.isAnnotationPresent(ManyToOne.class)) {
                        getLeftJoinSql(field.getType(), sql, info);
                    }
                }
            }
        }
        return sql;
    }
}
