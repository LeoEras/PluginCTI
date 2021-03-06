package pluginPackage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public final class GlobalTime {
    // NIST, Boulder, Colorado  (time-a.timefreq.bldrdoc.gov)
    // public static final String ATOMICTIME_SERVER="132.163.4.101";
    // NIST, Gaithersburg, Maryland (time-a.nist.gov)
    // public static final String ATOMICTIME_SERVER="129.6.15.28";
    // NIST, Gaithersburg, Maryland  (time-c.nist.gov)
    public static final String ATOMICTIME_SERVER="129.6.15.30";
    public static final int ATOMICTIME_PORT = 13;


    public final static GregorianCalendar getAtomicTime() throws IOException{
        BufferedReader in = null;
        Socket conn = null;

        try {
            conn = new Socket(ATOMICTIME_SERVER, ATOMICTIME_PORT);

            in = new BufferedReader
                    (new InputStreamReader(conn.getInputStream()));

            String atomicTime;
            while (true) {
                if ( (atomicTime = in.readLine()).indexOf("*") > -1) {
                    break;
                }
            }
            //System.out.println("DEBUG 1 : " + atomicTime);
            String[] fields = atomicTime.split(" ");
            GregorianCalendar calendar = new GregorianCalendar();

            String[] date = fields[1].split("-");
            calendar.set(Calendar.YEAR, 2000 +  Integer.parseInt(date[0]));
            calendar.set(Calendar.MONTH, Integer.parseInt(date[1])-1);
            calendar.set(Calendar.DATE, Integer.parseInt(date[2]));

            // deals with the timezone and the daylight-saving-time (you may need to adjust this)
            // here i'm using "EST" for Eastern Standart Time (to support Daylight Saving Time)
            TimeZone tz = TimeZone.getTimeZone("EST"); // or .getDefault()
            int gmt = (tz.getRawOffset() + tz.getDSTSavings()) / 3600000;
            //System.out.println("DEBUG 2 : " + gmt);

            String[] time = fields[2].split(":");
            calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time[0]) + gmt);
            calendar.set(Calendar.MINUTE, Integer.parseInt(time[1]));
            calendar.set(Calendar.SECOND, Integer.parseInt(time[2]));
            return calendar;
        }
        catch (IOException e){
            throw e;
        }
        finally {
            if (in != null) { in.close();   }
            if (conn != null) { conn.close();   }
        }
    }

    public static Date getTimeFromServer(){
        Date dateobj;
        try {
            dateobj = GlobalTime.getAtomicTime().getTime();
        } catch(NoRouteToHostException nrthex) {
            System.out.println("No hay conexión con el servidor para obtener el tiempo. Tomando hora local.");
            dateobj = new Date();
        } catch(Exception ex) {
            System.out.println("Tomando hora local.");
            dateobj = new Date();
        }
        return dateobj;
    }

    public static boolean isWorkingTime(String startTime, String endTime){
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        Date time = GlobalTime.getTimeFromServer();
        Calendar c = Calendar.getInstance();
        c.setTime(time);

        if(c.get(Calendar.DAY_OF_WEEK) == Calendar.THURSDAY){
            try{
                Date now = sdf.parse(sdf.format(time));
                Date dstart = sdf.parse(startTime);
                Date dend = sdf.parse(endTime);
                if(now.compareTo(dstart) > 0 && now.compareTo(dend) < 0){
                    return true;
                }
            } catch (ParseException ex) {
                ex.printStackTrace();
            }
        }

        return false;
    }

    public static void main(String args[]) throws IOException {
        System.out.println(isWorkingTime("10:15:00", "12:45:00"));
    }


  /*
    ref : http://www.bldrdoc.gov/doc-tour/atomic_clock.html

                       49825 95-04-18 22:24:11 50 0 0 50.0 UTC(NIST) *

                       |     |        |     | | |  |      |      |
    These are the last +     |        |     | | |  |      |      |
    five digits of the       |        |     | | |  |      |      |
    Modified Julian Date     |        |     | | |  |      |      |
                             |        |     | | |  |      |      |
    Year, Month and Day <----+        |     | | |  |      |      |
                                      |     | | |  |      |      |
    Hour, minute, and second of the <-+     | | |  |      |      |
    current UTC at Greenwich.               | | |  |      |      |
                                            | | |  |      |      |
    DST - Daylight Savings Time code <------+ | |  |      |      |
    00 means standard time(ST), 50 means DST  | |  |      |      |
    99 to 51 = Now on ST, goto DST when local | |  |      |      |
    time is 2:00am, and the count is 51.      | |  |      |      |
    49 to 01 = Now on DST, goto ST when local | |  |      |      |
    time is 2:00am, and the count is 01.      | |  |      |      |
                                              | |  |      |      |
    Leap second flag is set to "1" when <-----+ |  |      |      |
    a leap second will be added on the last     |  |      |      |
    day of the current UTC month.  A value of   |  |      |      |
    "2" indicates the removal of a leap second. |  |      |      |
                                                |  |      |      |
    Health Flag.  The normal value of this    <-+  |      |      |
    flag is 0.  Positive values mean there may     |      |      |
    be an error with the transmitted time.         |      |      |
                                                   |      |      |
    The number of milliseconds ACTS is advancing <-+      |      |
    the time stamp, to account for network lag.           |      |
                                                          |      |
    Coordinated Universal Time from the National <--------+      |
    Institute of Standards & Technology.                         |
                                                                 |
    The instant the "*" appears, is the exact time. <------------+
  */
}