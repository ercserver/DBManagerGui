package sample.db;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/*

/**
 * Created by ohad on 16/7/2015.
 */
public class DbModel {

    final String JDBC_DRIVER = "net.sourceforge.jtds.jdbc.Driver";
    public final static String DB_URL = "jdbc:jtds:sqlserver://socmedserver.mssql.somee.com;";
    public final static String DB_NAME = "socmedserver";
    private static Connection connection = null;
    public final static String DB_USERNAME = "saaccount";
    public final static String DB_PASS = "saaccount";

    Logger logger = Logger.getLogger(this.getClass().getName());

    private void connect() throws SQLException {
        try {
            Class.forName(JDBC_DRIVER);
            connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASS);
            //connection.setAutoCommit(true);

            Statement statement = connection.createStatement();

            statement.execute("USE " + DB_NAME);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    /*For all of the methods:in each HashMap if the value represent value of column with type varchar,
      the value need to be in this format: 'value' */


    public HashMap<Integer, HashMap<String, String>>
    getRowsFromTable(HashMap<String, String> whereConditions, String tableName) {
        logger.log(Level.INFO, "   In getRowsFromTable");
        String conditions = "";

        if (whereConditions == null) {
            // No conditions - get all the rows from the table
            conditions = "1=1";
            whereConditions = new HashMap<String, String>();
        } else {
            int numOfConditions = whereConditions.size();
            Set<String> keys = whereConditions.keySet();
            Iterator<String> iter = keys.iterator();
            // creates the where condition for sql query
            String key = iter.next();
            conditions = key + "=?";
            for (int i = 1; i < numOfConditions; i++) {
                key = iter.next();
                conditions += " AND " + key + "=?";
            }

        }
        ResultSet rs = null;
        try {
            // Create the query
            if (!(connection != null && !connection.isClosed() /*&& connection.isValid*/))
                connect();
            String query = "SELECT * FROM " + tableName + " WHERE " + conditions;
            // Assign the values to the where clause
            PreparedStatement stmt = connection.prepareStatement(query);
            Set<String> keys = whereConditions.keySet();
            int parameterIndex = 1;
            for (String key : keys) {
                stmt.setObject(parameterIndex, whereConditions.get(key));
                parameterIndex++;
            }

            rs = stmt.executeQuery();
            HashMap<Integer, HashMap<String, String>> hash = resultSetToMap(rs);

                       logger.log(Level.INFO, "   exiting getRowsFromTable");
            return hash;

        }
        // There was a fault with the connection to the server or with SQL
        catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<String> getTablesNames(){
        List<String> names = new ArrayList<>();
        try {
            if (connection == null || connection.isClosed()){
                connect();
            }
            ResultSet rs = connection.createStatement().executeQuery("select name from sys.tables order by name asc");
            while (rs.next()){
                names.add(rs.getString("name"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return names;
    }

    public int addRow(String tableName, HashMap<String, String> values){
        int affectedRows = 0;
        try{
            if (connection == null || connection.isClosed()){
                connect();
            }
            StringBuilder sql = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
            StringBuilder placeholders = new StringBuilder();

            for (Iterator<String> iter = values.keySet().iterator(); iter.hasNext();) {
                String cur = iter.next();
                if (values.get(cur) == null){
                    continue;
                }
                sql.append(cur);
                placeholders.append("?");

                if (iter.hasNext()) {
                    sql.append(",");
                    placeholders.append(",");
                }
            }

            sql.append(") VALUES (").append(placeholders).append(")");
            Statement stmt1 = connection.createStatement();
            ResultSet rs = stmt1.executeQuery("SELECT ident_current('" + tableName + "')");
            if (rs.next() && rs.getInt(1) != 0){
                System.out.println(rs.getInt(1));
                stmt1.close();
                stmt1 = connection.createStatement();
                stmt1.execute("SET identity_insert " + tableName + " ON");
                stmt1.close();
            }

            PreparedStatement preparedStatement = connection.prepareStatement(sql.toString());

            int i = 1;

            for (String value : values.values()) {
                preparedStatement.setObject(i, value);
                i++;
            }


            affectedRows = preparedStatement.executeUpdate();

            stmt1 = connection.createStatement();
            stmt1.execute("SET IDENTITY_INSERT " + tableName + " OFF");
            stmt1.close();


        }catch (SQLException ex){
            ex.printStackTrace();
        }
        return affectedRows;
    }

    private HashMap<Integer, HashMap<String, String>> resultSetToMap(ResultSet rs) {
        HashMap<Integer, HashMap<String, String>> map = new HashMap<Integer, HashMap<String, String>>();
        try {
            int j = 1;
            while (rs.next()) {
                int total_rows = rs.getMetaData().getColumnCount();
                HashMap<String, String> obj = new HashMap<String, String>();
                for (int i = 1; i <= total_rows; i++) {
                    // Add pair (column, value) if value is not null
                    String column = rs.getMetaData().getColumnLabel(i);
                    Object val = rs.getObject(i);
                    if (val != null) {
                        obj.put(column, val.toString());
                    } else {
                        obj.put(column, "null");
                    }
                }
                map.put(j, obj);
                j++;
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.log(Level.INFO, "exception in resultSetToMap");
        }
        return map;
    }


    public HashMap<Integer, HashMap<String, String>> getColumnsNames(String tableName) {
        // Get the columns names
        String query = "SELECT COLUMN_NAME from INFORMATION_SCHEMA.COLUMNS where TABLE_NAME=?";
        PreparedStatement stmt = null;
        try {
            if (connection == null || connection.isClosed()){
                connect();
            }
            stmt = connection.prepareStatement(query);
            stmt.setObject(1, tableName);
            ResultSet rs = stmt.executeQuery();

            HashMap<Integer, HashMap<String, String>> big = new HashMap<>();
            HashMap<String, String> map = new HashMap<>();
            while (rs.next()){
                map.put(rs.getString("COLUMN_NAME"), null);
            }
            big.put(1, map);
            return big;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void deleteRow(String tableName, HashMap<String, String> conds){
        StringBuilder sql = new StringBuilder("DELETE FROM ").append(tableName).append(" WHERE ");

        for (Iterator<String> iter = conds.keySet().iterator(); iter.hasNext();) {
            // <column>=? AND ...
            sql.append(iter.next()).append("=").append("?");

            if (iter.hasNext()) {
                sql.append(" AND ");
            }
        }

        try {
            if (connection == null || connection.isClosed()){
                connect();
            }
            PreparedStatement stmt = connection.prepareStatement(sql.toString());
            int i = 1;
            for (String key : conds.keySet()){
                stmt.setObject(i, conds.get(key));
                i++;
            }
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
