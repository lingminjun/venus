package com.venus.apigw.db;

import com.venus.esb.lang.ESBT;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: lingminjun
 * Date: 2018-10-03
 * Time: 上午10:14
 */
public final class DB {

    private static final Logger logger           = LoggerFactory.getLogger(DB.class);

    private DataSource datasource;

    public DB(String url, String user, String pswd) {
        this(url,user,pswd,"com.mysql.jdbc.Driver");
    }

    public DB(String url, String user, String pswd, String dirver) {

        PoolProperties p = new PoolProperties();

        p.setUrl(url);
        if (dirver == null) {
            dirver = "com.mysql.jdbc.Driver";
        }
        p.setDriverClassName(dirver);
        try {
            Class.forName(dirver);//提前加载下驱动
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        p.setUsername(user);
        p.setPassword(pswd);

        p.setJmxEnabled(false);
        p.setTestWhileIdle(true);
        p.setTestOnBorrow(true);
        p.setTestOnReturn(false);
        p.setValidationQuery("select 1");
        p.setValidationInterval(30000);
        p.setTimeBetweenEvictionRunsMillis(30000);
        p.setMinEvictableIdleTimeMillis(30000);
        p.setMaxActive(400);
        p.setInitialSize(10);
        p.setMaxWait(10000);
        p.setMinIdle(10);
        p.setMaxIdle(100);
        p.setLogAbandoned(false);
        p.setRemoveAbandoned(true);
        p.setRemoveAbandonedTimeout(60);
        p.setJdbcInterceptors("org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer");
        datasource = new DataSource();
        datasource.setPoolProperties(p);

    }

//    public static DB pool() {
//        return DBPoolSingletonHolder.INSTANCE;
//    }
//
//    private static class DBPoolSingletonHolder {
//        private static DB INSTANCE = new DB();
//    }

    public final Connection getConnection() {
        try {
            return this.datasource.getConnection();
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public final void close(Connection conn, ResultSet rs, Statement st) {
        SQLException dbException = null;
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            logger.error("result set close failed.", e);
            dbException = e;
        }
        try {
            if (st != null) {
                st.close();
            }
        } catch (SQLException e) {
            logger.error("statement close failed.", e);
            if (dbException != null) {
                dbException = e;
            }
        }
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            logger.error("connection close failed.", e);
            if (dbException != null) {
                dbException = e;
            }
        }

        if (dbException != null) {
            throw new RuntimeException(dbException);
        }
    }

    private static void bind(PreparedStatement statement, Object bind, int index) throws SQLException {
        if (bind.getClass() == Double.class || bind.getClass() == double.class) {
            statement.setDouble(index, (Double) bind);
        } else if (bind.getClass() == Float.class || bind.getClass() == float.class) {
            statement.setFloat(index, (Float) bind);
        } else if (bind.getClass() == Long.class || bind.getClass() == long.class) {
            statement.setLong(index, (Long) bind);
        } else if (bind.getClass() == Integer.class || bind.getClass() == int.class) {
            statement.setInt(index, (Integer) bind);
        } else if (bind.getClass() == Short.class || bind.getClass() == short.class) {
            statement.setShort(index, (Short) bind);
        } else if (bind.getClass() == Byte.class || bind.getClass() == byte.class) {
            statement.setByte(index, (Byte) bind);
        } else if (bind.getClass() == Boolean.class || bind.getClass() == boolean.class) {
            statement.setBoolean(index, (Boolean) bind);
        } else if (bind instanceof String) {
            statement.setString(index, (String) bind);
        } else if (bind instanceof BigDecimal) {
            statement.setBigDecimal(index, (BigDecimal) bind);
        } else if (bind.getClass() == Double.class || bind.getClass() == double.class) {
            statement.setDouble(index, (Double) bind);
        } else if (bind instanceof java.util.Date) {
            Timestamp dt = new Timestamp(((java.util.Date)bind).getTime());
            statement.setTimestamp(index, dt);
        } else if (bind instanceof java.sql.Date) {
            statement.setDate(index, (java.sql.Date)bind);
        } else if (bind instanceof Time) {
            statement.setTime(index, (Time)bind);
        } else if (bind instanceof Timestamp) {
            statement.setTimestamp(index, (Timestamp)bind);
        } else {
            throw new RuntimeException("不支持的类型绑定");
        }
    }

    private static <T extends Object> void add(List<T> list, ResultSet result, Class<T> type) throws SQLException, IllegalAccessException {
        T obj = ESBT.createObject(type.getName(),type);
        Field[] fV = type.getDeclaredFields();
        for (Field field : fV) {

            //去掉静态属性
            if ((field.getModifiers() & Modifier.STATIC) == Modifier.STATIC) {
                continue;
            }

            try {
                field.setAccessible(true);
            } catch (Throwable e) {
                continue;
            }

            if (field.getType() == String.class) {
                String value = result.getString(field.getName());
                if (value != null) {
                    field.set(obj, value);
                }
            } else if (field.getType() == Boolean.class || field.getType() == boolean.class) {
                boolean value = result.getBoolean(field.getName());
                if (field.getType() == boolean.class) {
                    field.setBoolean(obj, value);
                } else {
                    field.set(obj, new Boolean(value));
                }
            } else if (field.getType() == Byte.class || field.getType() == byte.class) {
                byte value = result.getByte(field.getName());
                if (field.getType() == byte.class) {
                    field.setByte(obj, value);
                } else {
                    field.set(obj, new Byte(value));
                }
            } else if (field.getType() == Short.class || field.getType() == short.class) {
                short value = result.getShort(field.getName());
                if (field.getType() == short.class) {
                    field.setShort(obj, value);
                } else {
                    field.set(obj, new Short(value));
                }
            } else if (field.getType() == Integer.class || field.getType() == int.class) {
                int value = result.getInt(field.getName());
                if (field.getType() == int.class) {
                    field.setInt(obj, value);
                } else {
                    field.set(obj, new Integer(value));
                }
            } else if (field.getType() == Long.class || field.getType() == long.class) {
                long value = result.getLong(field.getName());
                if (field.getType() == long.class) {
                    field.setLong(obj, value);
                } else {
                    field.set(obj, new Long(value));
                }
            } else if (field.getType() == Float.class || field.getType() == float.class) {
                float value = result.getFloat(field.getName());
                if (field.getType() == float.class) {
                    field.setFloat(obj, value);
                } else {
                    field.set(obj, new Float(value));
                }
            } else if (field.getType() == Double.class || field.getType() == double.class) {
                double value = result.getDouble(field.getName());
                if (field.getType() == double.class) {
                    field.setDouble(obj, value);
                } else {
                    field.set(obj, new Double(value));
                }
            } else if (field.getType() == java.util.Date.class) {
                Timestamp value = result.getTimestamp(field.getName());
                if (value != null) {
                    field.set(obj, new java.util.Date(value.getTime()));
                }
            } else if (field.getType() == java.sql.Date.class) {
                java.sql.Date value = result.getDate(field.getName());
                if (value != null) {
                    field.set(obj, value);
                }
            } else if (field.getType() == Time.class) {
                Time value = result.getTime(field.getName());
                if (value != null) {
                    field.set(obj, value);
                }
            } else if (field.getType() == Timestamp.class) {
                Timestamp value = result.getTimestamp(field.getName());
                if (value != null) {
                    field.set(obj, value);
                }
            } else {
                continue;
//                throw new RuntimeException("不支持的类型绑定");
            }
        }

        list.add(obj);
    }

    private static Object[] values(StringBuilder instr,StringBuilder vlstr, StringBuilder upstr, String[] keys, Object object) {

        List<Object> values = new ArrayList<>();
        Class<?> type = object.getClass();
        boolean isFirst = true;
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            Field field = null;
            try {
                field = type.getDeclaredField(key);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }

            //去掉静态属性
            if (field == null || (field.getModifiers() & Modifier.STATIC) == Modifier.STATIC) {
                continue;
            }


            Object value = null;
            try {
                field.setAccessible(true);
                value = field.get(object);
            } catch (Throwable e) {
                continue;
            }

            // ignore key
            if (value == null) {
                continue;
            }

            if (field.getType() == java.util.Date.class) {
                value = new Timestamp(((java.util.Date) value).getTime());
                values.add(value);
            }


            if (field.getType() == String.class
                    || field.getType() == Boolean.class || field.getType() == boolean.class
                    || field.getType() == Byte.class || field.getType() == byte.class
                    || field.getType() == Short.class || field.getType() == short.class
                    || field.getType() == Integer.class || field.getType() == int.class
                    || field.getType() == Long.class || field.getType() == long.class
                    || field.getType() == Float.class || field.getType() == float.class
                    || field.getType() == Double.class || field.getType() == double.class
                    || field.getType() == java.sql.Date.class
                    || field.getType() == Time.class
                    || field.getType() == Timestamp.class

                    || field.getType() == java.util.Date.class // convert
                    ) {
                values.add(value);
                if (isFirst) {
                    isFirst = false;
                } else {
                    instr.append(",");
                    vlstr.append(",");
                    upstr.append(",");
                }

                instr.append("`");
                instr.append(key);
                instr.append("`");

                vlstr.append("?");

                upstr.append("`");
                upstr.append(key);
                upstr.append("` = ?");

            } else {
                continue;
//                throw new RuntimeException("不支持的类型绑定");
            }
        }

        Object[] vs = new Object[values.size() * 2];
        for (int i = 0; i < values.size(); i++) {
            vs[i] = values.get(i);
            vs[i + values.size()] = values.get(i);
        }

        return vs;
    }

    public int upinsert(String table, String[] keys, Object object) {
        if (keys == null || keys.length == 0) {
            return 0;
        }

        StringBuilder instr = new StringBuilder();
        StringBuilder vlstr = new StringBuilder();
        StringBuilder upstr = new StringBuilder();
        Object[] binds = values(instr,vlstr,upstr,keys,object);

        // 数据库连接
        Connection connection = null;

        // 预编译的Statement，使用预编译的Statement提高数据库性能
        PreparedStatement statement = null;

        // 结果集
        ResultSet resultSet = null;

        int row = 0;

        try {
            // 加载数据库驱动
//            Class.forName("com.mysql.jdbc.Driver");

            // 通过驱动管理类获取数据库链接
            connection = getConnection();

            // 定义sql语句 ?表示占位符
            String sql = "insert into `" + table + "`(" + instr + ") values(" + vlstr + ") on duplicate key update " + upstr;

            // 获取预处理statement
            statement = connection.prepareStatement(sql);

            // 设置参数，第一个参数为sql语句中参数的序号（从1开始），第二个参数为设置的参数值
            if (binds != null) {
                for (int i = 1; i <= binds.length; i++) {
                    bind(statement,binds[i - 1],i);
                }
            }

//            statement.setString(1, "王五");
            // 向数据库发出sql执行查询，查询出结果集
            row = statement.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(connection,resultSet,statement);
        }

        return row;
    }

    public <T extends Object> List<T> query(String table, String condition, Object[] binds, Class<T> type) {
        return query(null,table,condition,binds,type);
    }
    public <T extends Object> List<T> query(String table, String condition, Object[] binds, Class<T> type, int limit) {
        return query(null,table,condition,binds,type,limit);
    }
    public <T extends Object> List<T> query(String scheme, String table, String condition, Object[] binds, Class<T> type) {
        return query(scheme,table,condition,binds,type,10000);
    }
    public <T extends Object> List<T> query(String scheme, String table, String condition, Object[] binds, Class<T> type, int limit) {

        // 数据库连接
        Connection connection = null;

        // 预编译的Statement，使用预编译的Statement提高数据库性能
        PreparedStatement statement = null;

        // 结果集
        ResultSet resultSet = null;

        List<T> list = new ArrayList<>();

        try {
            // 加载数据库驱动
//            Class.forName("com.mysql.jdbc.Driver");

            // 通过驱动管理类获取数据库链接
            connection = getConnection();

            // 定义sql语句 ?表示占位符
            String sql = "select * from `" + (scheme != null ? scheme + "`.`" : "") + table + "`";//where `domain` = ? and `module` = ? and `method` = ?";
            if (condition != null && condition.length() > 0) {
                sql = sql + " where " + condition;//"`domain` = ? and `module` = ? and `method` = ?";
            }
            if (limit > 0) {
                sql = sql + " limit " + limit;
            } else {
                sql = sql + " limit 10000";
            }

            // 获取预处理statement
            statement = connection.prepareStatement(sql);

            // 设置参数，第一个参数为sql语句中参数的序号（从1开始），第二个参数为设置的参数值
            if (binds != null) {
                for (int i = 1; i <= binds.length; i++) {
                    bind(statement,binds[i - 1],i);
                }
            }

//            statement.setString(1, "王五");
            // 向数据库发出sql执行查询，查询出结果集
            resultSet = statement.executeQuery();

            // 遍历查询结果集
            while (resultSet.next()) {
                add(list,resultSet,type);
//                System.out.println(resultSet.getString("id") + "  " + resultSet.getString("username"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(connection,resultSet,statement);
        }

        return list;
    }

    public int delete(String table, String condition, Object[] binds) {

        // 数据库连接
        Connection connection = null;

        // 预编译的Statement，使用预编译的Statement提高数据库性能
        PreparedStatement statement = null;

        // 结果集
        int row = 0;


        try {

            // 通过驱动管理类获取数据库链接
            connection = getConnection();

            // 定义sql语句 ?表示占位符
            String sql = "delete from `" + table + "`";//where `domain` = ? and `module` = ? and `method` = ?";
            if (condition != null && condition.length() > 0) {
                sql = sql + " where " + condition;//"`domain` = ? and `module` = ? and `method` = ?";
            }

            // 获取预处理statement
            statement = connection.prepareStatement(sql);

            // 设置参数，第一个参数为sql语句中参数的序号（从1开始），第二个参数为设置的参数值
            if (binds != null) {
                for (int i = 1; i <= binds.length; i++) {
                    bind(statement,binds[i - 1],i);
                }
            }

            // 向数据库发出sql执行查询，查询出结果集
            row = statement.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(connection,null,statement);
        }

        return row;
    }

    public int execute(String sql, Object[] binds) {

        // 数据库连接
        Connection connection = null;

        // 预编译的Statement，使用预编译的Statement提高数据库性能
        PreparedStatement statement = null;

        // 结果集
        int row = 0;

        try {

            // 通过驱动管理类获取数据库链接
            connection = getConnection();

            // 获取预处理statement
            statement = connection.prepareStatement(sql);

            // 设置参数，第一个参数为sql语句中参数的序号（从1开始），第二个参数为设置的参数值
            if (binds != null) {
                for (int i = 1; i <= binds.length; i++) {
                    bind(statement,binds[i - 1],i);
                }
            }

            // 向数据库发出sql执行查询，查询出结果集
            row = statement.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(connection,null,statement);
        }

        return row;
    }
}
