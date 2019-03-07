package pack;

import java.sql.*;

public class dbo {
    private static final String url = "jdbc:postgresql://localhost:5432/dashboard";
    private static final String user = "postgres";
    private static final String password = "123456";

    public static Connection connect() throws SQLException, ClassNotFoundException {
        Class.forName("org.postgresql.Driver");
        return DriverManager.getConnection(url, user, password);
    }

    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        int build = 111;
        String testId = "bbb";
        String machineName = "ccc";
        String packageName = "ddd";
        String testName = "eee";
        int duration = 222;
        String project = "Automation_Ct_Cloud";
        String SQL = "INSERT INTO " + project + " (build,testId,machineName,packageName,testName, duration)" +
                "VALUES (" + build + ",'" + testId + "','" + machineName + "','" + packageName + "','" + testName + "'," + duration + ");";
        System.out.println(SQL);

    }

    static boolean execute(String SQL) {
        long id = 0;

        try {
            Connection conn = connect();
            PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        id = rs.getLong(1);
                    }
                } catch (SQLException ex) {
                    System.out.println(ex.getMessage());
                }
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return id > 0;
    }

    static int executeAndGet(String SQL) {
        int id = 0;

        try {
            Connection conn = connect();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(SQL);

            if (rs != null) {
                if (rs.next()) {
                    id = rs.getInt(1);
                }
            }

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return id;
    }

    static void initDB() {
        Projects p[] = Projects.values();
        for (int i = 0; i < p.length; i++) {
            String SQL = "CREATE TABLE " + p[i] + "(" +
                    "build int," +
                    "testId varchar(255)," +
                    "machineName varchar(255)," +
                    "packageName varchar(255)," +
                    "testName varchar(255)," +
                    "duration int);";
            execute(SQL);
        }

    }
}
