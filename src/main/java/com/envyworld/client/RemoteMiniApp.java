package com.envyworld.client;

import com.envyworld.EnvyWorld;
import com.envyworld.logs.*;
import com.google.gson.*;
import com.sun.net.httpserver.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/** Ephemeral loopback server. Only signed Telegram owner sessions can read frames or control the game. */
public final class RemoteMiniApp {
    private static final String VERSION="2026.10.3",SHA="76b2805253d0d6ffacb092a7acfbee99e7af6917835c0c5410e23f3ef5052da2";
    private static final ExecutorService WORK=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"EnvyWorld Mini App setup");t.setDaemon(true);return t;});
    private static final Map<String,Long> SESSIONS=new ConcurrentHashMap<>();
    private static volatile String url="",status="Выключен";
    private static volatile boolean active;
    private static volatile long epoch;
    private static volatile long viewedAt;
    private static final java.util.concurrent.atomic.AtomicInteger streams=new java.util.concurrent.atomic.AtomicInteger();
    public static boolean viewing(){return active&&System.currentTimeMillis()-viewedAt<5000;}
    private static HttpServer server;private static Process tunnel;private static ExecutorService handlers;
    private RemoteMiniApp(){}
    public static String url(){return url;}
    public static synchronized String localUrl(){return server==null?"":"http://127.0.0.1:"+server.getAddress().getPort();}
    public static String status(){return status;}
    public static void start(String owner,String token){
        synchronized(RemoteMiniApp.class){if(active)return;active=true;status="Подготавливаем защищённое подключение…";long generation=++epoch;WORK.execute(()->open(owner,token,generation));}
    }
    private static void open(String owner,String token,long generation){
        try{
            if(!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows"))throw new IllegalStateException("Туннель пока поддерживает Windows x64");
            synchronized(RemoteMiniApp.class){
                if(!active||epoch!=generation)return;
                server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),16);
                handlers=Executors.newFixedThreadPool(4,r->{var t=new Thread(r,"EnvyWorld Mini App HTTP");t.setDaemon(true);return t;});server.setExecutor(handlers);
                server.createContext("/",exchange->handle(exchange,owner,token));server.start();
            }
            if(sshTunnel(generation))return;
            if(!active||epoch!=generation)return;
            Path binary=EnvyWorld.dataDir().resolve("tools/cloudflared-"+VERSION+".exe");
            if(!Files.isRegularFile(binary)||!digest(Files.readAllBytes(binary)).equals(SHA)){
                status="Первый запуск: загружаем Cloudflare Tunnel (55 МБ)…";
                var request=HttpRequest.newBuilder(URI.create("https://github.com/cloudflare/cloudflared/releases/download/"+VERSION+"/cloudflared-windows-amd64.exe")).timeout(Duration.ofMinutes(3)).build();
                var response=HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(10)).build().send(request,HttpResponse.BodyHandlers.ofByteArray());
                if(response.statusCode()!=200||!digest(response.body()).equals(SHA))throw new IllegalStateException("Не удалось проверить файл Cloudflare Tunnel");
                Files.createDirectories(binary.getParent());Files.write(binary,response.body());
            }
            synchronized(RemoteMiniApp.class){
                if(!active||epoch!=generation)return;
                tunnel=new ProcessBuilder(binary.toString(),"tunnel","--no-autoupdate","--protocol","http2","--edge-ip-version","4","--url",localUrl()).redirectErrorStream(true).start();
            }
            status="Ожидаем адрес HTTPS…";
            Process process=tunnel;
            CompletableFuture.delayedExecutor(50,TimeUnit.SECONDS).execute(()->{if(active&&epoch==generation&&url.isBlank()&&process.isAlive()){process.destroy();status="Туннель не подключился. Используем кадры и кнопки Telegram. Для повтора закройте и откройте эфир";TelegramNotify.refreshPanel();}});
            String candidate="",problem="";
            try(var lines=new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream(),StandardCharsets.UTF_8))){
                String line;while((line=lines.readLine())!=null&&active&&epoch==generation){
                    var match=java.util.regex.Pattern.compile("https://[a-z0-9-]+\\.trycloudflare\\.com").matcher(line);
                    if(match.find()&&!match.group().equals("https://api.trycloudflare.com")){candidate=match.group();status="Адрес создан. Проверяем канал Cloudflare…";}
                    if(line.contains("lookup")&&line.contains("no such host"))problem="DNS не возвращает служебные записи Cloudflare";
                    if(line.contains("TLS handshake")||line.contains("timeout"))problem="Сеть не пропускает соединение с Cloudflare";
                    if(line.contains("Serve tunnel error")||line.contains("Unregistered tunnel connection")){url="";status="Cloudflare переподключается. Кадры Telegram доступны";}
                    if(line.contains("Registered tunnel connection")&&!candidate.isBlank()){url=candidate;status="Mini App подключён. Откройте панель из Telegram";TelegramNotify.refreshPanel();}
                }
            }
            if(active&&epoch==generation){url="";status=(problem.isBlank()?"Туннель отключился":problem)+". Доступны кадры и кнопки Telegram. Для повтора закройте и откройте эфир";TelegramNotify.refreshPanel();}
        }catch(Exception failure){if(epoch==generation){url="";status=(failure instanceof IllegalStateException?failure.getMessage():"Не удалось открыть туннель. Проверьте доступ к Cloudflare и GitHub")+". Доступны кадры Telegram";TelegramNotify.refreshPanel();}}
    }
    /** Free HTTPS tunnel via system OpenSSH; never reads personal SSH config, keys or agent. */
    private static boolean sshTunnel(long generation)throws Exception {
        Path ssh=Path.of(System.getenv().getOrDefault("SystemRoot","C:\\Windows"),"System32","OpenSSH","ssh.exe");
        if(!Files.isRegularFile(ssh))return false;
        Path hosts=EnvyWorld.dataDir().resolve("tools/remote-known-hosts").toAbsolutePath();Files.createDirectories(hosts.getParent());
        Process process;
        synchronized(RemoteMiniApp.class){
            if(!active||epoch!=generation)return true;
            process=new ProcessBuilder(ssh.toString(),"-F","NUL","-o","BatchMode=yes","-o","IdentitiesOnly=yes","-o","IdentityFile=none","-o","IdentityAgent=none","-o","StrictHostKeyChecking=accept-new","-o","UserKnownHostsFile="+hosts,"-o","ConnectTimeout=10","-o","ExitOnForwardFailure=yes","-o","ServerAliveInterval=15","-o","ServerAliveCountMax=2","-T","-R","80:127.0.0.1:"+server.getAddress().getPort(),"nokey@localhost.run","--","--output","json").redirectErrorStream(true).start();
            tunnel=process;
        }
        status="Подключаем HTTPS-приложение…";TelegramNotify.refreshPanel();
        var timeout=CompletableFuture.delayedExecutor(30,TimeUnit.SECONDS);
        timeout.execute(()->{if(active&&epoch==generation&&url.isBlank()&&process.isAlive())process.destroy();});
        boolean connected=false;
        try(var lines=new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream(),StandardCharsets.UTF_8))){
            String line;while((line=lines.readLine())!=null&&active&&epoch==generation){
                String data=line;
                if(line.contains("Permission denied")||line.contains("REMOTE HOST IDENTIFICATION")||line.contains("Connection timed out"))com.envyworld.EnvyWorld.LOGGER.warn("Mini App SSH: {}",line);
                try{var event=JsonParser.parseString(line).getAsJsonObject();if(!event.has("type")||!event.get("type").getAsString().equals("v1.tcpip_forward.register.accepted"))continue;data=event.get("data").getAsString();}catch(Exception ignored){continue;}
                var match=java.util.regex.Pattern.compile("https://[a-z0-9-]+\\.(?:lhr\\.life|localhost\\.run)").matcher(data);
                while(match.find()){
                    String candidate=match.group();
                    if(candidate.equals("https://admin.localhost.run"))continue;
                    if(probe(candidate,generation)){
                        synchronized(RemoteMiniApp.class){if(!active||epoch!=generation)return true;url=candidate;}
                        connected=true;status="Приложение готово. Нажмите «Открыть приложение» в Telegram";TelegramNotify.refreshPanel();break;
                    }
                }
            }
        }finally{if(process.isAlive())process.destroy();}
        if(active&&epoch==generation){url="";status=connected?"HTTPS отключился. Закройте и откройте эфир для повтора":"Резервное подключение HTTPS…";TelegramNotify.refreshPanel();}
        return connected;
    }
    private static boolean probe(String candidate,long generation){
        try{
            var response=HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(5)).build().send(HttpRequest.newBuilder(URI.create(candidate+"/health")).timeout(Duration.ofSeconds(8)).GET().build(),HttpResponse.BodyHandlers.ofString());
            boolean ok=active&&epoch==generation&&response.statusCode()==200&&response.body().equals("envyworld-miniapp");
            if(!ok)EnvyWorld.LOGGER.warn("Mini App HTTPS health: HTTP {} for {}",response.statusCode(),candidate);
            return ok;
        }catch(Exception failure){EnvyWorld.LOGGER.warn("Mini App HTTPS health: {}",failure.toString());return false;}
    }
    private static String digest(byte[] bytes)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    public static synchronized void stop(){active=false;epoch++;url="";SESSIONS.clear();if(server!=null){server.stop(0);server=null;}if(tunnel!=null){tunnel.destroy();tunnel=null;}if(handlers!=null){handlers.shutdownNow();handlers=null;}status="Выключен";RemoteGame.stop();}
    private static void handle(HttpExchange x,String owner,String token){
        try{
            x.getResponseHeaders().set("Cache-Control","no-store");x.getResponseHeaders().set("Referrer-Policy","no-referrer");x.getResponseHeaders().set("X-Content-Type-Options","nosniff");
            String path=x.getRequestURI().getPath(),method=x.getRequestMethod();
            if(path.equals("/health")&&method.equals("GET")){reply(x,200,"text/plain", "envyworld-miniapp".getBytes(StandardCharsets.UTF_8));return;}
            if(path.equals("/")&&method.equals("GET")){try(var stream=RemoteMiniApp.class.getResourceAsStream("/assets/envyworld/remote/index.html")){reply(x,200,"text/html; charset=utf-8",stream.readAllBytes());}return;}
            if(path.equals("/auth")&&method.equals("POST")){
                String raw=new String(x.getRequestBody().readNBytes(16385),StandardCharsets.UTF_8);
                if(raw.length()>16384||!TelegramAuth.verify(raw,token,owner,System.currentTimeMillis()/1000)){reply(x,403,"text/plain","Откройте приложение из личного чата своего бота".getBytes(StandardCharsets.UTF_8));return;}
                if(SESSIONS.size()>16)SESSIONS.clear();String session=UUID.randomUUID()+"-"+UUID.randomUUID();SESSIONS.put(session,System.currentTimeMillis()+3600000);RemoteGame.start();json(x,new JsonPrimitive(session));return;
            }
            String bearer=Objects.toString(x.getRequestHeaders().getFirst("Authorization"),"").replaceFirst("^Bearer ","");Long expires=SESSIONS.get(bearer);
            if(!active||expires==null||expires<System.currentTimeMillis()){reply(x,403,"text/plain",new byte[0]);return;}
            if(path.equals("/stream")&&method.equals("GET")){
                if(streams.incrementAndGet()>2){streams.decrementAndGet();reply(x,429,"text/plain",new byte[0]);return;}
                try{
                    x.getResponseHeaders().set("Content-Type","text/event-stream");x.getResponseHeaders().set("Cache-Control","no-cache, no-transform");x.getResponseHeaders().set("X-Accel-Buffering","no");x.sendResponseHeaders(200,0);
                    var out=new java.io.DataOutputStream(x.getResponseBody());long after=-1,end=System.currentTimeMillis()+55000;
                    while(active&&System.currentTimeMillis()<end&&expires>System.currentTimeMillis()){
                        viewedAt=System.currentTimeMillis();var packet=RemoteGame.awaitFrame(after);
                        if(packet==null||packet.id()<=after)continue;
                        out.writeBytes("id: "+packet.id()+"\ndata: ");out.write(Base64.getEncoder().encode(packet.bytes()));out.writeBytes("\n\n");out.flush();after=packet.id();
                    }
                }finally{streams.decrementAndGet();}return;
            }
            if(path.equals("/frame")&&method.equals("GET")){
                viewedAt=System.currentTimeMillis();var frame=RemoteGame.packet();
                if(frame==null||Long.toString(frame.id()).equals(query(x,"after"))){reply(x,204,"image/jpeg",new byte[0]);return;}
                x.getResponseHeaders().set("X-Frame-Id",Long.toString(frame.id()));
                x.getResponseHeaders().set("X-Frame-At",Long.toString(frame.at()));
                reply(x,200,"image/jpeg",frame.bytes());return;
            }
            if(path.equals("/input")&&method.equals("POST")){
                String raw=new String(x.getRequestBody().readNBytes(8193),StandardCharsets.UTF_8);if(raw.length()>8192)throw new IllegalArgumentException();
                RemoteGame.inputEvent(JsonParser.parseString(raw).getAsJsonObject());json(x,new JsonPrimitive(true));return;
            }
            if(path.equals("/state")&&method.equals("GET")){var state=new Gson().toJsonTree(SessionState.snapshot()).getAsJsonObject();state.addProperty("capture",RemoteGame.status());state.addProperty("fps",RemoteGame.fps());state.addProperty("chatRevision",SessionChat.revision());state.addProperty("owner",TelegramNotify.ownerLabel());state.addProperty("menu",RemoteGame.menuOpen());state.addProperty("position",RemoteGame.position());json(x,state);return;}
            if(path.equals("/chat")&&method.equals("GET")){
                int page=Math.max(0,Integer.parseInt(query(x,"page").isBlank()?"0":query(x,"page")));var all=SessionChat.entries();int pages=Math.max(1,(all.size()+99)/100);page=Math.min(page,pages-1);
                var out=new JsonObject();out.addProperty("page",page);out.addProperty("pages",pages);out.addProperty("total",all.size());out.addProperty("revision",SessionChat.revision());
                out.add("rows",new Gson().toJsonTree(all.subList(page*100,Math.min(all.size(),page*100+100))));json(x,out);return;
            }
            if(path.equals("/send")&&method.equals("POST")){
                var body=JsonParser.parseString(new String(x.getRequestBody().readNBytes(4096),StandardCharsets.UTF_8)).getAsJsonObject();String text=body.get("text").getAsString();if(text.isBlank()||text.length()>256)throw new IllegalArgumentException();
                var sent=new CompletableFuture<String>();com.envyworld.client.TelegramGameChat.send(text,SessionState.destination(),sent::complete);json(x,new JsonPrimitive(sent.get(9,TimeUnit.SECONDS)));return;
            }
            if(path.equals("/skin")&&method.equals("GET")){String nick=query(x,"nick");if(!nick.matches("[A-Za-z0-9_]{1,16}"))throw new IllegalArgumentException();var out=new JsonObject();String image=com.envyworld.client.gui.KLauncherSkins.preview(nick).get(12,TimeUnit.SECONDS);if(image.isBlank()){var c=net.minecraft.client.Minecraft.getInstance();var texture=net.minecraft.client.resources.DefaultPlayerSkin.get(java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:"+nick).getBytes(StandardCharsets.UTF_8))).body().texturePath();try(var resource=c.getResourceManager().getResourceOrThrow(texture).open()){image="data:image/png;base64,"+Base64.getEncoder().encodeToString(resource.readAllBytes());}}out.addProperty("image",image);json(x,out);return;}
            if(path.equals("/check")&&method.equals("POST")){
                var data=JsonParser.parseString(new String(x.getRequestBody().readNBytes(1024),StandardCharsets.UTF_8)).getAsJsonObject();String nick=data.get("nick").getAsString(),command=data.get("command").getAsString();
                if(!nick.matches("[A-Za-z0-9_]{1,16}")||!Set.of("dupeip","iphistory").contains(command))throw new IllegalArgumentException();
                var result=new CompletableFuture<String>();net.minecraft.client.Minecraft.getInstance().execute(()->result.complete(com.envyworld.client.gui.ProfileChecks.request(nick,command)));json(x,new JsonPrimitive(result.get(2,TimeUnit.SECONDS)));return;
            }
            if(path.equals("/logs")&&method.equals("GET")){
                String query=query(x,"q");if(!query.matches("[A-Za-z0-9_]{0,16}"))throw new IllegalArgumentException();
                PunishLogClient.tickAll(query,"all");var logs=PunishLogClient.snapshot(query,"all");var result=new Gson().toJsonTree(logs).getAsJsonObject();
                for(var row:result.getAsJsonArray("rows")) {var r=row.getAsJsonObject();r.addProperty("server",ServerModes.label(r.get("server").getAsString()));}
                json(x,result);return;
            }
            if(path.equals("/profile")&&method.equals("GET")){String nick=query(x,"nick");if(!nick.matches("[A-Za-z0-9_]{1,16}"))throw new IllegalArgumentException();json(x,RemoteGame.profile(nick));return;}
            reply(x,404,"text/plain",new byte[0]);
        }catch(Exception invalid){try{reply(x,400,"text/plain","Некорректный запрос".getBytes(StandardCharsets.UTF_8));}catch(Exception ignored){}}finally{x.close();}
    }
    private static String query(HttpExchange x,String key){String raw=x.getRequestURI().getRawQuery();if(raw!=null)for(String pair:raw.split("&")){var p=pair.split("=",2);if(p[0].equals(key))return p.length==2?URLDecoder.decode(p[1],StandardCharsets.UTF_8):"";}return "";}
    private static void json(HttpExchange x,JsonElement object)throws Exception{reply(x,200,"application/json; charset=utf-8",object.toString().getBytes(StandardCharsets.UTF_8));}
    private static void reply(HttpExchange x,int code,String type,byte[] data)throws Exception{x.getResponseHeaders().set("Content-Type",type);x.sendResponseHeaders(code,code==204?-1:data.length);if(code!=204)x.getResponseBody().write(data);}
}
