/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imdbpro;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlEmailInput;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import connectionManager.MyConnection;
import connectionManager.Utility;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 *
 * @author Khushbu
 */
public class IMDBPro {

    /**
     * @param args the command line arguments
     */
    static int current_thread_count = 0;
    static final int MAX_THREAD_COUNT = 5;

    public static void main(String[] args) {
        // TODO code application logic here
        logintoProAccount();

    }

    private static void logintoProAccount() {
        String loginScreen = "https://secure.imdb.com/ap/signin?openid.pape.max_auth_age=0&openid.return_to=https%3A%2F%2Fwww.imdb.com%2Fap-signin-handler&openid.identity=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0%2Fidentifier_select&openid.assoc_handle=imdb_pro_us&openid.mode=checkid_setup&siteState=eyJvcGVuaWQuYXNzb2NfaGFuZGxlIjoiaW1kYl9wcm9fdXMiLCJyZWRpcmVjdFRvIjoiaHR0cHM6Ly9wcm8uaW1kYi5jb20vIn0&openid.claimed_id=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0%2Fidentifier_select&openid.ns=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0";
        final WebClient webClient = new WebClient(BrowserVersion.CHROME);
        //   webClient.waitForBackgroundJavaScript(1000);

        webClient.getOptions()
                .setJavaScriptEnabled(false);
        webClient.setAjaxController(
                new NicelyResynchronizingAjaxController());
        webClient.getOptions()
                .setCssEnabled(false);
        webClient.getCookieManager()
                .setCookiesEnabled(true);
        webClient.getOptions()
                .setRedirectEnabled(true);
        webClient.getOptions()
                .setTimeout(0);
        webClient.getOptions()
                .setThrowExceptionOnScriptError(false);
        webClient.getOptions()
                .setThrowExceptionOnFailingStatusCode(false);
        logout(webClient);
        webClient.waitForBackgroundJavaScript(5 * 1000);
        /*  System.exit(0);*/
        //Login
        HtmlPage page = null;

        try {
            System.out.println("login...");
            page = webClient.getPage("https://pro.imdb.com/login/ap");
            HtmlAnchor parentNode = (HtmlAnchor) page.getElementById("login_with_imdb").getParentNode();
            page = parentNode.click();
            webClient.waitForBackgroundJavaScript(5 * 1000);
            //  System.out.println(""+page.asText());
            HtmlForm loginForm = page.getFormByName("signIn");
            HtmlEmailInput email = loginForm.getInputByName("email");
            HtmlPasswordInput pwd = loginForm.getInputByName("password");
            email.setAttribute("value", "gabrielesalomoni@gmail.com");
            pwd.setAttribute("value", "Ceccherini1.");
            HtmlSubmitInput signIn = (HtmlSubmitInput) page.getElementById("signInSubmit");
            HtmlPage resultPage = signIn.click();
            webClient.waitForBackgroundJavaScript(5 * 1000);
            //   System.out.println("" + resultPage.asText());
            //  webClient.waitForBackgroundJavaScript(10 * 1000);
            if (resultPage.asText().contains("Log out")) {
                System.out.println("Login Successful!");
                /**
                 * Scrapes links from imdbpro account
                 */
                crawlLinks(webClient);

                /**
                 * detailscrape crawler
                 */
                //detailScrape(webClient); -- not using now
                //          detailScrapeCrawler(webClient);
                logout(webClient);
            } else {
                System.out.println("page txt:" + page.asText());
                System.out.println("Unable to login!");
            }
        } catch (IOException ex) {
            Logger.getLogger(IMDBPro.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FailingHttpStatusCodeException ex) {
            Logger.getLogger(IMDBPro.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void logout(WebClient webClient) {
        String url = "https://pro.imdb.com/logout?ref_=search_nv_usr_logout";
        //   String url = "https://pro.imdb.com/name/nm0001806/?ref_=search_name_T1_result_1,456";
        try {
            HtmlPage page = webClient.getPage(url);
            //     System.out.println(""+page.asText());
            System.out.println("---------------------Logged OUt----------------------------------------");
            /// System.out.println("" + page.asText());
        } catch (IOException ex) {
            Logger.getLogger(IMDBPro.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FailingHttpStatusCodeException ex) {
            Logger.getLogger(IMDBPro.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Log out");

    }

    private static void scrapeLinks(WebClient webClient, String url) {
        // String url = "https://pro.imdb.com/people/directors?ref_=hm_nv_ppl_dir#profession=director%2Cproducer%2Ccasting_director%2Cexecutive&age=35-59%2C60-%2Cunknown&sort=ranking&pos=0&page=1";
        // String url = "https://pro.imdb.com/people/directors?ref_=hm_nv_ppl_dir#profession=director&starMeter=100000-&age=35-42&sort=ranking";

        int offset = 0;
        boolean isFirstPage = true;
        // url = "https://pro.imdb.com/people/directors/_paginated?offset=" + offset + "&count=25&q=&ref=undefined&keyspace=NAME&profession=director,producer,casting_director,executive&age=35-59,60-,unknown&sort=ranking&pos=0&page=1";
        // int offset = 50;
        do {
            try {
                System.out.println("Getting.." + url);
                System.out.println("Offset:" + offset);
                HtmlPage page = webClient.getPage(url);
                webClient.waitForBackgroundJavaScript(1 * 1000);
                Document doc = Utility.getWebDocument(page);
                if (doc.text().contains("One or more query parameters was invalid")) {
                    System.out.println("Invalid page exiting..");
                    break;
                }
                if (isFirstPage) {
                    //call new url
                    String str = "/_paginated?offset=" + offset + "&count=25&q=&ref=undefined&keyspace=NAME&";

                    url = url.replace("?ref_=hm_nv_ppl_dir#", str);
                    url = url.replace("?ref_=hm_nv_ppl_tsm#", str);
                    offset = offset + 25;
                    System.out.println("Getting First URL.." + url);
                    page = webClient.getPage(url);
                    doc = Utility.getWebDocument(page);
                    isFirstPage = false;
                    String insertQ = "INSERT INTO `imdbpro`.`link_master`\n"
                            + "(\n"
                            + "`link`,\n"
                            + "`page_url`)\n"
                            + "VALUES ";
                    Element div = doc.getElementById("results");
                    if (div == null) {
                        System.out.println("" + doc.html());
                    }
                    for (Element e : div.getElementsByTag("li")) {

                        if (!e.getElementsByTag("a").isEmpty() && !e.hasAttr("class")) {
                            String detailURL = e.getElementsByTag("a").first().attr("href");
                            //    String detailURL = e.attr("href");
                            System.out.println(detailURL);
                            // scrapeDetail(detailURL, webClient);
                            insertQ = insertQ + " ('" + detailURL + "','" + url + "'),";
                        }
                    }
                    insertQ = insertQ.substring(0, insertQ.length() - 1);
                    MyConnection.getConnection("imdbpro");
                    MyConnection.insertData(insertQ);
                    System.out.println("Inserted!");

                } else {

                    //  Element div = doc.getElementById("results");
                    // for (Element e : div.getElementsByTag("li")) {\
                    String insertQ = "INSERT INTO `imdbpro`.`link_master`\n"
                            + "(\n"
                            + "`link`,\n"
                            + "`page_url`)\n"
                            + "VALUES ";
                    for (Element e : doc.getElementsByTag("li")) {
                        if (!e.getElementsByTag("a").isEmpty() && !e.hasAttr("class")) {
                            String detailURL = e.getElementsByTag("a").first().attr("href");
                            //    String detailURL = e.attr("href");
                            System.out.println(detailURL);
                            // scrapeDetail(detailURL, webClient);
                            insertQ = insertQ + " ('" + detailURL + "','" + url + "'),";
                        }
                    }
                    insertQ = insertQ.substring(0, insertQ.length() - 1);
                    MyConnection.getConnection("imdbpro");
                    MyConnection.insertData(insertQ);
                    System.out.println("Inserted!");
                }
                // url = "https://pro.imdb.com/people/directors/_paginated?offset="+offset+"&count=25&q=&ref=undefined&keyspace=NAME&profession=director&starMeter=100000-&age=35-42&sort=ranking";
                String str = "/_paginated?offset=" + offset + "&count=25&q=&ref=undefined&keyspace=NAME&";
                if (url.contains("?ref_=hm_nv_ppl_dir#")) {
                    url = url.replace("?ref_=hm_nv_ppl_dir#", str);
                } else if (url.contains("?ref_=hm_nv_ppl_tsm#")) {
                    url = url.replace("?ref_=hm_nv_ppl_tsm#", str);
                } else {
                    url = url.replace("offset=" + (offset - 25), "offset=" + offset);
                }
                offset = offset + 25;

                if (offset > 10000) {
                    break;
                }

            } catch (IOException ex) {
                Logger.getLogger(IMDBPro.class.getName()).log(Level.SEVERE, null, ex);
            } catch (FailingHttpStatusCodeException ex) {
                Logger.getLogger(IMDBPro.class.getName()).log(Level.SEVERE, null, ex);
            }
        } while (true);
    }

    static void scrapeDetail(WebClient webClient, int id, String detailURL) {

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
            //  String profession = "";
            if (doc.getElementById("name_heading") != null && !doc.getElementById("name_heading").getElementsByTag("h1").isEmpty()) {
                name = doc.getElementById("name_heading").getElementsByTag("h1").first().text();
                //  profession = doc.getElementById("name_heading").getElementsByClass("a-row").last().text();
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
                /*   String IQ = "INSERT INTO `imdbpro`.`contact_master`\n"
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
                MyConnection.insertData(IQ);*/
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

    private static void detailScrapeCrawler(WebClient webClient) {
        String selectQ = "SELECT link_id,link FROM imdbpro.link_master where is_scraped=0 group by ref_url limit 0,10000";
        MyConnection.getConnection("imdbpro");
        ResultSet rs = MyConnection.getResultSet(selectQ);
        try {
            while (rs.next()) {
                String link = rs.getString("link");
                int id = rs.getInt("link_id");
                scrapeDetail(webClient, id, link);
                /*  ImDBDetailThread dt = new ImDBDetailThread(webClient, id, link);
                //ImDBDetailThread dt = new ImDBDetailThread(webClient, 1, "https://pro.imdb.com/name/nm0000799/?ref_=search_name_T1_result_1,761");
                Thread th = new Thread(dt);
                th.start();
                while (current_thread_count > MAX_THREAD_COUNT) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(IMDBPro.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }*/
            }
        } catch (SQLException ex) {
            Logger.getLogger(IMDBPro.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void crawlLinks(WebClient webClient) {
        String urlList[] = {
            "https://pro.imdb.com/people?ref_=hm_nv_ppl_tsm#profession=actress&age=unknown&starMeter=2090000-2160000&sort=ranking&pos=0",
            "https://pro.imdb.com/people?ref_=hm_nv_ppl_tsm#profession=actress&age=unknown&starMeter=2160000-2230000&sort=ranking",
            "https://pro.imdb.com/people?ref_=hm_nv_ppl_tsm#profession=actress&age=unknown&starMeter=2230000-2300000&sort=ranking&pos=0",
            "https://pro.imdb.com/people?ref_=hm_nv_ppl_tsm#profession=actress&age=unknown&starMeter=2300000-2380000&sort=ranking&pos=0",
            "https://pro.imdb.com/people?ref_=hm_nv_ppl_tsm#profession=actress&age=unknown&starMeter=2380000-2450000&sort=ranking&pos=0",
            "https://pro.imdb.com/people?ref_=hm_nv_ppl_tsm#profession=actress&age=unknown&starMeter=2450000-2530000&sort=ranking&pos=0",
            "https://pro.imdb.com/people?ref_=hm_nv_ppl_tsm#profession=actress&age=unknown&starMeter=2530000-2600000&sort=ranking&pos=0",
            "https://pro.imdb.com/people?ref_=hm_nv_ppl_tsm#profession=actress&age=unknown&starMeter=2600000-2670000&sort=ranking&pos=0",
            "https://pro.imdb.com/people?ref_=hm_nv_ppl_tsm#profession=actress&age=unknown&starMeter=2670000-2740000&sort=ranking&pos=0",
            "https://pro.imdb.com/people?ref_=hm_nv_ppl_tsm#profession=actress&age=unknown&starMeter=2740000-2810000&sort=ranking&pos=0",
            "https://pro.imdb.com/people?ref_=hm_nv_ppl_tsm#profession=actress&age=unknown&starMeter=2810000-2880000&sort=ranking&pos=0",};
        for (String url : urlList) {
            scrapeLinks(webClient, url);
        }
    }

}
