import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.InetSocketAddress;

public class RX {
  private Socket       socket = null;
  private InputStream  inputStream = null;
  private List<Packet> pList = null;

 public RX(String host, int port) throws IOException {
   socket = new Socket();
   socket.connect( new InetSocketAddress(host,port) ); 
 
   // ADD print here if you want
   System.out.println("connected to "+socket.getInetAddress().getHostAddress()+" on port "+port);

 }

 public void close() throws IOException {

  // ADD your code here
   socket.close();
 }

 public int run(String fname) {
   try {
     get_pkts();
     order_pkts();
     write_pkts(fname);
     close();
   } catch (Exception e) { e.printStackTrace(); }

   return 0;
 }

 /* get_pkts: pull all packets off the wire and store in pList
    this method also prints the packet info stats              */
 public int get_pkts ( ) throws IOException {

   // ADD your variables here
   byte[] recvPacket = new byte[1506];
   pList = new ArrayList<Packet>();
   long delay, totaldelay = 0, delayChecker = System.currentTimeMillis();
   int packetSizeRead = 0, totalSize = 0;

   int npackets = 0;  // how many packets read

   /* loop: get all packets and capture stats
      must use getInputStream.read()          */

   while((packetSizeRead = socket.getInputStream().read(recvPacket, 0, 1506)) != -1) {
     delay = System.currentTimeMillis() - delayChecker;
     delayChecker = System.currentTimeMillis(); 
     totaldelay += delay;
     totalSize = totalSize + (packetSizeRead-6);
     System.out.print("Pkt "+(++npackets)+" ");
     Packet rPacket = new Packet(recvPacket);
     System.out.println("delay = "+delay+" ms");
     if (!pList.contains(rPacket)){
       pList.add(rPacket);
     }   
   } // while (read from socket)

  // ADD print here
   System.out.println("Total "+npackets+" packets / "+totalSize+" bytes recd. Total delay = "+totaldelay+" ms, average = "+(totaldelay/npackets)+ " ms");

   return npackets;
 }

 public void write_pkts(String f) throws Exception {
  // this must call Packet.write() for each Packet
   FileOutputStream fos = new FileOutputStream(f);
   for (int i = 0; i < pList.size(); i++){
     pList.get(i).write(fos); 
   }
   fos.close();
 }

 // put the pList in the correct order
 // and remove duplicates
 public void order_pkts() {
   Collections.sort(pList);
 }

 public static void main(String[] args) {

   if(args.length != 3) {
     System.out.println("Usage: host  port filename");
     return;
   }

   try {
     RX recv = new RX( args[0],
                         Integer.parseInt(args[1]));
		recv.run ( args[2] );
   } catch (Exception e) { e.printStackTrace(); }
 } // main()

} // class RX

/* Packet class */
class Packet implements Comparable<Packet>{ 
 /* DO_NOT change these private fields */
 private byte[]      payload;
 private int         seqNo;
 private short       len;

 public Packet(byte[] buf) {
  seqNo = get_seqno(buf); // must use only this method to get sequence no
  len   = get_len(buf);   // must use only this method to get length

  // ADD code here
   System.out.print("SEQ=" + seqNo + ", len=" + len + ", ");

   payload = new byte[len];
   int arrayIter = 0;
   for(int loopIter = 6; loopIter < len+6; loopIter++){
     payload[arrayIter++] = buf[loopIter];
   }
 } // Packet CTOR

 /*Retrieve the sequence number from the packet header. The function converts the
 * stream bytes to bits and converts the bits to an integer. */
 private int   get_seqno(byte []b) {
  int seqno = 0;
 
  int[] bits = new int[32];
  int temp = 0;
  for(int j=0; j<4; j++){
    temp = (int) b[j];
    if(temp < 0){
      temp = (int) b[j] + 256;
    }
    for(int k = ((j+1)*8)-1; k >= (j*8); k--){
      bits[k] = temp%2;
      temp /= 2;
    }
  }

  int powerValue = 1;      
  for(int l = bits.length-1; l >= 1; l--){
    if(bits[l] == 1){
      seqno += powerValue;
    }
    powerValue = powerValue*2;
  }

  return seqno;
 }

 /*Retrieve the length of the payload from the packet header. The function converts the 
 * stream bytes to bits and converts the bits to an integer.*/
 private short get_len(byte []b) {
  short len = 0;

  int[] bits = new int[16];
  int temp = 0;
  for(int j=4; j<6; j++){
    temp = (int)  b[j];
    if(temp < 0){
      temp = (int) b[j] + 256;
    }
    for(int k = ((j+1-4)*8)-1; k >= ((j-4)*8); k--){
      bits[k] = temp%2;
      temp /= 2;
    }
  }

  int powerValue = 1;
  for(int l = bits.length-1; l >= 0; l--){
    if(bits[l] == 1){
      len += powerValue;
    }
    powerValue = powerValue*2;
  }
 
  return len;
 }

 // write this Packet to file: no need to change this
 public void write(FileOutputStream f) throws IOException {
   f.write(payload);
 }

 @Override
 public int compareTo(Packet P1){
  return this.seqNo - P1.seqNo;
 }

 @Override
 public boolean equals(Object P1){
  if(this.seqNo == ((Packet)P1).seqNo){
    return true;
  }
  return false;
 }
}  // class Packet
