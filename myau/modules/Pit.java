package myau.modules;

import java.util.*;
import myau.modules.pit.PitAutoMath;
import myau.modules.pit.PitHud;

public final class Pit {
    public enum Alerts { OFF, JOIN, LEAVE, BOTH }
    public enum Horizontal { LEFT, MIDDLE, RIGHT }
    public enum Vertical { TOP, MIDDLE, BOTTOM }
    public enum HideFar { NONE, CLOSE, EXTENDED, SMART }
    public enum HideSpawn { NONE, VISUALS, ENTITY }
    public enum ItemKind { OTHER, IRON_ARMOR, IRON_SWORD, DIAMOND_SWORD }
    public static final class Settings {
        public boolean pitOnly=true;
        public boolean playerList,playerListFriends=true,playerListEnemies=true,playerListDarks=true,
                playerListVenoms=true,playerListRegularity=true,playerListBountyHunters=true,playerListNicked=true,playerListEnchants;
        public int playerListMaxPlayers=10;
        public Alerts playerListAlerts=Alerts.OFF;
        public Horizontal playerListPositionX=Horizontal.LEFT;
        public Vertical playerListPositionY=Vertical.MIDDLE;
        public int playerListOffsetX=2,playerListOffsetY;
        public float playerListScale=1;
        public boolean denicker=true,denickerNotifications=true,autoMath,chatFilter,chatFilterStreaks=true,chatFilterBounty;
        public boolean eventList;
        public Horizontal eventListPositionX=Horizontal.RIGHT;
        public Vertical eventListPositionY=Vertical.BOTTOM;
        public int eventListOffsetX=2,eventListOffsetY=2;
        public float eventListScale=1;
        public boolean inventoryCleaner,throwIronArmor=true,throwIronSwords=true;
        public HideFar hideFarPlayer=HideFar.NONE;
        public HideSpawn hidePitSpawn=HideSpawn.NONE;
        public boolean showInSpawn=true;
        public void validate(){
            Objects.requireNonNull(playerListAlerts,"alerts");Objects.requireNonNull(playerListPositionX,"playerListPositionX");
            Objects.requireNonNull(playerListPositionY,"playerListPositionY");Objects.requireNonNull(eventListPositionX,"eventListPositionX");
            Objects.requireNonNull(eventListPositionY,"eventListPositionY");Objects.requireNonNull(hideFarPlayer,"hideFarPlayer");Objects.requireNonNull(hidePitSpawn,"hidePitSpawn");
            if(playerListMaxPlayers<1||playerListMaxPlayers>80)throw new IllegalArgumentException("max players: 1..80");
            for(int offset:new int[]{playerListOffsetX,playerListOffsetY,eventListOffsetX,eventListOffsetY})
                if(offset<0||offset>255)throw new IllegalArgumentException("offset: 0..255");
            for(float scale:new float[]{playerListScale,eventListScale})
                if(!Float.isFinite(scale)||scale<.5F||scale>1.5F)throw new IllegalArgumentException("scale: .5..1.5");
        }
    }
    public static final class PlayerInfo {
        public final Object entity;
        public final UUID id;
        public final String name,enchants;
        public final int categories;
        public final double distance;
        public final boolean spawn;
        public PlayerInfo(Object entity,UUID id,String name,String enchants,int categories,double distance,boolean spawn){
            this.entity=Objects.requireNonNull(entity,"entity");this.id=Objects.requireNonNull(id,"id");
            this.name=Objects.requireNonNull(name,"name");this.enchants=Objects.requireNonNull(enchants,"enchants");
            this.categories=categories;this.distance=distance;this.spawn=spawn;
        }
    }
    public static final class Event {
        public final String name;public final long startMillis;public final boolean major;
        public Event(String name,long startMillis,boolean major){this.name=Objects.requireNonNull(name,"name");this.startMillis=startMillis;this.major=major;}
    }
    public static final class InventoryItem {
        public final ItemKind kind;public final boolean named,enchanted;
        public InventoryItem(ItemKind kind,boolean named,boolean enchanted){this.kind=Objects.requireNonNull(kind,"kind");this.named=named;this.enchanted=enchanted;}
    }
    public interface Host {
        boolean ready();
        Object worldIdentity();
        int playerTick();
        String localName();
        boolean inPit();
        boolean localInSpawn(boolean pitOnly);
        boolean spawnHeight(double y,boolean pitOnly);
        long nowMillis();
        List<PlayerInfo> players(boolean pitOnly);
        void updateDenicker(boolean enabled,boolean notifications,boolean pitOnly);
        void updateEventSchedule(boolean enabled,boolean pitOnly);
        List<Event> events();
        boolean eventsLoading();
        boolean hypixel();
        void message(String text);
        void sendChat(String text);
        boolean inventoryScreenOpen();
        boolean survivalOrAdventure();
        InventoryItem inventoryItem(int index);
        void dropStack(int containerSlot);
        boolean hudAllowed();
        int screenWidth();
        int screenHeight();
        int fontHeight();
        int textWidth(String text);
        boolean textShadow();
        void drawHud(PitHud.Panel panel);
    }
    private static final String[] CATEGORIES={"Friends","Enemies","Darks","Venoms","Regularity","Bounty Hunters","Nicked"};
    public final Settings settings=new Settings();
    private final Host host;
    private final PitAutoMath math=new PitAutoMath();
    private boolean enabled,listBaseline,inventoryWasOpen;
    private long nextListUpdate;
    private int cleanDelay,openDelay,smartTick=-1;
    private Object smartWorld;
    private final Set<Object> smartVisible=new HashSet<>();
    private final Map<UUID,String> previousPlayers=new LinkedHashMap<>();
    private List<String> playerRows=Collections.emptyList();

    public Pit(Host host){this.host=Objects.requireNonNull(host,"host");}
    public boolean isEnabled(){return enabled;}
    public void setEnabled(boolean value){settings.validate();if(enabled==value)return;enabled=value;if(!value)reset();}
    public boolean allowedServer(){return !settings.pitOnly||host.inPit();}
    public boolean categoryEnabled(int category){
        switch(category){case 0:return settings.playerListFriends;case 1:return settings.playerListEnemies;
            case 2:return settings.playerListDarks;case 3:return settings.playerListVenoms;case 4:return settings.playerListRegularity;
            case 5:return settings.playerListBountyHunters;default:return settings.playerListNicked;}
    }
    public void onTick(){
        settings.validate();
        host.updateDenicker(enabled&&settings.denicker,settings.denickerNotifications,settings.pitOnly);
        host.updateEventSchedule(enabled&&settings.eventList,settings.pitOnly);
        boolean allowed=enabled&&host.ready()&&allowedServer();
        math.tick(allowed&&settings.autoMath,host.nowMillis(),host::sendChat);
        if(!allowed||!settings.playerList){clearList();return;}
        long now=host.nowMillis();
        if(now<nextListUpdate)return;
        nextListUpdate=now+250;
        rebuildPlayerList(host.players(settings.pitOnly));
    }
    private void rebuildPlayerList(List<PlayerInfo> players){
        Map<UUID,String> current=new LinkedHashMap<>();
        List<String> rows=new ArrayList<>();
        for(int category=0;category<CATEGORIES.length;category++){
            if(!categoryEnabled(category))continue;
            int count=0;
            for(PlayerInfo info:players){
                if((info.categories&(1<<category))==0)continue;
                current.put(info.id,info.name);
                if(count>=settings.playerListMaxPlayers)continue;
                if(count++==0)rows.add("\u00a7b"+CATEGORIES[category]);
                rows.add(info.name+(settings.playerListEnchants&&!info.enchants.isEmpty()?" \u00a77["+info.enchants+"]":""));
            }
        }
        if(listBaseline){
            if(settings.playerListAlerts==Alerts.JOIN||settings.playerListAlerts==Alerts.BOTH)
                for(Map.Entry<UUID,String> e:current.entrySet())if(!previousPlayers.containsKey(e.getKey()))host.message(e.getValue()+" joined.");
            if(settings.playerListAlerts==Alerts.LEAVE||settings.playerListAlerts==Alerts.BOTH)
                for(Map.Entry<UUID,String> e:previousPlayers.entrySet())if(!current.containsKey(e.getKey()))host.message(e.getValue()+" left.");
        }
        listBaseline=true;previousPlayers.clear();previousPlayers.putAll(current);
        playerRows=Collections.unmodifiableList(rows);
    }
    public boolean onChat(String formatted,String unformatted,int type,boolean cancelled){
        settings.validate();
        if(!enabled||cancelled)return false;
        if(settings.autoMath&&type!=2&&host.ready()&&allowedServer())math.receive(unformatted,host.nowMillis());
        if(!settings.chatFilter||!allowedServer())return false;
        String text=formatted;
        if(text.startsWith("\u00a7r")&&text.length()>4)text=text.substring(2);
        if(host.ready()&&text.contains(host.localName()))return false;
        if(settings.chatFilterStreaks&&text.startsWith("\u00a7c\u00a7l"))return text.substring(4).contains("STREAK");
        if(settings.chatFilterBounty&&text.startsWith("\u00a76\u00a7l"))return text.substring(4).contains("BOUNTY");
        return false;
    }
    public void onRotation(){
        settings.validate();
        if(cleanDelay>0)cleanDelay--;if(openDelay>0)openDelay--;
        if(!host.ready()||!host.inventoryScreenOpen()){inventoryWasOpen=false;return;}
        if(!inventoryWasOpen){inventoryWasOpen=true;openDelay=2;}
        if(!enabled||!settings.inventoryCleaner||openDelay>0||cleanDelay>0||!host.survivalOrAdventure())return;
        boolean diamond=false;
        if(settings.throwIronSwords)for(int i=0;i<9;i++){
            InventoryItem item=host.inventoryItem(i);if(item!=null&&item.kind==ItemKind.DIAMOND_SWORD){diamond=true;break;}
        }
        for(int i=0;i<36;i++){
            InventoryItem item=host.inventoryItem(i);if(item==null)continue;
            boolean drop=settings.throwIronArmor&&item.kind==ItemKind.IRON_ARMOR
                    ||settings.throwIronSwords&&diamond&&item.kind==ItemKind.IRON_SWORD&&!item.named&&!item.enchanted;
            if(!drop)continue;
            host.dropStack(containerSlot(i));cleanDelay=2;return;
        }
    }
    public static int containerSlot(int index){return index>=36?44-index:index>8?index:36+index;}
    public boolean hideEntity(PlayerInfo entity){return entity!=null&&(hideSpawn(entity.spawn,true)||hideFar(entity));}
    public boolean hideVisual(PlayerInfo entity){return entity!=null&&(hideSpawn(entity.spawn,false)||hideFar(entity));}
    public boolean hideSpawn(boolean entityInSpawn,boolean entityHook){
        if(!enabled||!host.ready()||settings.hidePitSpawn==HideSpawn.NONE||entityHook&&settings.hidePitSpawn!=HideSpawn.ENTITY)return false;
        return entityInSpawn&&(!settings.showInSpawn||!host.localInSpawn(settings.pitOnly));
    }
    public boolean hideSpawnHeight(double y){
        return enabled&&host.ready()&&settings.hidePitSpawn==HideSpawn.ENTITY&&host.spawnHeight(y,settings.pitOnly)
                &&(!settings.showInSpawn||!host.localInSpawn(settings.pitOnly));
    }
    private boolean hideFar(PlayerInfo entity){
        if(!enabled||!host.ready()||!allowedServer())return false;
        switch(settings.hideFarPlayer){
            case NONE:return false;
            case CLOSE:return entity.distance>24;
            case EXTENDED:return entity.distance>64;
            default:
                if(entity.spawn)return false;
                if(smartWorld!=host.worldIdentity()||smartTick!=host.playerTick()){
                    smartWorld=host.worldIdentity();smartTick=host.playerTick();smartVisible.clear();
                    List<PlayerInfo> farther=new ArrayList<>();
                    for(PlayerInfo p:host.players(settings.pitOnly)){
                        if(p.spawn)continue;
                        if(p.distance<=24)smartVisible.add(p.entity);else if(p.distance<=64)farther.add(p);
                    }
                    farther.sort(Comparator.comparingDouble(p->p.distance));
                    int capacity=24-smartVisible.size();
                    for(int i=0;i<farther.size()&&i<capacity;i++)smartVisible.add(farther.get(i).entity);
                }
                return !smartVisible.contains(entity.entity);
        }
    }
    public void onRender(){
        settings.validate();
        if(!enabled||!host.ready()||!allowedServer()||!host.hudAllowed())return;
        if(settings.playerList&&!playerRows.isEmpty())host.drawHud(PitHud.layout(playerRows,settings.playerListPositionX,
                settings.playerListPositionY,settings.playerListOffsetX,settings.playerListOffsetY,settings.playerListScale,host,true));
        if(settings.eventList){
            List<String> rows=new ArrayList<>();long now=host.nowMillis();
            int max=Math.min(3,Math.max(1,(int)((host.screenHeight()/settings.eventListScale-4)/host.fontHeight())));
            for(Event event:host.events()){
                if(event.startMillis<=now)continue;
                long seconds=(event.startMillis-now+999)/1000;
                String time=String.format(Locale.ROOT,"%02d:%02d",seconds/60,seconds%60);
                String name=(event.major?"[M] ":"")+event.name;
                rows.add(settings.eventListPositionX==Horizontal.RIGHT?name+" \u00a77"+time:"\u00a77"+time+" \u00a7r"+name);
                if(rows.size()==max)break;
            }
            if(!rows.isEmpty()&&!host.hypixel())rows.add("\u00a77Hypixel times, may differ here");
            if(rows.isEmpty())rows.add(host.eventsLoading()?"\u00a77Loading events...":"\u00a77Schedule unavailable");
            host.drawHud(PitHud.layout(rows,settings.eventListPositionX,settings.eventListPositionY,
                    settings.eventListOffsetX,settings.eventListOffsetY,settings.eventListScale,host,false));
        }
    }
    private void clearList(){playerRows=Collections.emptyList();previousPlayers.clear();listBaseline=false;nextListUpdate=0;}
    private void reset(){
        clearList();math.reset();smartVisible.clear();smartWorld=null;smartTick=-1;
        host.updateDenicker(false,settings.denickerNotifications,settings.pitOnly);host.updateEventSchedule(false,settings.pitOnly);
    }
    public void onWorldChange(){reset();}
}
