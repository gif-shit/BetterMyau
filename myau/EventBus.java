package myau;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EventBus {

    private final Map<Class<? extends Event>, List<EventListener>> listeners = new LinkedHashMap<Class<? extends Event>, List<EventListener>>();

    public void register(Object owner) {
        for (Method method : owner.getClass().getDeclaredMethods()) {
            EventHandler handler = method.getAnnotation(EventHandler.class);
            if (handler == null)
                continue;
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 1 || !Event.class.isAssignableFrom(parameterTypes[0]))
                continue;
            Class<? extends Event> eventType = parameterTypes[0].asSubclass(Event.class);
            if (!method.isAccessible())
                method.setAccessible(true);
            List<EventListener> eventListeners = this.listeners.computeIfAbsent(eventType, type -> new ArrayList<>());
            eventListeners.add(new EventListener(owner, method, handler.priority()));
            eventListeners.sort(Comparator.comparingInt(EventListener::getPriority).reversed());
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
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
            }
        }
        return event;
    }
}
