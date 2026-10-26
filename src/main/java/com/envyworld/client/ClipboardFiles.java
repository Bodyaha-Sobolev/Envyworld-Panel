package com.envyworld.client;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.win32.StdCallLibrary;
import java.nio.file.Path;

/** Read Explorer's copied files without AWT (Minecraft runs in headless AWT mode). */
public final class ClipboardFiles {
    public interface User32 extends StdCallLibrary {
        boolean OpenClipboard(Pointer owner);
        boolean CloseClipboard();
        boolean IsClipboardFormatAvailable(int format);
        Pointer GetClipboardData(int format);
        boolean EmptyClipboard();
        Pointer SetClipboardData(int format,Pointer data);
    }
    public interface Kernel32 extends StdCallLibrary{Pointer GlobalAlloc(int flags,long size);Pointer GlobalLock(Pointer data);boolean GlobalUnlock(Pointer data);Pointer GlobalFree(Pointer data);}
    public interface Shell32 extends StdCallLibrary { int DragQueryFileW(Pointer drop,int index,char[] buffer,int size); }
    private ClipboardFiles(){}
    public static void copy(Path file){
        if(!System.getProperty("os.name","").startsWith("Windows")){net.minecraft.client.Minecraft.getInstance().keyboardHandler.setClipboard(file.toAbsolutePath().toString());return;}
        var user=Native.load("user32",User32.class);var kernel=Native.load("kernel32",Kernel32.class);
        String name=file.toAbsolutePath().toString();long bytes=20+(name.length()+2L)*2;
        Pointer handle=kernel.GlobalAlloc(2,bytes);if(handle==null)throw new IllegalStateException("Clipboard allocation");boolean transferred=false,opened=false;
        try{Pointer memory=kernel.GlobalLock(handle);if(memory==null)throw new IllegalStateException("Clipboard lock");memory.clear(bytes);memory.setInt(0,20);memory.setInt(16,1);memory.setWideString(20,name);kernel.GlobalUnlock(handle);
            long hwnd=org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window(net.minecraft.client.Minecraft.getInstance().getWindow().handle());
            if(!(opened=user.OpenClipboard(Pointer.createConstant(hwnd)))||!user.EmptyClipboard()||user.SetClipboardData(15,handle)==null)throw new IllegalStateException("Clipboard busy");transferred=true;
        }finally{if(opened)user.CloseClipboard();if(!transferred)kernel.GlobalFree(handle);}
    }
    public static Path first(){
        if(!System.getProperty("os.name","").startsWith("Windows"))return null;
        User32 user=Native.load("user32",User32.class);
        if(!user.IsClipboardFormatAvailable(15)||!user.OpenClipboard(null))return null;
        try{
            return pathFromDrop(user.GetClipboardData(15));
        }finally{user.CloseClipboard();}
    }
    private static Path pathFromDrop(Pointer drop){
        if(drop==null)return null;Shell32 shell=Native.load("shell32",Shell32.class);
        int length=shell.DragQueryFileW(drop,0,null,0);if(length<=0||length>32767)return null;
        char[] buffer=new char[length+1];if(shell.DragQueryFileW(drop,0,buffer,buffer.length)<=0)return null;
        return Path.of(Native.toString(buffer));
    }
}
