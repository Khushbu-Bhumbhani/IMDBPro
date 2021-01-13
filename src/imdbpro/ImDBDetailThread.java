/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imdbpro;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import connectionManager.MyConnection;
import connectionManager.Utility;
import static imdbpro.IMDBPro.current_thread_count;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 *
 * @author Khushbu
 */
public class ImDBDetailThread implements Runnable {
    
    WebClient webClient;
    int id;
    String detailURL;
    
    public ImDBDetailThread(WebClient webClient, int id, String detailURL) {
        this.webClient = webClient;
        this.id = id;
        this.detailURL = detailURL;
        current_thread_count++;
    }
    
    @Override
    public void run() {
        scrapeDetail(webClient,id,detailURL);
        current_thread_count--;
    }
    
    synchronized void scrapeDetail(WebClient webClient, int id, String detailURL) {
        
        try {
            System.out.println("" + detailURL);
            HtmlPage page = webClient.getPage(detailURL);
           // System.out.println("" + page.asText());
            if (page.asText().contains("Access blocked")) {
                System.out.println("ACCESS BLOCKED");
                IMDBPro.logout(webClient);
                System.exit(0);
            }
            //    webClient.waitForBackgroundJavaScript(1 * 1000);
            Document doc = Utility.getWebDocument(page);
            String name = "";
            String profession = "";
            if (doc.getElementById("name_heading") != null && !doc.getElementById("name_heading").getElementsByTag("h1").isEmpty()) {
                name = doc.getElementById("name_heading").getElementsByTag("h1").first().text();
                profession = doc.getElementById("name_heading").getElementsByClass("a-row").last().text();
            }
            Element div = doc.getElementById("contacts");
            if (div != null) {
                for (Element e : div.getElementsByClass("header_section")) {
                    String header = e.getElementsByClass("contacts_header").first().text().trim();
                  //  System.out.println("-" + header + " - ");
                    if (header.equalsIgnoreCase("Company") || header.equalsIgnoreCase("Direct Contact")) {
                        System.out.println("Header:" + header);
                        
                        if (e.html().contains("mailto:")) {
                            // String[] emails = StringUtils.substringsBetween(e.html(), "mailto:", "\">");
                            String emails = StringUtils.substringBetween(e.html(), "mailto:", "\">");
                            // for (String m : emails) {
                            if (emails != null) {
                                System.out.println("-------->;" + name + ";" + emails + ";" + detailURL + ";<-------------");
                                String insertQ = "INSERT INTO `imdbpro`.`email_master`\n"
                                        + "(\n"
                                        + "`user_name`,\n"
                                        + "`email`,\n"
                                        + "`link_id`)\n"
                                        + "VALUES\n"
                                        + "("
                                        + "'" + Utility.prepareString(name) + "',"
                                        //+ "'" + m + "',"
                                        + "'" + emails + "',"
                                        + id
                                        + ")";
                                MyConnection.getConnection("imdbpro");
                                //   synchronized (this) {
                                MyConnection.insertData(insertQ);
                                // }
                                System.out.println("Inserted!");
                                // }
                            }
                        }
                    }
                }
                //get all contact html in db
                String IQ = "INSERT INTO `imdbpro`.`contact_master`\n"
                        + "(\n"
                        + "`user_name`,\n"
                        + "`link_id`,\n"
                        + "`contact_html`,\n"
                        + "`user_profession`,"
                        + "link"
                        + ")\n"
                        + "VALUES\n"
                        + "("
                        + "'" + Utility.prepareString(name) + "',"
                        + id + ","
                        + "'" + Utility.prepareString(div.html()) + "',"
                        + "'" + Utility.prepareString(profession) + "',"
                        + "'" + detailURL + "'"
                        + ");";
                MyConnection.getConnection("imdbpro");
                //synchronized (this) {
                MyConnection.insertData(IQ);
                //  }
            }
            updateId(id);
            
        } catch (IOException ex) {
            Logger.getLogger(IMDBPro.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FailingHttpStatusCodeException ex) {
            Logger.getLogger(IMDBPro.class.getName()).log(Level.SEVERE, null, ex);
            
        }
        // System.gc();
    }
    
    private static void updateId(int id) {
        String updateQ = "update link_master set is_scraped=1 where link_id=" + id;
        MyConnection.getConnection("imdbpro");
        MyConnection.insertData(updateQ);
        
    }
}
