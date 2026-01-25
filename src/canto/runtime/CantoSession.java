/* Canto Compiler and Runtime Engine
 * 
 * CantoSession.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import org.eclipse.jetty.server.Session;

import java.util.*;

/**
 * CantoSession support for Canto.
 */

public class CantoSession {
    
    /** Default maximum interval, in seconds, between requests.  If a new request
     *  isn't received within this interval then the session times out and is 
     *  invalidated.
     *  
     *  An interval value less than or equal to zero means the session never
     *  times out. 
     **/
    public static int DEFAULT_INACTIVE_INTERVAL = 0;

    private String id = null;
    private Session session = null;
    private Map<String, Object> attributes = null;
    private long createdTime = 0L;
    private long lastAccessedTime = 0L;
    private int maxInactive = 0;
    private boolean invalidated = false;
    

    public CantoSession(Session session) {
        this.session = session;
        attributes = session.asAttributeMap();
        createdTime = lastAccessedTime = session.getLastAccessedTime();
        maxInactive = session.getMaxInactiveInterval();
        id = session.getId();
    }

    public CantoSession(String sessionId) {
        attributes = new HashMap<String, Object>();
        createdTime = lastAccessedTime = System.currentTimeMillis();
        maxInactive = DEFAULT_INACTIVE_INTERVAL;
        id = sessionId;
    }

    public Map<String, Object> attributes() {
        return attributes;
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public void removeAttribute(String key) {
        attributes.remove(key);
    }

    public long created() {
        return createdTime;
    }

    public long accessed() {
        if (session != null) {
            return Math.max(session.getLastAccessedTime(), lastAccessedTime);
        } else {
            return lastAccessedTime;
        }
    }

    public String id() {
        return id;
    }

    public int max_inactive(int max) {
        if (session != null) {
            session.setMaxInactiveInterval(max);
            maxInactive = session.getMaxInactiveInterval();
        } else {
            maxInactive = max;
        }
        return maxInactive;
    }

    public int max_inactive() {
        if (session != null) {
            maxInactive = session.getMaxInactiveInterval();
        }
        return maxInactive;
    }
    
    public void invalidate() {
        if (session != null) {
            session.invalidate();
        }
        invalidated = true;
    };
    
    public void updateAccessedTime() {
        long now = System.currentTimeMillis();
        if (maxInactive > 0 && (now - lastAccessedTime)/1000 > maxInactive) {
            invalidated = true;
        } else {
            lastAccessedTime = now;
        }
    }
    
    public boolean is_valid() {
        return !invalidated;
    }
}
