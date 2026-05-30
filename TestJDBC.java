import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.ResultSetMetaData;

public class TestJDBC {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://192.168.1.176:5432/stok-anandam";
        String user = "anandamstok";
        String password = "Letmein99+";
        try {
            Connection conn = DriverManager.getConnection(url, user, password);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM public.ijin_import LIMIT 1");
            ResultSetMetaData rsmd = rs.getMetaData();
            System.out.println("COLUMN NAMES IN ijin_import:");
            for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                System.out.println("- '" + rsmd.getColumnName(i) + "'");
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
