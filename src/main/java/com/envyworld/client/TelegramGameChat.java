package com.envyworld.client;

import com.envyworld.logs.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;

/** One game-thread dispatch path for Telegram, with a terminal result even if the game stalls. */
public final class TelegramGameChat {
    private TelegramGameChat() {}
    public static void send(String text,String destination,Consumer<String> result) {
        send(text,destination,()->true,result);
    }
    public static void send(String text,String destination,java.util.function.BooleanSupplier authorized,Consumer<String> result) {
        var done=new AtomicBoolean();
        CompletableFuture.delayedExecutor(8,TimeUnit.SECONDS).execute(()->{
            if(done.compareAndSet(false,true))result.accept("Не отправлено: Minecraft не обработал запрос. Вернитесь в игру и повторите отправку");
        });
        Minecraft.getInstance().execute(()->{
            if(!done.compareAndSet(false,true))return;
            var client=Minecraft.getInstance();
            if(!authorized.getAsBoolean()){result.accept("Не отправлено: Telegram отключён");return;}
            if(client.player==null||client.getConnection()==null||!SessionState.destinationNow(client).equals(destination)){
                result.accept("Не отправлено: подключение изменилось. Отправьте текст боту ещё раз");return;
            }
            String value=text.strip();
            if(value.isEmpty()||value.length()>256||value.contains("\n")||value.contains("\r")||!ChatFilter.redact(value).equals(value)){
                result.accept("Не отправлено: нужна одна строка до 256 символов без паролей");return;
            }
            try {
                if(value.startsWith("/"))client.getConnection().sendCommand(value.substring(1));
                else client.getConnection().sendChat(value);
                result.accept("Передано серверу Minecraft:\n"+value+"\nОтвет сервера смотрите в разделе «Чат»");
            }catch(RuntimeException failed){result.accept("Не отправлено: ошибка чата Minecraft. Проверьте подключение");}
        });
    }
}
