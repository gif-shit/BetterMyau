import java.util.*;
import myau.modules.*;
import myau.modules.pit.*;
import myau.modules.swordswap.SwordSwapMinecraftAccess;

public final class PitSwapChecks {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static void near(double a,double b,String why){check(Math.abs(a-b)<1e-6,why+" "+a+" vs "+b);}
    static void invalid(Runnable action,String why){boolean failed=false;try{action.run();}catch(IllegalArgumentException|NullPointerException e){failed=true;}check(failed,why);}
    static void worker(Runnable action){Thread thread=new Thread(action);thread.start();try{thread.join(5000);}catch(InterruptedException e){throw new AssertionError(e);}if(thread.isAlive())throw new AssertionError("fixture worker did not finish");}
    static final class SwordHost implements SwordSwap.Host {
        boolean ready=true,screen,pit=true,aura=true,key,swing,healing,eating;
        Object player=new Object(),world=new Object();Object[] stacks={"A","food","B","C",null,null,null,null,null};
        int slot,syncs,shows,shownSlot=-1;Object shown;
        public boolean ready(){return ready;}public Object playerIdentity(){return player;}public boolean screenOpen(){return screen;}
        public Object worldIdentity(){return world;}
        public boolean inPit(){return pit;}public boolean auraEnabledWithTarget(){return aura;}public boolean attackKeyDown(){return key;}
        public boolean swinging(){return swing;}public boolean autoHealing(){return healing;}public boolean consuming(){return eating;}
        public int currentSlot(){return slot;}public Object stack(int n){return stacks[n];}public boolean allowedDestination(Object s){return !s.equals("food");}
        public void selectSlot(int n){slot=n;}public void syncHeldItem(){syncs++;}public void showItem(Object s,int n){shown=s;shownSlot=n;shows++;}
    }
    static class Renderer {private Object itemToRender;private int equippedItemSlot;private float equippedProgress,prevEquippedProgress;}
    static final class InheritedRenderer extends Renderer{}
    static final class SrgRenderer {private Object field_78453_b;private int field_78450_g;private float field_78454_c,field_78451_d;}
    static class Controller {int calls;private void syncCurrentPlayItem(){calls++;}}
    static final class InheritedController extends Controller{}
    static final class SrgController {int calls;private void func_78750_j(){calls++;}}
    static final class ThrowingController {private void syncCurrentPlayItem(){throw new IllegalArgumentException("sync failure");}}
    static void swordChecks(){
        int start=checks;SwordHost h=new SwordHost();SwordSwap m=new SwordSwap(h);
        check(m.settings.pitOnly&&m.settings.killauraOnly&&m.settings.slot1==1&&m.settings.slot2==3&&m.settings.spoof==SwordSwap.Spoof.NONE,"current five defaults");
        m.onTick();check(h.slot==0&&h.syncs==0,"disabled");m.setEnabled(true);m.onTick();check(h.slot==2&&h.syncs==1,"first swap");
        m.onTick();check(h.slot==0&&h.syncs==2,"second swap alternates");
        m.settings.spoof=SwordSwap.Spoof.FULL;m.onTick();check(h.slot==2&&h.shown.equals("A")&&h.shownSlot==2,"full shows original item with selected slot");
        m.onTick();check(h.slot==0&&h.shown.equals("A"),"full visual survives alternation");
        int shows=h.shows;m.onPostTick();check(h.shows==shows+1,"post tick reasserts visual");
        m.settings.spoof=SwordSwap.Spoof.ANIMATIONS;m.onTick();check(h.shown.equals("B"),"animations shows actual destination");
        h.aura=false;m.onTick();m.onPostTick();check(h.slot==0,"no aura target restores original");
        h.aura=true;h.screen=true;m.onTick();check(h.slot==0,"screen gate");h.screen=false;h.pit=false;m.onTick();check(h.slot==0,"pit-only gate");
        h.pit=true;h.healing=true;m.onTick();check(h.slot==0,"autoheal owns slot");h.healing=false;h.eating=true;m.onTick();check(h.slot==0,"consumption gate");h.eating=false;
        m.settings.killauraOnly=false;h.aura=false;m.onTick();check(h.slot==0,"manual no input gate");
        h.swing=true;m.onTick();check(h.slot==2,"manual swing allows swap");h.swing=false;h.key=true;m.onTick();check(h.slot==0,"manual key allows swap");
        m.settings.slot2=2;m.onTick();check(h.slot==0,"disallowed destination category");m.settings.slot2=5;m.onTick();check(h.slot==0,"null destination");
        m.settings.slot2=1;m.onTick();check(h.slot==0,"identical slots");m.settings.slot2=3;h.slot=3;m.onTick();m.onPostTick();check(h.slot==3,"manual unrelated slot preserved");
        h.slot=0;m.onTick();h.healing=true;m.setEnabled(false);check(h.slot==2,"autoheal defers restoration");h.healing=false;m.onPostTick();check(h.slot==0,"deferred restore after ownership release");
        m.setEnabled(true);m.onTick();h.player=new Object();m.setEnabled(false);check(h.slot==2,"never restores old player's slot into new player");
        m.setEnabled(true);h.slot=0;m.onTick();m.onWorldChange();m.setEnabled(false);check(h.slot==2,"world reset discards restore state");
        m.settings.slot1=0;invalid(()->m.settings.validate(),"slot lower bound");m.settings.slot1=10;invalid(()->m.settings.validate(),"slot upper bound");m.settings.slot1=1;
        m.settings.spoof=null;invalid(()->m.settings.validate(),"null spoof");
        Renderer renderer=new InheritedRenderer();Object visual=new Object();SwordSwapMinecraftAccess.show(renderer,visual,3);
        check(renderer.itemToRender==visual&&renderer.equippedItemSlot==3&&renderer.equippedProgress==1&&renderer.prevEquippedProgress==1,"real reflection renderer update with inherited MCP fields");
        SwordSwapMinecraftAccess.show(renderer,null,2);check(renderer.itemToRender==null&&renderer.equippedItemSlot==2,"renderer cache retains live values");
        SrgRenderer srgRenderer=new SrgRenderer();SwordSwapMinecraftAccess.show(srgRenderer,visual,4);
        check(srgRenderer.field_78453_b==visual&&srgRenderer.field_78450_g==4&&srgRenderer.field_78454_c==1&&srgRenderer.field_78451_d==1,"SRG renderer fields");
        Controller controller=new InheritedController();SwordSwapMinecraftAccess.sync(controller);SwordSwapMinecraftAccess.sync(controller);check(controller.calls==2,"inherited MCP sync and cached method");
        SrgController srgController=new SrgController();SwordSwapMinecraftAccess.sync(srgController);check(srgController.calls==1,"SRG sync");
        invalid(()->SwordSwapMinecraftAccess.sync(new ThrowingController()),"propagates real sync cause");
        boolean failed=false;try{SwordSwapMinecraftAccess.show(new Object(),visual,1);}catch(IllegalStateException e){failed=true;}check(failed,"missing mappings fail visibly");
        SwordHost missingController=new SwordHost();SwordSwap partial=new SwordSwap(missingController);partial.setEnabled(true);partial.onTick();missingController.ready=false;
        partial.setEnabled(false);check(missingController.slot==0,"missing controller does not prevent local restoration");
        System.out.println("SwordSwap="+(checks-start));
    }

    static final class PantHost implements PantSwap.Host {
        Object world=new Object(),player=new Object(),screen;Object cursor;
        final Object[] slots=new Object[45];boolean ready=true,container=true,other,survival=true,equip=true,observe=true,throwClick;
        long clock;short action;int clicks,closes,opens;String message;PantSwap module;
        PantHost(){slots[7]="P-old";slots[36]="P-new";slots[20]="stone";}
        public boolean ready(){return ready;}public Object worldIdentity(){return world;}public Object playerIdentity(){return player;}
        public boolean playerContainerOpen(){return container;}public boolean inventoryScreenOpen(){return screen!=null&&!other;}
        public boolean otherScreenOpen(){return other;}public boolean survivalOrAdventure(){return survival;}public Object screenIdentity(){return screen;}
        public void openInventory(){screen=new Object();opens++;}public void closeInventory(){screen=null;closes++;}
        public Object stack(int n){return slots[n];}public Object cursor(){return cursor;}public Object copy(Object s){return s==null?null:new String(s.toString());}
        public boolean equal(Object a,Object b){return Objects.equals(a,b);}public boolean leggings(Object s){return s!=null&&s.toString().startsWith("P-");}
        public boolean canEquipLeggings(Object s){return equip&&leggings(s);}public long nowMillis(){return clock;}
        public void click(int n){if(throwClick)throw new IllegalStateException("fixture failure");clicks++;Object a=slots[n];slots[n]=cursor;cursor=a;action++;if(observe)module.onPacketSend(0,action);}
        public void message(String text){message=text;}
        PantSwap create(){module=new PantSwap(this);return module;}
        void ack(){module.onPacketReceive(0,action,true,false);}
    }
    static void pantChecks(){
        int start=checks;PantHost h=new PantHost();PantSwap m=h.create();check(m.settings.delay==100&&!m.settings.inventory,"pant defaults");
        m.setEnabled(true);check(m.isEnabled()&&h.opens==1,"opens owned inventory");m.onTick();check(h.clicks==1&&h.cursor.equals("P-new")&&m.pendingTransaction()==1,"first pickup and observed transaction");
        m.onPacketReceive(1,1,true,false);m.onPacketReceive(0,2,true,false);m.onPacketReceive(0,1,true,true);h.clock=50;m.onTick();check(h.clicks==1,"wrong/cancelled confirmations ignored");
        h.ack();h.clock=99;m.onTick();check(h.clicks==1,"delay lower boundary");h.clock=100;m.onTick();check(h.clicks==2&&h.slots[7].equals("P-new")&&h.cursor.equals("P-old"),"equip after accepted confirmation");
        m.onPacketReceive(0,1,true,false);h.ack();h.clock=200;m.onTick();check(h.clicks==3&&h.cursor==null&&h.slots[36].equals("P-old"),"returns old pants to spare slot");
        h.ack();m.onTick();check(!m.isEnabled()&&h.closes==1&&h.message.equals("Swapped leggings."),"complete closes only owned empty-cursor inventory");
        for(int empty=0;empty<2;empty++){
            h=new PantHost();h.screen=new Object();h.slots[7]=empty==0?null:"P-old";m=h.create();m.settings.delay=0;m.setEnabled(true);
            int expected=empty==0?2:3;
            for(int i=0;i<expected;i++){m.onTick();h.ack();}
            m.onTick();check(h.clicks==expected&&!m.isEnabled()&&h.closes==0&&h.cursor==null,"empty/occupied armor plan and user GUI ownership");
        }
        h=new PantHost();m=h.create();m.setEnabled(true);m.onTick();m.onPacketReceive(0,h.action,false,false);m.onTick();
        check(!m.isEnabled()&&h.clicks==1&&h.cursor!=null&&h.closes==0,"rejection stops without dropping or closing cursor");
        h=new PantHost();m=h.create();m.setEnabled(true);m.onTick();h.slots[20]="changed";m.onTick();check(!m.isEnabled()&&h.clicks==1,"unrelated inventory mutation invalidates snapshot");
        h=new PantHost();m=h.create();m.setEnabled(true);m.onTick();h.clock=1999;m.onTick();check(m.isEnabled(),"confirmation timeout lower boundary");h.clock=2000;m.onTick();check(!m.isEnabled()&&h.clicks==1,"timeout stops");
        h=new PantHost();m=h.create();h.cursor="held";m.setEnabled(true);check(!m.isEnabled()&&h.clicks==0&&h.cursor.equals("held"),"occupied cursor rejected");
        h=new PantHost();h.container=false;m=h.create();m.setEnabled(true);check(!m.isEnabled()&&h.opens==0,"foreign container rejected");
        h=new PantHost();h.other=true;m=h.create();m.setEnabled(true);check(!m.isEnabled()&&h.opens==0,"other GUI rejected");
        h=new PantHost();h.survival=false;m=h.create();m.setEnabled(true);check(!m.isEnabled(),"creative/spectator rejected");
        h=new PantHost();h.slots[36]=null;h.slots[9]="P-inventory";m=h.create();m.setEnabled(true);check(!m.isEnabled()&&h.opens==0,"hotbar default does not use inventory spare");
        m.settings.inventory=true;m.setEnabled(true);m.onTick();check(h.cursor.equals("P-inventory"),"inventory option scans main inventory");
        h=new PantHost();h.equip=false;m=h.create();m.setEnabled(true);check(!m.isEnabled(),"equip validation gate");
        h=new PantHost();h.observe=false;m=h.create();m.setEnabled(true);m.onTick();check(!m.isEnabled()&&h.clicks==1,"no invented action ID when hook absent");
        h=new PantHost();m=h.create();m.setEnabled(true);check(m.cancelInventoryAction(),"external click guard");m.onTick();check(!m.issuingClick(),"internal click guard cleared");
        h.screen=new Object();m.onTick();check(!m.isEnabled()&&h.closes==0,"new screen not closed");
        h=new PantHost();m=h.create();m.setEnabled(true);h.world=new Object();m.onWorldChange();check(!m.isEnabled()&&h.closes==0,"new world not touched");
        h=new PantHost();h.action=Short.MAX_VALUE;m=h.create();m.setEnabled(true);m.onTick();check(m.pendingTransaction()==Short.MIN_VALUE,"signed transaction wrap");h.ack();h.clock=100;m.onTick();check(h.clicks==2,"negative action acknowledged");
        h=new PantHost();h.throwClick=true;m=h.create();m.setEnabled(true);boolean threw=false;try{m.onTick();}catch(IllegalStateException e){threw=true;}
        check(threw&&!m.isEnabled()&&!m.issuingClick(),"controller exception cleans state");
        PantSwap settingsModule=new PantSwap(new PantHost());settingsModule.settings.delay=-1;invalid(()->settingsModule.settings.validate(),"negative delay");settingsModule.settings.delay=1001;invalid(()->settingsModule.settings.validate(),"excess delay");
        PantHost threaded=new PantHost();PantSwap threadedModule=threaded.create();threadedModule.setEnabled(true);threadedModule.onTick();
        worker(()->threadedModule.onPacketReceive(0,1,true,false));threaded.clock=100;threadedModule.onTick();check(threaded.clicks==2,"receive-thread acknowledgement advances only during main tick");
        System.out.println("PantSwap="+(checks-start));
    }

    static final class PitHost implements Pit.Host {
        boolean ready=true,pit=true,localSpawn,hypixel=true,inventory,survival=true,hud=true,loading,shadow=true,denicker,eventService;
        Object world=new Object();int tick,playerReads,width=320,height=180;
        long clock;List<Pit.PlayerInfo> players=new ArrayList<>();List<Pit.Event> events=new ArrayList<>();
        Pit.InventoryItem[] items=new Pit.InventoryItem[36];List<String> messages=new ArrayList<>(),chat=new ArrayList<>();
        List<Integer> drops=new ArrayList<>();List<PitHud.Panel> panels=new ArrayList<>();
        public boolean ready(){return ready;}public Object worldIdentity(){return world;}public int playerTick(){return tick;}
        public String localName(){return "LocalPlayer";}public boolean inPit(){return pit;}public boolean localInSpawn(boolean only){return localSpawn;}
        public boolean spawnHeight(double y,boolean only){return y>=100&&(!only||pit);}public long nowMillis(){return clock;}
        public List<Pit.PlayerInfo> players(boolean only){playerReads++;return players;}
        public void updateDenicker(boolean enabled,boolean n,boolean only){denicker=enabled;}public void updateEventSchedule(boolean enabled,boolean only){eventService=enabled;}
        public List<Pit.Event> events(){return events;}public boolean eventsLoading(){return loading;}public boolean hypixel(){return hypixel;}
        public void message(String text){messages.add(text);}public void sendChat(String text){chat.add(text);}
        public boolean inventoryScreenOpen(){return inventory;}public boolean survivalOrAdventure(){return survival;}
        public Pit.InventoryItem inventoryItem(int n){return items[n];}public void dropStack(int n){drops.add(n);int inv=n>=36?n-36:n;items[inv]=null;}
        public boolean hudAllowed(){return hud;}public int screenWidth(){return width;}public int screenHeight(){return height;}public int fontHeight(){return 9;}
        public int textWidth(String text){return text.replaceAll("\u00a7.","").length()*6;}public boolean textShadow(){return shadow;}public void drawHud(PitHud.Panel panel){panels.add(panel);}
    }
    static Pit.PlayerInfo person(int id,int cats,double distance,boolean spawn){return new Pit.PlayerInfo(new Object(),new UUID(0,id),"Player"+id,"Reg 3",cats,distance,spawn);}
    static void pitChecks(){
        int start=checks;PitHost h=new PitHost();Pit m=new Pit(h);
        check(Pit.Settings.class.getFields().length==35,"all current settings represented");
        check(m.settings.pitOnly&&m.settings.denicker&&m.settings.denickerNotifications&&!m.settings.autoMath&&!m.settings.chatFilter,"pit main defaults");
        check(m.settings.playerListMaxPlayers==10&&m.settings.playerListAlerts==Pit.Alerts.OFF&&!m.settings.playerListEnchants,"list defaults");
        check(m.settings.hideFarPlayer==Pit.HideFar.NONE&&m.settings.hidePitSpawn==Pit.HideSpawn.NONE&&m.settings.showInSpawn,"hide defaults");
        m.onTick();check(!h.denicker&&!h.eventService,"disabled services");m.setEnabled(true);m.onTick();check(h.denicker,"actual denicker enable delegated");
        m.settings.chatFilter=true;check(m.onChat("\u00a7r\u00a7c\u00a7lSTREAK! Other", "",0,false),"formatted streak filtered");
        check(!m.onChat("\u00a7c\u00a7lSTREAK! LocalPlayer","",0,false),"own name retained");
        check(!m.onChat("\u00a7c\u00a7lhello STREAK","",0,true),"cancelled receive untouched");
        check(!m.onChat("STREAK! Other","",0,false),"plain text not false-positive filter");
        check(!m.onChat("\u00a76\u00a7lBOUNTY! Other","",0,false),"bounty default off");m.settings.chatFilterBounty=true;
        check(m.onChat("\u00a76\u00a7lBOUNTY! Other","",0,false),"bounty on");h.pit=false;
        check(!m.onChat("\u00a7c\u00a7lSTREAK! Other","",0,false),"pit-only filter gate");m.settings.pitOnly=false;
        check(m.onChat("\u00a7c\u00a7lSTREAK! Other","",0,false),"pit-only off");h.pit=true;m.settings.pitOnly=true;
        m.settings.autoMath=true;m.onChat("","QUICK MATHS! Solve: 2 + 3 * 4",0,false);h.clock=249;m.onTick();check(h.chat.isEmpty(),"queued math delay");
        h.clock=250;m.onTick();check(h.chat.equals(Arrays.asList("/ac 14")),"queued math result");
        m.onChat("","QUICK MATHS! Solve: 2 + 3 * 4",0,false);h.clock=600;m.onTick();check(h.chat.size()==1,"duplicate question suppressed");
        m.onChat("","QUICK MATHS! Solve: 1+1",2,false);h.clock=900;m.onTick();check(h.chat.size()==1,"actionbar not parsed");
        m.onChat("","QUICK MATHS! Solve: 1+1",0,false);m.onWorldChange();h.clock=2000;m.onTick();check(h.chat.size()==1,"world discards queued answer");
        String[][] expressions={{"2+3*4","14"},{"(2+3)*4","20"},{"-2 * -3","6"},{"2.50+.5","3"},{"9 \u00f7 2","4.5"},{"3x4","12"},{"3\u00d74","12"},{"+2-5","-3"}};
        for(String[] pair:expressions)check(PitAutoMath.solve(pair[0]).orElse("bad").equals(pair[1]),"math "+pair[0]);
        for(String bad:new String[]{"1/0","","1+","1e2","System.exit(0)","(1+2","1 2","NaN","..1"})check(!PitAutoMath.solve(bad).isPresent(),"reject malformed math "+bad);
        check(!PitAutoMath.solve(String.join("",Collections.nCopies(129,"1"))).isPresent(),"expression length bound");
        PitAutoMath threadedMath=new PitAutoMath();List<String> answers=new ArrayList<>();worker(()->threadedMath.receive("QUICK MATHS! Solve: 3+4",0));
        threadedMath.tick(true,250,answers::add);check(answers.equals(Arrays.asList("/ac 7")),"receive-thread math hands off to main tick");
        m.settings.playerList=true;m.settings.playerListAlerts=Pit.Alerts.BOTH;m.settings.playerListEnchants=true;
        h.players.add(person(1,1,5,false));h.clock=3000;m.onTick();check(h.messages.isEmpty(),"list initial baseline silent");
        m.onRender();check(h.panels.size()==1&&h.panels.get(0).rows.get(0).contains("Friends")&&h.panels.get(0).rows.get(1).contains("Reg 3"),"classified rows and enchants");
        int reads=h.playerReads;h.clock=3249;m.onTick();check(h.playerReads==reads,"250ms directory throttle");
        h.players.add(person(2,3,10,false));h.clock=3250;m.onTick();check(h.messages.equals(Arrays.asList("Player2 joined.")),"one join per UUID across categories");
        h.players.remove(0);h.clock=3500;m.onTick();check(h.messages.get(1).equals("Player1 left."),"leave alert");
        for(int i=0;i<7;i++)check(m.categoryEnabled(i),"default category "+i);m.settings.playerListNicked=false;check(!m.categoryEnabled(6)&&!m.categoryEnabled(99),"nicked fallback category");
        h.hud=false;int panels=h.panels.size();m.onRender();check(h.panels.size()==panels,"HUD debug/screen gate");h.hud=true;
        m.settings.playerList=false;m.settings.eventList=true;h.events.add(new Pit.Event("Major",h.clock+61001,true));h.events.add(new Pit.Event("Expired",h.clock,false));
        h.hypixel=false;m.onRender();PitHud.Panel eventPanel=h.panels.get(h.panels.size()-1);
        check(eventPanel.rows.get(0).equals("[M] Major \u00a7701:02")&&eventPanel.rows.size()==2,"event ceil seconds, major marker, nonhypixel note");
        check(eventPanel.rightAligned,"right aligned event rows");h.events.clear();h.loading=true;m.onRender();check(h.panels.get(h.panels.size()-1).rows.get(0).contains("Loading"),"loading status");
        h.loading=false;m.onRender();check(h.panels.get(h.panels.size()-1).rows.get(0).contains("unavailable"),"missing schedule explicit");
        PitHud.Panel layout=PitHud.layout(Arrays.asList("One","Two"),Pit.Horizontal.RIGHT,Pit.Vertical.BOTTOM,2,2,1,h,false);
        near(layout.x,320-layout.width-2,"right anchor");near(layout.y,180-layout.height-2,"bottom anchor");
        h.width=50;h.height=25;layout=PitHud.layout(Arrays.asList("Long [enchants]","two","three","four"),Pit.Horizontal.LEFT,Pit.Vertical.TOP,255,255,1,h,true);
        check(layout.width<=50&&layout.rows.size()==2&&layout.rows.get(1).contains("more"),"HUD width/height clipping");
        check(layout.x>=0&&layout.x+layout.width<=50,"HUD clamped x");h.width=320;h.height=180;
        m.settings.inventoryCleaner=true;h.inventory=true;h.items[0]=new Pit.InventoryItem(Pit.ItemKind.IRON_ARMOR,true,true);
        m.onRotation();m.onRotation();check(h.drops.isEmpty(),"inventory opening delay");m.onRotation();check(h.drops.equals(Arrays.asList(36)),"source iron armor rule includes named/enchanted");
        h.items[1]=new Pit.InventoryItem(Pit.ItemKind.IRON_SWORD,false,false);m.onRotation();m.onRotation();check(h.drops.size()==1,"no diamond in hotbar keeps iron sword");
        h.items[2]=new Pit.InventoryItem(Pit.ItemKind.DIAMOND_SWORD,false,false);m.onRotation();check(h.drops.get(1)==37,"diamond hotbar permits plain iron sword drop");
        h.items[3]=new Pit.InventoryItem(Pit.ItemKind.IRON_SWORD,true,false);m.onRotation();m.onRotation();check(h.drops.size()==2,"named iron sword retained");
        for(int i=0;i<9;i++)check(Pit.containerSlot(i)==36+i,"hotbar mapping "+i);for(int i=9;i<36;i++)check(Pit.containerSlot(i)==i,"inventory mapping "+i);
        check(Pit.containerSlot(36)==8&&Pit.containerSlot(39)==5,"armor mapping");
        Pit.PlayerInfo at24=person(10,0,24,false),at64=person(11,0,64,false),far=person(12,0,65,false),spawn=person(13,0,100,true);
        m.settings.hideFarPlayer=Pit.HideFar.CLOSE;check(!m.hideEntity(at24)&&m.hideEntity(at64),"close strict24");
        m.settings.hideFarPlayer=Pit.HideFar.EXTENDED;check(!m.hideEntity(at64)&&m.hideEntity(far),"extended strict64");
        h.players.clear();h.players.add(at24);h.players.add(at64);h.players.add(far);h.players.add(spawn);h.tick++;
        m.settings.hideFarPlayer=Pit.HideFar.SMART;check(!m.hideEntity(at24)&&!m.hideEntity(at64)&&m.hideEntity(far)&&!m.hideEntity(spawn),"smart distances and spawn exception");
        reads=h.playerReads;m.hideEntity(at64);check(h.playerReads==reads,"smart cache once per world/tick");
        h.players.clear();for(int i=0;i<25;i++)h.players.add(person(100+i,0,10+i*.1,false));h.players.add(at64);h.tick++;
        check(!m.hideEntity(h.players.get(24))&&m.hideEntity(at64),"all within24 kept even above24 budget");
        m.settings.hideFarPlayer=Pit.HideFar.NONE;m.settings.hidePitSpawn=Pit.HideSpawn.VISUALS;
        check(m.hideVisual(spawn)&&!m.hideEntity(spawn),"visual spawn mode only");m.settings.hidePitSpawn=Pit.HideSpawn.ENTITY;
        check(m.hideEntity(spawn)&&m.hideSpawnHeight(100),"entity spawn and height predicate");h.localSpawn=true;
        check(!m.hideEntity(spawn)&&!m.hideSpawnHeight(100),"show in spawn preserves others");m.settings.showInSpawn=false;
        check(m.hideEntity(spawn),"show-in-spawn off");check(!m.hideEntity(null)&&!m.hideVisual(null),"null entity");
        m.setEnabled(false);check(!m.hideEntity(spawn)&&!h.denicker&&!h.eventService,"disable cleanup");
        Pit invalidModule=new Pit(new PitHost());invalidModule.settings.playerListScale=Float.NaN;invalid(()->invalidModule.settings.validate(),"nan scale");
        invalidModule.settings.playerListScale=1;invalidModule.settings.playerListOffsetX=256;invalid(()->invalidModule.settings.validate(),"offset bound");
        invalidModule.settings.playerListOffsetX=2;invalidModule.settings.playerListMaxPlayers=0;invalid(()->invalidModule.settings.validate(),"max bound");
        System.out.println("Pit="+(checks-start));
    }
    public static void main(String[] args){swordChecks();pantChecks();pitChecks();System.out.println("PASS "+checks);}
}
