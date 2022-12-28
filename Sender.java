import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.net.ServerSocket;

/* Running the sender 
   java Sender 2002 sample.txt 1 1
   java classFile port file testOption randomPayload

   testOption range 1-5
   randomPayload - give any value to enable

   testOption
   	1 for duplicate packets
        2 for outOFOrder packets
        3 for droping packets
        4 for corrupt packets
        5 enables all
        Any other value default send

 Implemented duplicate packets and outOfOrder packets 
*/

public class Sender {
  public static void main(String args[]) {

    boolean duplicates = false, outOfOrder = false, dropPackets = false, corruptPackets = false;
    boolean randomGen = false;
    int dupTimes = 10, oooTimes = 14, dropTimes = 15, corTimes = 20, pktNo = 1;
    int returnTimes = oooTimes * 2 - 4;
    int dupPacketSent = 0, oooPacSent = 0;

    //Choosing the testing parameters for sending.
    if (args.length > 2) {
      switch (Integer.parseInt(args[2])) {
        case 1:
          duplicates = true;
          break;
        case 2:
          outOfOrder = true;
          break;
        case 3:
        //Drop packets not implemented
          dropPackets = true;
          break;
        case 4:
        //Corrupt packets not implemented
          corruptPackets = true;
          break;
        case 5:
          duplicates = true;
          outOfOrder = true;
          dropPackets = true;
          corruptPackets = true;
          break;
        default:
          duplicates = false;
          outOfOrder = false;
          dropPackets = false;
          corruptPackets = false;
      }
    }
    if (args.length > 3) {
      randomGen = true;
    }

    ArrayList<byte[]> oooList = new ArrayList<byte[]>();

    try {
      //Socket creation
      ServerSocket serverSocket = new ServerSocket(Integer.parseInt(args[0]));
      Socket socket = serverSocket.accept();
      System.out.println("Client connected");
      FileInputStream fis = new FileInputStream(args[1]);

      byte[] fileContents = new byte[1506];
      Random rand = new Random();
      int noOfBytesRead = 0, arraySize = 1500;
      long timer = System.currentTimeMillis(), timeChecker = 0;

      if (randomGen) {
        //Not required for fixed packet size, like a tcp packet which would ideally have 1500 bytes
        arraySize = rand.nextInt(1501);
      }
      noOfBytesRead = fis.read(fileContents, 6, arraySize);

      //Initiate sequence number
      int seqNo = 5;
      int[] bits = new int[48];

      //Execute until end of the file is reached
      while (noOfBytesRead != -1) {

        //arraySize is the no of bytes that is sent a payload
        if (noOfBytesRead < arraySize) {
          arraySize = noOfBytesRead;
        }

        //Initiating a packet with header size 6
        byte[] packet = new byte[6 + arraySize];
        System.out.print("SeqNo: " + seqNo);
        int tempSeq = seqNo;
        //First bit is not used.
        bits[0] = 0;

        //4 bytes - 1 bit of sequence numbers
        for (int j = 31; j >= 1; j--) {
          bits[j] = tempSeq % 2;
          tempSeq /= 2;
        }
        System.out.print(", PayloadSize: " + arraySize + ", PacketSize: " + (arraySize + 6));
        int temparraySize = arraySize;

        //2 bytes of payload size
        for (int j = 47; j >= 32; j--) {
          bits[j] = temparraySize % 2;
          temparraySize /= 2;
        }
        int processBits = 0, fileArrayIter = 0;
        do {
          int byteValue = 0;
          int powerValue = 128;

          // Converting a byte into its corresponding decimal value and assigning to the byte array
          for (int j = 0; j < 8; j++) {
            if (powerValue == 0)
              powerValue = 1;
            byteValue += bits[processBits] * powerValue;
            powerValue /= 2;
            processBits++;
          }
          if (byteValue > 127)
            byteValue -= 256;
          fileContents[fileArrayIter++] = (byte) byteValue;
        } while (processBits < 48);
        packet = Arrays.copyOfRange(fileContents, 0, 6 + arraySize);

        //Storing packets to be sent later
        if (outOfOrder && pktNo % oooTimes == 0) {
          System.out.println(".\n\t Storing the packet with seq No " + (seqNo) + " packet for sending later");
          oooList.add(packet);
          oooPacSent++;
        } else {
          //Sending packets
          System.out.println(", Packet delay is 20 ms");
          Thread.sleep(20);
          socket.getOutputStream().write(fileContents, 0, arraySize + 6);
          if (duplicates && pktNo % dupTimes == 0) {
            int duplicatesCount = rand.nextInt(3);

            //Sending duplicate packets
            while (duplicatesCount-- > -1) {
              dupPacketSent++;
              System.out.println("Sending a duplicate packet");
              Thread.sleep(20);
              socket.getOutputStream().write(packet, 0, arraySize + 6);
            }
          }
        }
        seqNo += arraySize;
        if (randomGen) {
          arraySize = rand.nextInt(1501);
        }
        noOfBytesRead = fis.read(fileContents, 6, arraySize);

        //Send the stored packets at equal intervals or when the entire file has been sent
        if (outOfOrder && ((pktNo % returnTimes == 0 && !oooList.isEmpty()) || (noOfBytesRead == -1))) {
          while (!oooList.isEmpty()) {
            System.out.println("Sending a out of order packet");
            Thread.sleep(20);
            socket.getOutputStream().write(oooList.get(0), 0, oooList.get(0).length);
            oooList.remove(0);
          }
        }
        pktNo++;
      }
      timeChecker = System.currentTimeMillis();
      System.out.println("Packet sent correctly is " + (pktNo - 1 - oooPacSent));
      System.out.println("Packet sent outOfOrder is " + oooPacSent);
      System.out.println("Packet sent as duplicates is " + dupPacketSent);
      System.out.println("Total time taken for sender to send: " + ((timeChecker - timer) / 1000) + " seconds");
      fis.close();
      socket.close();
      serverSocket.close();
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }
}
