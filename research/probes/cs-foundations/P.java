import java.io.*;import java.net.*;import java.nio.file.*;import java.util.*;
public class P {
 static List<String> cat(String f) throws Exception{Process p=new ProcessBuilder("cat",f).start();try(BufferedReader r=new BufferedReader(new InputStreamReader(p.getInputStream()))){List<String> l=new ArrayList<>();String s;while((s=r.readLine())!=null)l.add(s);return l;}}
 public static void main(String[] a) throws Exception{
  if(a[0].equals("tw")){try(ServerSocket ss=new ServerSocket(0)){Thread t=new Thread(()->{try{while(true){Socket s=ss.accept();s.getInputStream().read();s.close();}}catch(Exception e){}});t.setDaemon(true);t.start();
   for(int i=0;i<2000;i++){try(Socket c=new Socket("127.0.0.1",ss.getLocalPort())){}}
   int sum=0;for(int w:new int[]{1,29,28,4}){Thread.sleep(w*1000);sum+=w;long n=cat("/proc/net/tcp").stream().filter(x->x.trim().split("\\s+")[3].equals("06")).count();System.out.println("TIME_WAIT at "+sum+"s: "+n);}
   System.out.println(cat("/proc/sys/net/ipv4/ip_local_port_range")+" reuse="+cat("/proc/sys/net/ipv4/tcp_tw_reuse"));}}
  else{Path p=Path.of("/tmp/big.bin");byte[] b=new byte[1<<20];new Random().nextBytes(b);try(OutputStream o=Files.newOutputStream(p)){for(int i=0;i<1024;i++)o.write(b);}
   for(int warm=0;warm<3;warm++){try(InputStream in=Files.newInputStream(p)){while(in.read(b)>0);}}
   for(int rep=0;rep<3;rep++){new ProcessBuilder("sh","-c","sync; echo 3 > /proc/sys/vm/drop_caches").inheritIO().start().waitFor();
    for(int r=1;r<=2;r++){long s=System.nanoTime();try(InputStream in=Files.newInputStream(p)){while(in.read(b)>0);}System.out.printf("rep %d read %d: %.0fms%n",rep,r,(System.nanoTime()-s)/1e6);}}Files.delete(p);}
 }}
