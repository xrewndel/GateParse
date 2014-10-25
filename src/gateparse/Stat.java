package gateparse;

/**
 *
 * @author Andrew
 */
public class Stat {
    public String time = "";
    public int port = 0;
    public String request = "";
    public String response = "";
    public int thread = 0;
    public String msg = "";
    
    public Stat(String s) { 
        time = s.substring(0, s.indexOf(","));
        int beginPort = s.indexOf("Port ") + 5;
        int endPort = s.indexOf(".", beginPort);
        port = Integer.valueOf(s.substring(beginPort, endPort));
        
        if (s.contains("setUssdDwgResults")) {
            response = s.substring(s.indexOf(".") + 2);
            int b = s.indexOf("Thread-") + 7;
            int e = s.indexOf("etisalat") - 1;
            thread = Integer.valueOf(s.substring(b, e));
        }
        else 
            request = s.substring(s.indexOf("*"), s.indexOf("#"));
    }
    
    public Stat(StringBuilder sb) { 
        try {
            time = sb.substring(0, sb.indexOf(" "));
            int b = sb.indexOf("-") + 1;
            int e = sb.indexOf("etisalat") - 1;
            thread = Integer.valueOf(sb.substring(b, e));

            msg = sb.substring(sb.indexOf("{"), sb.indexOf("}") + 1);
            msg = msg.replaceAll("\n", " ");
            
            if (!msg.contains("Operation not supported")) msg = "";
            

            /*String[] str = sb.toString().split("\n");
            for (String s : str) {
                if (s.contains("DEBUG") && s.contains("setUssdDwgResults")) {
                    int beginPort = s.indexOf("Port ") + 5;
                    int endPort = s.indexOf(".", beginPort);
                    port = Integer.valueOf(s.substring(beginPort, endPort));
                    response = s.substring(s.indexOf(".") + 2);
                }
            }*/
        } catch(Exception ex) {
            System.out.println("Parse:" + sb);
            System.out.println(ex);
        }
         
        if (thread ==0) {
            System.out.println(sb);
            System.out.println("---Thread: " + thread);
        }
        //System.out.println("---Time: " + time);
        //System.out.println("---Thread: " + thread);
        //System.out.println("---Message: " + message);
        //System.out.println("---Port: " + port);
        //System.out.println("---Response: " + response);
    }
   
    @Override public String toString() {
        return "Stat{" + "time=" + time + ", port=" + port + ", thread=" + thread + " request=" + request + ", response=" + response + ", msg=" + msg  + '}';
    }
    
    public boolean isRequest() { return !request.isEmpty(); }
    
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 71 * hash + (this.time != null ? this.time.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final Stat other = (Stat) obj;
        return this.port == other.port;
    }
}
