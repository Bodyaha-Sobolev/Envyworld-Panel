package com.envyworld.client.gui;

import java.util.*;
import java.util.regex.*;

/** Numeric addresses only; no hostname lookup and no command syntax from server chat. */
public final class IpCheckPlan {
    private static final Pattern ADDRESS_TOKEN = Pattern.compile("(?<![A-Za-z0-9_.:%])(?:\\[([0-9A-Fa-f:.]+)\\]|([0-9A-Fa-f:.]+))(?![A-Za-z0-9_.:%])");
    private static final Pattern CHAT = Pattern.compile("(?:^|\\]\\s*)(?:<[A-Za-z0-9_]{1,16}>|[A-Za-z0-9_]{1,16}\\s*[:>])");
    private static final Pattern FORMAT = Pattern.compile("(?i)§[0-9a-fk-orx]");
    private static final Pattern GENERIC_HEADER = Pattern.compile("(?iu)^(?:\\[[^\\]]{0,32}\\]\\s*)?(?:история\\s+ip(?:[- ]адресов)?|ip(?:\\s+address)?\\s+history|iphistory|dupeip|аккаунты\\s+(?:по|с)\\s+ip)(?:\\s*\\([0-9]+\\))?\\s*[:：]?$");
    public enum Response { IGNORE, HEADER, LINE, DENIED }
    private IpCheckPlan(){}
    public static List<String> addresses(List<String> lines){
        var result=new LinkedHashSet<String>();
        for (String line : lines) {
            if (line == null) continue;
            var matcher = ADDRESS_TOKEN.matcher(line);
            while (matcher.find()) {
                String candidate = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
                if (candidate.indexOf('.') >= 0 && candidate.indexOf(':') == candidate.lastIndexOf(':') && candidate.indexOf(':') > 0) {
                    int colon = candidate.lastIndexOf(':');
                    String port = candidate.substring(colon + 1);
                    if (port.matches("[0-9]{1,5}") && Integer.parseInt(port) <= 65535) candidate = candidate.substring(0, colon);
                }
                if (valid(candidate)) result.add(candidate.toLowerCase(Locale.ROOT));
            }
        }
        return List.copyOf(result);
    }
    public static boolean valid(String address){
        if (address == null || address.isEmpty() || address.length() > 45) return false;
        if (address.indexOf(':') < 0) return ipv4(address);
        if (!address.matches("[0-9a-fA-F:.]+") || address.indexOf(':') == address.lastIndexOf(':')) return false;
        int compression = address.indexOf("::");
        if (compression >= 0 && address.indexOf("::", compression + 2) >= 0) return false;
        if (compression < 0 && (address.startsWith(":") || address.endsWith(":"))) return false;
        if (address.contains(":::")) return false;
        String[] parts = address.split(":", -1);
        int groups = 0;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) continue;
            if (part.contains(".")) {
                if (i != parts.length - 1 || !ipv4(part)) return false;
                groups += 2;
            } else {
                if (!part.matches("[0-9a-fA-F]{1,4}")) return false;
                groups++;
            }
        }
        return compression >= 0 ? groups < 8 : groups == 8;
    }
    private static boolean ipv4(String address) {
        if (!address.matches("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}")) return false;
        for (String octet : address.split("\\.")) if (Integer.parseInt(octet) > 255) return false;
        return true;
    }
    /** A recognised command header starts the stream; unrelated chat cannot enqueue addresses. */
    public static Response response(String raw, String subject, String command, boolean headerSeen) {
        if (raw == null || subject == null) return Response.IGNORE;
        String text = FORMAT.matcher(raw).replaceAll("").trim();
        // CMI prints history as "- [dd.MM.yyyy] Nick: address". Its colon is
        // a field separator, not a player's chat message.
        if(headerSeen&&command.equals("iphistory")&&text.matches("(?iu)^[-•*]?\\s*\\[[0-9]{2}\\.[0-9]{2}\\.[0-9]{4}\\]\\s*"+Pattern.quote(subject)+"\\s*:\\s*[^\\r\\n]+$")
                &&addresses(List.of(text)).size()==1)return Response.LINE;
        boolean labelled = text.matches("(?iu)^(?:iphistory|dupeip|ip|accounts)\\s*:.*");
        boolean numericStart = valid(text.split("\\s+",2)[0]);
        if (text.isEmpty() || text.contains("»") || text.contains("→") || text.contains("➥") || text.contains(" -> ") || !labelled && !numericStart && CHAT.matcher(text).find()) return Response.IGNORE;
        String lower = text.toLowerCase(Locale.ROOT);
        boolean denied = lower.contains("нет прав") || lower.contains("недостаточно прав") || lower.contains("permission")
                || lower.contains("permission denied") || lower.contains("unknown command") || lower.contains("неизвестная команд")
                || lower.contains("слишком часто") || lower.contains("too many") || lower.contains("подождите") || lower.contains("please wait");
        if (denied) return Response.DENIED;
        if(headerSeen&&command.equals("dupeip")&&text.matches("(?iu)^(?:[-•*]\\s*|[0-9]{1,5}[.)]\\s*)?[A-Za-z0-9_]{1,16}(?:\\s*[,;|].*|\\s+\\([^\\r\\n]*\\)|\\s+[-–—].*)?$"))return Response.LINE;
        boolean mentionsSubject = Pattern.compile("(?iu)(?<![\\p{L}\\p{N}_])" + Pattern.quote(subject) + "(?![\\p{L}\\p{N}_])").matcher(text).find();
        if(command.equals("dupeip")&&mentionsSubject&&lower.startsWith("сканирование "))return Response.HEADER;
        boolean terminology = lower.contains("iphistory") || lower.contains("dupeip") || lower.contains("ip")
                || lower.contains("айпи") || lower.contains("адрес") || lower.contains("аккаунт") || lower.contains("account");
        boolean empty = lower.contains("не найден") || lower.contains("нет данных")
                || lower.contains("no history") || lower.contains("not found") || lower.contains("no accounts");
        String headerText=text.replaceFirst("^\\[[^\\]]{0,32}\\]\\s*","");
        boolean responseStart=headerText.matches("(?iuU)^(?:/?(?:iphistory|dupeip)|история|аккаунты|адреса|ip|айпи|accounts|history|alts|связанные)\\b.*")
                || Pattern.compile("(?iu)^"+Pattern.quote(subject)+"(?![\\p{L}\\p{N}_]).*").matcher(headerText).matches();
        if (mentionsSubject && (terminology || empty) && (responseStart || empty) || GENERIC_HEADER.matcher(text).matches()) return Response.HEADER;
        if (!headerSeen) return Response.IGNORE;
        if (empty) return Response.LINE;
        if (!addresses(List.of(text)).isEmpty()) {
            String row=text.replaceFirst("^(?:[-•*]\\s*|[0-9]{1,5}[.)]\\s*)","");
            // Address rows start with the address, a date, an index or a known field label.
            // Arbitrary prose mentioning an IP after the header is still unrelated chat.
            boolean addressRow=row.matches("(?iu)^(?:\\[?[0-9a-f:.]+\\]?(?::[0-9]+)?(?:[\\s,;|].*)?|[0-9]{1,4}[-./][0-9].*|(?:ip(?:[- ]?адрес)?|адрес|последний\\s+ip|first\\s+ip|last\\s+ip)\\s*[:：].*)$");
            if(addressRow)return Response.LINE;
        }
        if (command.equals("dupeip") && text.matches("(?iu)^(?:[-•*]\\s*|[0-9]{1,5}[.)]\\s*)?[A-Za-z0-9_]{1,16}(?:\\s*[,;|].*|\\s+\\([^\\r\\n]*\\)|\\s+[-–—].*)?$")) return Response.LINE;
        return Response.IGNORE;
    }
    public static long delay(int count){return count>10?500:250;}
}
