import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestInfoSchema {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://192.168.1.176:5432/stok-anandam";
        String user = "anandamstok";
        String password = "Letmein99+";
        try {
            Connection conn = DriverManager.getConnection(url, user, password);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'ijin_import'");
            System.out.println("COLUMNS IN information_schema FOR ijin_import:");
            while (rs.next()) {
                System.out.println("- " + rs.getString("column_name") + " (" + rs.getString("data_type") + ")");
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
