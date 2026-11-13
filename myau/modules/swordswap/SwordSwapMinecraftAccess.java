package myau.modules.swordswap;

import java.lang.reflect.*;

public final class SwordSwapMinecraftAccess {
    private SwordSwapMinecraftAccess(){}
    private static final ClassValue<Method> SYNC=new ClassValue<Method>(){
        protected Method computeValue(Class<?> type){
            for(Class<?> owner=type;owner!=null;owner=owner.getSuperclass())for(String name:new String[]{"syncCurrentPlayItem","func_78750_j"}){
                try{
                    Method method=owner.getDeclaredMethod(name);
                    if(method.getReturnType()!=Void.TYPE)throw new IllegalStateException("Unexpected controller sync return type");
                    method.setAccessible(true);return method;
                }catch(NoSuchMethodException absent){}
            }
            throw new IllegalStateException("Controller item sync mapping unavailable");
        }
    };
    private static final ClassValue<Field[]> RENDERER=new ClassValue<Field[]>(){
        protected Field[] computeValue(Class<?> type){
            return new Field[]{field(type,"itemToRender","field_78453_b",null),field(type,"equippedItemSlot","field_78450_g",Integer.TYPE),
                    field(type,"equippedProgress","field_78454_c",Float.TYPE),field(type,"prevEquippedProgress","field_78451_d",Float.TYPE)};
        }
    };
    private static Field field(Class<?> type,String mcp,String srg,Class<?> expected){
        for(Class<?> owner=type;owner!=null;owner=owner.getSuperclass())for(String name:new String[]{mcp,srg}){
            try{
                Field field=owner.getDeclaredField(name);
                if(expected==null?field.getType().isPrimitive():field.getType()!=expected)throw new IllegalStateException("Unexpected renderer field type: "+name);
                field.setAccessible(true);return field;
            }catch(NoSuchFieldException absent){}
        }
        throw new IllegalStateException("Renderer field mapping unavailable: "+mcp);
    }
    public static void sync(Object controller){
        try{SYNC.get(controller.getClass()).invoke(controller);}
        catch(InvocationTargetException e){
            Throwable cause=e.getCause();if(cause instanceof RuntimeException)throw (RuntimeException)cause;
            if(cause instanceof Error)throw (Error)cause;throw new IllegalStateException(cause);
        }catch(IllegalAccessException e){throw new IllegalStateException(e);}
    }
    public static void show(Object renderer,Object stack,int slot){
        Field[] fields=RENDERER.get(renderer.getClass());
        try{fields[0].set(renderer,stack);fields[1].setInt(renderer,slot);fields[2].setFloat(renderer,1);fields[3].setFloat(renderer,1);}
        catch(IllegalAccessException e){throw new IllegalStateException(e);}
    }
}
