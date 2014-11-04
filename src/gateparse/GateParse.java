package gateparse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 *
 * @author Andrew
 */
public class GateParse {
    private final static Logger csv = Logger.getLogger("csv");
    private final static Logger main = Logger.getLogger("main");
    private static String path = "";
    private static List<Stat> requests = new ArrayList<Stat>();
    private static List<Stat> responseBodies = new ArrayList<Stat>();
    private static String date = "";

    public static void main(String[] args) {
        // settings for log
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
        System.setProperty("current.date", dateFormat.format(new Date()));
        // set path
        if (args.length > 1) path = args[1];
        File cfgFile = new File(args[0]);
        PropertyConfigurator.configure(cfgFile.getAbsolutePath());
        
        StringBuilder sb = new StringBuilder()
            .append("Date").append(";")
            .append("Request time").append(";")
            .append("Response time").append(";")
            .append("Request (USSD)").append(";")
            .append("Response").append(";")
            .append("Message");
        csv.debug(sb);
        
        File[] files = getFilesInDir(path);
        Set<File> flist = new TreeSet<File>(Arrays.asList(files));
        for (File file : flist) {
            //clear(file.getAbsolutePath());
            read(file.getAbsolutePath());
        }
        System.out.println("Total files: " + files.length);
        System.out.println("After all:");
        for (Stat stat : requests) System.out.println(stat);
        System.out.println("");
        for (Stat stat : responseBodies) System.out.println(stat);
    }
    
    public static File[] getFilesInDir(String path) {
        File[] files = new File(path).listFiles(new FilenameFilter() {
            @Override public boolean accept(File directory, String fileName) {
                return fileName.startsWith("etisalat-gate.log");
            }
        });
        
        return files;
    }
    
    // Чтение файла
    private static void read(String fileName) {
        System.out.println("Read " + fileName);
        date = fileName.substring(fileName.lastIndexOf(".") + 1);
        System.out.println("DATE:" + date);
        parse(fileName);
    }
    
    private static void clear(String filename) {
        String s0 = "EtisalatWorker-&566 etisalat-gate:printPorts";
        String s1 = "EtisalatWorker";
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));
            while (in.ready()) {
                String s = in.readLine();
                if (!s.contains(s1)) main.debug(s);
            }
            in.close();
        } catch (Exception ex) { System.out.println(ex);  }
    }
    
    
    private static void parse(String filename) {
        StringBuilder bodyBuf = new StringBuilder();
        boolean isBody = false;
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));
            while (in.ready()) {
                String s = in.readLine();
                //System.out.println("Parse:" + s);
                if (s.contains("body=*")) {                                     // request
                    Stat request = Stat.request(s);
                    requests.add(request);
                }
                else if(s.contains("message = HEAD{")) {                        // response body
                    isBody = true;
                    bodyBuf = new StringBuilder();
                }
                else if (s.contains("setUssdDwgResults") && s.contains("Got Result = ")) {  //response result
                    // 19:11:53,291  INFO Thread-76 etisalat-gate:setUssdDwgResults:65 - Port 25. Got Result = 1
                    int port = Integer.valueOf(s.substring(s.indexOf("Port ") + 5, s.indexOf(".")));
                    int result = Integer.valueOf(s.substring(s.indexOf("=") + 1).trim());
                    // идем по запросам и ищем запрос с таким портом
                    Stat request = findRequest(port);
                    //идем по ответам и ищем первый с таким результатом
                    if (request != null) {
                        Stat body = findBody(result);
                        if (body != null) print(request, body);
                    }
                }
                
                if(isBody) {
                    if (s.equals("}")) { // end
                        isBody = false;
                        Stat responseBody = Stat.body(bodyBuf);
                        responseBodies.add(responseBody);
                    }
                    else bodyBuf.append(s).append("\n");
                }
                
            }
            in.close();
        } catch (Exception ex) { System.out.println(ex);  }
    }
    
    private static Stat findRequest(int port) {
        Stat request = null;
        boolean found = false;
        Iterator it = requests.listIterator();
        while (it.hasNext() && !found) {
            Stat stat = (Stat) it.next();
            if (stat.port == port){
                request = stat;
                found = true;
                it.remove();
            }
        }
        
        return request;
    }
    
    private static Stat findBody(int result) {
        Stat body = null;
        boolean found = false;
        Iterator it = responseBodies.listIterator();
        while (it.hasNext() && !found) {
            Stat stat = (Stat) it.next();
            if (stat.result == result){
                body = stat;
                found = true;
                it.remove();
            }
        }
        
        return body;
    }
    
    private static void print(Stat request, Stat response) {
        StringBuilder sb = new StringBuilder()
            .append(date)             .append(";")
            .append(request.time)     .append(";")
            .append(response.time)    .append(";")
            .append(request.request)  .append(";")
            .append(response.response).append(";")
            .append(response.msg);
          csv.debug(sb);
    }
}
