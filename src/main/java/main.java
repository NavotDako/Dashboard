import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class main {
    private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");


    public static void main(String[] args) throws Exception {
        Projects p[] = Projects.values();
        for (int i = 0; i < p.length; i++) {
            int newBuild = getLastBuildId(p[i]);
            int oldBuildId = getLastBuildFromDB(p[i]);
            if (newBuild > oldBuildId) {
                Document doc = getFailures(newBuild);
                saveToDB(doc, p[i]);
            }
        }
    }

    private static int getLastBuildId(Projects project) throws IOException, SAXException, ParserConfigurationException {
        String page = "http://192.168.1.213:8090/httpAuth/app/rest/projects/id:Automation_Ct/buildTypes/id:" + project + "/builds";
        System.out.println(page);
        String authString = "Navot" + ":" + "good1luck";
        byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
        String authStringEnc = new String(authEncBytes);
        String result = null;
        try {
            URL url = new URL(page);
            URLConnection urlConnection = url.openConnection();
            urlConnection.setRequestProperty("Authorization", "Basic " + authStringEnc);
            InputStream is = urlConnection.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            int numCharsRead;
            char[] charArray = new char[1024];
            StringBuilder sb = new StringBuilder();
            while ((numCharsRead = isr.read(charArray)) > 0) {
                sb.append(charArray, 0, numCharsRead);
            }
            result = sb.toString();
            if (((HttpURLConnection) urlConnection).getResponseCode() < 300) {
                System.out.println(result);
            } else {
                throw new RuntimeException(result);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // System.out.println(result);

        Document doc = fromStringToDoc(result);

        String buildId = null;
        if (doc != null) {
            NodeList nList = doc.getElementsByTagName("build");
//            for (int i = 0; i < nList.getLength(); i++) {
            Node nNode = nList.item(0);
            Element eElement = (Element) nNode;

            buildId = eElement.getAttribute("id");
            System.out.println(getTime() + " " + "buildId : " + buildId);
//            }

        }

        return Integer.parseInt(buildId);

    }

    private static Document getFailures(int buildId) throws IOException, SAXException, ParserConfigurationException {
        String page = "http://192.168.1.213:8090/httpAuth/app/rest/testOccurrences?locator=build:(id:" + buildId + "),status:FAILURE,count:10000";
        System.out.println(page);
        String authString = "Navot" + ":" + "good1luck";
        byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
        String authStringEnc = new String(authEncBytes);
        String result = null;
        try {
            URL url = new URL(page);
            URLConnection urlConnection = url.openConnection();
            urlConnection.setRequestProperty("Authorization", "Basic " + authStringEnc);
            InputStream is = urlConnection.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            int numCharsRead;
            char[] charArray = new char[1024];
            StringBuilder sb = new StringBuilder();
            while ((numCharsRead = isr.read(charArray)) > 0) {
                sb.append(charArray, 0, numCharsRead);
            }
            result = sb.toString();
            if (((HttpURLConnection) urlConnection).getResponseCode() < 300) {
                System.out.println(result);
            } else {
                throw new RuntimeException(result);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //  System.out.println(result);

        return fromStringToDoc(result);
    }

    private static int getLastBuildFromDB(Projects project) {
        String SQL = "SELECT MAX(build) FROM " + project + ";";

        return  dbo.executeAndGet(SQL);
    }

    private static void saveToDB(Document doc, Projects project) {
        int fail = 0;
        NodeList nodes = doc.getElementsByTagName("testOccurrences");

        Element element = (Element) nodes.item(0);
        NodeList testList = element.getElementsByTagName("testOccurrence");
        for (int i = 0; i < testList.getLength(); i++) {
            Element testOccurrence = (Element) testList.item(i);
            String id = testOccurrence.getAttributes().getNamedItem("id").getNodeValue();
            String name = testOccurrence.getAttributes().getNamedItem("name").getNodeValue();
            String duration = testOccurrence.getAttributes().getNamedItem("duration").getNodeValue();

            try {
                String build = id.substring((id.lastIndexOf(":") + 1), id.lastIndexOf(")"));

                String testId = id.substring((id.indexOf(":") + 1), id.indexOf(","));

                String machineName = name.substring(0, name.indexOf(":"))
                        .replace("SeeTestAutomation.Runners.","");

                String packageName = name.substring((name.indexOf(":") + 2), name.lastIndexOf("."))
                        .replace("SeeTestAutomation.","")
                        .replace("AppiumStudio.","")
                        .replace("experiverse.tests.","")
                        .replace("Grid.runners.","");

                String testName = name.substring(name.lastIndexOf(".") + 1);


                String SQL = "INSERT INTO " + project + " (build,testId,machineName,packageName,testName, duration)" +
                        "VALUES (" + build + ",'" + testId + "','" + machineName + "','" + packageName + "','" + testName + "'," + duration + ");";
                System.out.println(SQL);

                if (dbo.execute(SQL)) {

                } else {
                    System.out.println("Failed To Insert - " + SQL);

                }

//                System.out.println("build: " + build+"    test id: " + testId+"    machine name: " + machineName+"    package: " + packageName+"    test name: " + testName+"    duration: " + duration);
            } catch (Exception e) {
                System.out.println("Failed To Parse - " + id + " - " + name + " - " + duration);
                fail++;
            }

        }
        System.out.println("fail: " + fail);
    }

    static Document fromStringToDoc(String result) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(result));
        Document doc = db.parse(is);
        doc.getDocumentElement().normalize();
        return doc;
    }

    static String getTime() {
        return df.format(new Date(System.currentTimeMillis()));
    }
}

