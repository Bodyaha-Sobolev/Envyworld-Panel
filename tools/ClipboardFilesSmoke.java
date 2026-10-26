import com.sun.jna.*;
import com.sun.jna.win32.StdCallLibrary;
public class ClipboardFilesSmoke {
    public interface Kernel extends StdCallLibrary {Pointer GlobalAlloc(int flags,long size);Pointer GlobalLock(Pointer p);boolean GlobalUnlock(Pointer p);Pointer GlobalFree(Pointer p);}
    public static void main(String[] args)throws Exception{
        if(!System.getProperty("os.name").startsWith("Windows"))return;
        Kernel k=Native.load("kernel32",Kernel.class);String file="C:\\Users\\Проверка\\предметы.json";
        Pointer handle=k.GlobalAlloc(2,20+(file.length()+2)*2);if(handle==null)throw new AssertionError("GlobalAlloc");
        try{Pointer data=k.GlobalLock(handle);data.clear(20+(file.length()+2)*2);data.setInt(0,20);data.setInt(16,1);data.setWideString(20,file);k.GlobalUnlock(handle);
            var method=com.envyworld.client.ClipboardFiles.class.getDeclaredMethod("pathFromDrop",Pointer.class);method.setAccessible(true);
            if(!java.nio.file.Path.of(file).equals(method.invoke(null,handle)))throw new AssertionError("Unicode file clipboard path");
            com.envyworld.client.ClipboardFiles.first();
            System.out.println("PASS: Windows Explorer clipboard file decoding works in headless mode; no clipboard mutation");
        }finally{k.GlobalFree(handle);}
    }
}
