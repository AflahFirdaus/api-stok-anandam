import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestData {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://192.168.1.176:5432/stok-anandam";
        String user = "anandamstok";
        String password = "Letmein99+";
        try {
            Connection conn = DriverManager.getConnection(url, user, password);
            Statement stmt = conn.createStatement();

            // 1. Cek jumlah data
            ResultSet rs1 = stmt.executeQuery("SELECT COUNT(*) as total FROM public.ijin_import");
            if (rs1.next()) {
                System.out.println("JUMLAH DATA di ijin_import: " + rs1.getInt("total"));
            }

            // 2. Ambil sample data
            ResultSet rs2 = stmt.executeQuery("SELECT * FROM public.ijin_import LIMIT 3");
            System.out.println("SAMPLE DATA:");
            while (rs2.next()) {
                System.out.println("  no=" + rs2.getInt("no") + ", nama_barang=" + rs2.getString("nama_barang") + ", keterangan=" + rs2.getString("keterangan"));
            }

            // 3. Cek schema - pastikan tabel ada di schema 'public'
            ResultSet rs3 = stmt.executeQuery(
                "SELECT table_schema, table_name FROM information_schema.tables WHERE table_name = 'ijin_import'"
            );
            System.out.println("TABLE SCHEMA INFO:");
            while (rs3.next()) {
                System.out.println("  schema=" + rs3.getString("table_schema") + ", table=" + rs3.getString("table_name"));
            }

            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
