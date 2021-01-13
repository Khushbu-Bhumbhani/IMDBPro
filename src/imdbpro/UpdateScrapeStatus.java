/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imdbpro;

import connectionManager.MyConnection;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Khushbu
 */
public class UpdateScrapeStatus {

    public static void main(String[] args) {
        updateStatus();
    }

    private static void updateStatus() {
        try {
            List<String> lines = Files.readAllLines(Paths.get("E:\\imdbsept3.txt"));
            lines.forEach(line -> {
                String updateQuery = "update email_master set is_delivered=1 where email='" + line + "';";
                MyConnection.getConnection("imdbpro");
                MyConnection.insertData(updateQuery);
                System.out.println("Updated!");
            });
        } catch (IOException ex) {
            Logger.getLogger(UpdateScrapeStatus.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
