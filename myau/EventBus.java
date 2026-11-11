package myau;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import myau.Event;
import myau.EventHandler;
import myau.EventListener;

public final class EventBus {
    private final Map<Class<? extends Event>, List<EventListener>> listeners = new LinkedHashMap<Class<? extends Event>, List<EventListener>>();

    public void register(Object owner) {
        for (Method method : owner.getClass().getDeclaredMethods()) {
            List<EventListener> eventListeners;
            Class<?>[] parameterTypes;
            EventHandler handler = method.getAnnotation(EventHandler.class);
            if (handler == null || (parameterTypes = method.getParameterTypes()).length != 1 || !Event.class.isAssignableFrom(parameterTypes[0])) continue;
            Class<?> eventType = parameterTypes[0];
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
            if ((eventListeners = this.listeners.get(eventType)) == null) {
                eventListeners = new ArrayList<EventListener>();
                this.listeners.put(eventType, eventListeners);
            }
            eventListeners.add(new EventListener(owner, method, handler.priority()));
            Collections.sort(eventListeners, Comparator.comparingInt(EventListener::getPriority).reversed());
        }
    }

    public Event post(Event event) {
        List<EventListener> eventListeners = this.listeners.get(event.getClass());
        if (eventListeners == null) {
            return event;
        }
        for (EventListener listener : eventListeners) {
            try {
                listener.getMethod().invoke(listener.getOwner(), event);
            }
            catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {}
        }
        return event;
    }
}
