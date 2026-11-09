import java.io.*;import java.net.*;import java.nio.*;import java.nio.channels.*;import java.nio.file.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.locks.*;
public class M {
 static void nagle(boolean nodelay) throws Exception {
  try(ServerSocket ss=new ServerSocket(0)){
   Thread t=new Thread(()->{try(Socket s=ss.accept()){s.setTcpNoDelay(true);InputStream in=s.getInputStream();OutputStream o=s.getOutputStream();byte[] b=new byte[2];
     while(true){int n=0;while(n<2){int r=in.read(b,n,2-n);if(r<0)return;n+=r;}o.write(1);}}catch(Exception e){}});t.setDaemon(true);t.start();
   try(Socket c=new Socket("127.0.0.1",ss.getLocalPort())){c.setTcpNoDelay(nodelay);OutputStream o=c.getOutputStream();InputStream in=c.getInputStream();
    long[] l=new long[50];for(int i=0;i<50;i++){long a=System.nanoTime();o.write(1);o.flush();o.write(2);o.flush();in.read();l[i]=System.nanoTime()-a;}
    Arrays.sort(l);System.out.printf("nagle nodelay=%s median=%.3fms max=%.3fms%n",nodelay,l[25]/1e6,l[49]/1e6);}}}
 static void stall(int threads,long ms) throws Exception {
  long end=System.nanoTime()+ms*1_000_000;ConcurrentLinkedQueue<Long> gaps=new ConcurrentLinkedQueue<>();Thread[] ts=new Thread[threads];
  for(int i=0;i<threads;i++){ts[i]=new Thread(()->{long p=System.nanoTime();while(p<end){long n=System.nanoTime();if(n-p>1_000_000)gaps.add(n-p);p=n;}});ts[i].start();}
  for(Thread t:ts)t.join();List<Long> g=new ArrayList<>(gaps);Collections.sort(g);long tot=0;for(long x:g)tot+=x;
  System.out.printf("stall threads=%d gaps>1ms=%d max=%.1fms median=%.1fms totalStalled=%.0fms-per-thread-avg of %dms%n",threads,g.size(),g.isEmpty()?0:g.get(g.size()-1)/1e6,g.isEmpty()?0:g.get(g.size()/2)/1e6,tot/1e6/threads,ms);
 }
 static void info(){Runtime r=Runtime.getRuntime();System.out.printf("cpus=%d maxHeap=%dMB%n",r.availableProcessors(),r.maxMemory()/1048576);}
 static void oom(){List<byte[]> k=new ArrayList<>();try{for(;;){byte[] b=new byte[1<<20];Arrays.fill(b,(byte)1);k.add(b);}}catch(OutOfMemoryError e){int n=k.size();k=null;System.out.println("OutOfMemoryError after MB="+n);}}
 static void fsync(int n) throws Exception{Path p=Files.createTempFile(Path.of("/tmp"),"f",".x");try(FileChannel c=FileChannel.open(p,StandardOpenOption.WRITE)){for(boolean f:new boolean[]{false,true}){long a=System.nanoTime();for(int i=0;i<n;i++){c.write(ByteBuffer.wrap(new byte[4096]));if(f)c.force(false);}double us=(System.nanoTime()-a)/1e3/n;System.out.printf("fsync=%s avg=%.1fus perSec=%.0f%n",f,us,1e6/us);}}Files.delete(p);}
 static void pagecache() throws Exception{Path p=Path.of("/tmp/big.bin");byte[] b=new byte[1<<20];new Random().nextBytes(b);try(OutputStream o=Files.newOutputStream(p)){for(int i=0;i<512;i++)o.write(b);}
  new ProcessBuilder("sh","-c","sync; echo 3 > /proc/sys/vm/drop_caches").inheritIO().start().waitFor();
  for(int r=1;r<=2;r++){long a=System.nanoTime();try(InputStream in=Files.newInputStream(p)){while(in.read(b)>0);}System.out.printf("read %d: %.0fms%n",r,(System.nanoTime()-a)/1e6);}Files.delete(p);}
 static void timewait() throws Exception{try(ServerSocket ss=new ServerSocket(0)){Thread t=new Thread(()->{try{while(true){Socket s=ss.accept();s.getInputStream().read();s.close();}}catch(Exception e){}});t.setDaemon(true);t.start();
   for(int i=0;i<2000;i++){try(Socket c=new Socket("127.0.0.1",ss.getLocalPort())){c.setSoLinger(false,0);}}Thread.sleep(200);
   for(int w:new int[]{0,30,29,5}){Thread.sleep(w*1000);long n=Files.readAllLines(Path.of("/proc/net/tcp")).stream().filter(x->x.trim().split("\\s+")[3].equals("06")).count();System.out.println("TIME_WAIT after +"+w+"s: "+n);}
   System.out.println(Files.readString(Path.of("/proc/sys/net/ipv4/ip_local_port_range")).trim()+" tw_reuse="+Files.readString(Path.of("/proc/sys/net/ipv4/tcp_tw_reuse")).trim());}}
 static class Pad{volatile long a;long p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15;volatile long b;}
 static class NoPad{volatile long a;volatile long b;}
 static void sharing() throws Exception{for(int rep=0;rep<3;rep++){long n=200_000_000L;
   NoPad x=new NoPad();long a=System.nanoTime();Thread t1=new Thread(()->{for(long i=0;i<n;i++)x.a++;}),t2=new Thread(()->{for(long i=0;i<n;i++)x.b++;});t1.start();t2.start();t1.join();t2.join();double s=(System.nanoTime()-a)/1e6;
   Pad y=new Pad();a=System.nanoTime();t1=new Thread(()->{for(long i=0;i<n;i++)y.a++;});t2=new Thread(()->{for(long i=0;i<n;i++)y.b++;});t1.start();t2.start();t1.join();t2.join();double p=(System.nanoTime()-a)/1e6;System.out.printf("same-line=%.0fms padded=%.0fms%n",s,p);}}
 static void access() {int n=1<<26;int[] a=new int[n];for(int i=0;i<n;i++)a[i]=i;Random r=new Random(1);int[] idx=new int[n];for(int i=0;i<n;i++)idx[i]=i;for(int i=n-1;i>0;i--){int j=r.nextInt(i+1);int t=idx[i];idx[i]=idx[j];idx[j]=t;}
   for(int rep=0;rep<3;rep++){long s=0,t=System.nanoTime();for(int i=0;i<n;i++)s+=a[i];double q=(System.nanoTime()-t)/(double)n;t=System.nanoTime();for(int i=0;i<n;i++)s+=a[idx[i]];double w=(System.nanoTime()-t)/(double)n;System.out.printf("seq=%.2fns random=%.2fns (%d)%n",q,w,s&1);}}
 static void pingpong() throws Exception{int n=200000;for(int rep=0;rep<3;rep++){SynchronousQueue<Integer> a=new SynchronousQueue<>(),b=new SynchronousQueue<>();
   Thread t=new Thread(()->{try{for(int i=0;i<n;i++){b.put(a.take());}}catch(Exception e){}});t.start();long s=System.nanoTime();for(int i=0;i<n;i++){a.put(i);b.take();}t.join();System.out.printf("handoff=%.2fus per one-way%n",(System.nanoTime()-s)/1e3/(2*n));}}
 static void spawn() throws Exception{for(int rep=0;rep<2;rep++){int n=10000;long a=System.nanoTime();Thread[] ts=new Thread[n];CountDownLatch l=new CountDownLatch(1);for(int i=0;i<n;i++){ts[i]=new Thread(()->{try{l.await();}catch(Exception e){}});ts[i].start();}double p=(System.nanoTime()-a)/1e6;l.countDown();for(Thread t:ts)t.join();
   a=System.nanoTime();CountDownLatch m=new CountDownLatch(1);for(int i=0;i<n;i++){ts[i]=Thread.ofVirtual().start(()->{try{m.await();}catch(Exception e){}});}double v=(System.nanoTime()-a)/1e6;m.countDown();for(Thread t:ts)t.join();System.out.printf("10k platform=%.0fms virtual=%.0fms%n",p,v);}}
 static void zc() throws Exception{Path p=Path.of("/tmp/zc.bin");byte[] b=new byte[1<<20];try(OutputStream o=Files.newOutputStream(p)){for(int i=0;i<1024;i++)o.write(b);}
  try(ServerSocketChannel ss=ServerSocketChannel.open().bind(new InetSocketAddress("127.0.0.1",0))){Thread t=new Thread(()->{try{ByteBuffer bb=ByteBuffer.allocateDirect(1<<20);while(true){try(SocketChannel s=ss.accept()){while(s.read(bb)>=0)bb.clear();}}}catch(Exception e){}});t.setDaemon(true);t.start();
  for(int rep=0;rep<4;rep++){long a=System.nanoTime();try(SocketChannel c=SocketChannel.open(ss.getLocalAddress());InputStream in=Files.newInputStream(p)){ByteBuffer bb=ByteBuffer.wrap(new byte[1<<16]);int r;while((r=in.read(bb.array()))>0){bb.position(0).limit(r);while(bb.hasRemaining())c.write(bb);}}double rw=(System.nanoTime()-a)/1e6;
   a=System.nanoTime();try(SocketChannel c=SocketChannel.open(ss.getLocalAddress());FileChannel f=FileChannel.open(p)){long pos=0,sz=f.size();while(pos<sz)pos+=f.transferTo(pos,sz-pos,c);}double tz=(System.nanoTime()-a)/1e6;System.out.printf("1GB read+write=%.0fms transferTo=%.0fms%n",rw,tz);}}Files.delete(p);}
 public static void main(String[] a) throws Exception{switch(a[0]){case "nagle"->{nagle(false);nagle(true);nagle(false);nagle(true);}case "stall"->stall(Integer.parseInt(a[1]),Long.parseLong(a[2]));case "info"->info();case "oom"->oom();case "fsync"->fsync(Integer.parseInt(a[1]));case "pagecache"->pagecache();case "timewait"->timewait();case "sharing"->sharing();case "access"->access();case "pingpong"->pingpong();case "spawn"->spawn();case "zc"->zc();}}
}
