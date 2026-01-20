import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordHashGenerator {
    
    public static String hashPassword(String pwd) {
        if (pwd == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(pwd.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            return pwd;
        }
    }
    
    public static void main(String[] args) {
        // Generate hashes for default passwords
        String[] passwords = {"customer", "admin", "staff"};
        
        System.out.println("Password hashes (SHA-256):");
        System.out.println("===========================");
        
        for (String pwd : passwords) {
            String hash = hashPassword(pwd);
            System.out.println(pwd + " -> " + hash);
        }
        
        // Also show what the current hashes hash to
        System.out.println("\n\nCurrent hashes in SQL:");
        System.out.println("======================");
        System.out.println("'customer','b041c0aeb35bb0fa4aa668ca5a920b590196fdaf9a00eb852c9b7f4d123cc6d6'");
        System.out.println("'admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9'");
        System.out.println("'staff', '10176e7b7b24d317acfcf8d2064cfd2f24e154f7b5a96603077d5ef813d6a6b6'");
    }
}
