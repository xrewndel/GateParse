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
    public int result = -1;
    
    private Stat() {}

    //05:21:00,230  INFO PortSenderThreadPORT-16 etisalat-gate:run:157 - Port 16. Got Command = CommandSet{id=0, commands=[Command{body=*139*102*24512*5152*2#}]}
    public static Stat request(String s) {
        Stat stat = new Stat();
        stat.time = getTime(s);
        int beginPort = s.indexOf("Port ") + 5;
        int endPort = s.indexOf(".", beginPort);
        stat.port = Integer.valueOf(s.substring(beginPort, endPort));
        stat.request = s.substring(s.indexOf("*"), s.indexOf("#"));
        
        return stat;
   }
    
    public static Stat body(StringBuilder sb) {
        Stat stat = new Stat();
        String[] str = sb.toString().split("\n");
        // 04:42:50,360  INFO Thread-70 etisalat-gate:run:115 - message = HEAD{
        String s0 = str[0]; 
        
        stat.time = getTime(s0);
        int b = s0.indexOf("-") + 1;
        int e = s0.indexOf("etisalat") - 1;
        stat.thread = Integer.valueOf(s0.substring(b, e));
        
        String body = sb.substring(sb.lastIndexOf("{") + 2).replaceAll("\n", " ");
        /*
        Body{
        0
        }
        
        BODY{
        16
        2
        Recharging subscriber 971557957803 of 5 More Time completed through Operation 2256233824257. 
        } 
        
        BODY{
        5
        1
        Thank you for using Etisalat eRecharge Service. Your Airtime mWallet balance is 0.00 ID 40780540 on 02/11/14.
        - - -
        00:menu
        0:<--
        }
        */
        stat.msg = body;
        
        str = body.split(" ");
        if (str.length == 1) stat.result = Integer.parseInt(str[0].trim());
        else if (str.length >= 2) stat.result = Integer.parseInt(str[1].trim());
        else System.out.println("not parsed: " + body);
        
        switch(stat.result) {
            case 0: stat.response = "NO_FURTHER_USER_ACTION_REQUIRED";  break;
            case 1: stat.response = "FURTHER_USER_ACTION_REQUIRED";     break;
            case 2: stat.response = "USSD_TERMINATED_BY_NETWORK";       break;
            case 3: stat.response = "";                                 break;
            case 4: stat.response = "OPERATION_NOT_SUPPORTED";          break;
            default: System.out.println("Unknown result code: " + stat.result);
        }
        
        return stat;
    }
    
    private static String getTime(String s) {
        return s.substring(0, s.indexOf(","));
    }
    
    @Override public String toString() {
        return "Stat{" + "time=" + time + ", port=" + port + ", thread=" + thread + " request=" + request + ", response=" + response + ", msg=" + msg  + '}';
    }
    
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
