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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 *
 * @author Andrew
 */
public class GateParse {
    private final static Logger csv = Logger.getLogger("csv");
    private static String path = "";
    private static List<Stat> requests = new ArrayList<Stat>();
    private static List<Stat> responseBody = new ArrayList<Stat>();
    private static String date = "";
    private static String time = "";
    private static Pattern r;
    private static Pattern timeStamp;
    
    public static void main(String[] args) {
        // "03:05:27,499";
        String timePattern = "\\d{2}:\\d{2}:\\d{2}\\,\\d{3}";
        timeStamp = Pattern.compile(timePattern);
        // "03:05:27,499  INFO PortSenderThreadPORT-103 etisalat-gate:run:157 - Port 103. Got Command = CommandSet{id=6846232, commands=[Command{body=*130*5462*20*0502712443*1#}]}";
        String reqPattern = "(\\d{2}:\\d{2}:\\d{2}\\,\\d{3})(.*\\{)(.*)";
        r = Pattern.compile(reqPattern);
        
        Matcher m = timeStamp.matcher("03:05:27,499");
        if (m.find( )) {
            System.out.println("Found value: " + m.group());
            //System.out.println("Found value: " + m.group(2));
            //System.out.println("Found value: " + m.group(3));
        } else {
           System.out.println("NO MATCH");
        }
        
        //System.exit(1);
        
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
            read(file.getAbsolutePath());
        }
        System.out.println("Total files: " + files.length);
        System.out.println("After all:");
        for (Stat stat : requests) System.out.println(stat);
        System.out.println("");
        for (Stat stat : responseBody) System.out.println(stat);
    }
    
    private static File[] getFilesInDir(String path) {
        File[] files = new File(path).listFiles(new FilenameFilter() {
            @Override public boolean accept(File directory, String fileName) {
                return fileName.startsWith("etisalat-gate.log");
            }
        });
        
        return files;
    }
    
    // Чтение файла
    private static List<Stat> read(String filename) {
        System.out.println("Read " + filename);
        date = filename.substring(filename.lastIndexOf(".") + 1);
        System.out.println("DATE:" + date);
        return readTxtFile(filename);
    }
    
    private static List<Stat> readTxtFile(String filename) {
        boolean add = false;
        String currTime = "";
        StringBuilder buffer = new StringBuilder();
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));
            while (in.ready()) {
                String s = in.readLine();
                if (!s.contains("EtisalatWorker")) {
                    //System.out.println("Parse:" + s);
                    Matcher m = timeStamp.matcher(s);
                    if (m.find()) time = m.group();

                    if (s.contains("body=*") || (s.contains("setUssdDwgResults") && !s.contains("Got Result") && !s.contains("Send"))) {
                        Stat stat = new Stat(s);
                        //System.out.println(stat);
                        if (stat.isRequest()) requests.add(stat);
                        else {
                            boolean gotRequest = false;
                            Iterator itReq = requests.listIterator();
                            while(itReq.hasNext() && !gotRequest) {
                                Stat request = (Stat) itReq.next();
                                if (request.equals(stat)) {
                                    Stat body = null;
                                    if (stat.response.contains("OPERATION_NOT_SUPPORTED")) body = body();
                                    
                                    StringBuilder sb = new StringBuilder()
                                      .append(date)             .append(";")
                                      .append(request.time)     .append(";")
                                      .append(stat.time)        .append(";")
                                      .append(request.request)  .append(";")
                                      .append(stat.response);//    .append("\n");
                                    if (body != null) sb.append(";").append(body.msg);
                                    csv.debug(sb);
                                    itReq.remove();
                                    gotRequest = true;
                                }
                            }
                        }
                    }
                    
                    if (s.contains("message = HEAD{")) { 
                        add = true;
                        buffer = new StringBuilder();
                        
                        //m = timeStamp.matcher(s);
                        currTime = m.group();
                    }
                    if (!time.equals(currTime) && add) {
                        buffer.append(s);
                        add = false;
                        // parse
                        //System.out.println("Buffer:\n" + buffer + "\n");
                        Stat stat = new Stat(buffer);
                        if (!stat.msg.isEmpty())responseBody.add(stat);
                        /*
                        boolean gotRequest = false;
                        Iterator it = requests.listIterator();
                        while(it.hasNext() && !gotRequest) {
                            Stat request = (Stat) it.next();
                            if (request.equals(stat)) {
                                StringBuilder sb = new StringBuilder()
                                  .append(date)             .append(";")
                                  .append(request.time)     .append(";")
                                  .append(stat.time)        .append(";")
                                  .append(request.request)  .append(";")
                                  .append(stat.response)    .append(";")
                                  .append(stat.message);

                                csv.debug(sb);
                                it.remove();
                                gotRequest = true;
                            }
                        }
                        */
                    }

                    if (add) buffer.append(s).append("\n");
                    
            }
            }
            in.close();
        } catch (Exception ex) { System.out.println(ex);  }

        return new ArrayList<Stat>();
    }
    
    private static Stat body() {
        Stat body = null;
        Iterator it = responseBody.iterator();
        while(it.hasNext()) {
            Stat stat = (Stat) it.next();
            body = stat;
            it.remove();
        }
        
        return body;
    }
}
