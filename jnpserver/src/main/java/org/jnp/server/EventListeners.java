/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jnp.server;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.naming.Binding;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.event.EventContext;
import javax.naming.event.NamespaceChangeListener;
import javax.naming.event.NamingEvent;
import javax.naming.event.NamingListener;
import javax.naming.event.ObjectChangeListener;

import org.jboss.logging.Logger;

/**
 * @author Scott.Stark@jboss.org
 * @version $Revision:$
 */
public class EventListeners
{
   private static Logger log = Logger.getLogger(EventListeners.class);
   private EventContext context;
   private CopyOnWriteArrayList<EventListenerInfo> listeners;
   private boolean trace;

   public EventListeners(EventContext context)
   {
      super();
      this.context = context;
      this.trace = log.isTraceEnabled();
   }

   public synchronized void addNamingListener(EventContext context, Name target, int scope, NamingListener l) 
      throws NamingException
   {
      if(listeners == null)
         listeners = new CopyOnWriteArrayList<EventListenerInfo>();
      this.context = context;
      String prefix = context.getNameInNamespace();
      String fullTargetName = prefix + target.toString();
      EventListenerInfo info = new EventListenerInfo(l, fullTargetName, scope);
      listeners.add(info);
   }
   public void removeNamingListener(NamingListener l)
   {
      if(listeners == null)
         return;

      EventListenerInfo info = new EventListenerInfo(l, null, 0);
      listeners.remove(info);
   }
   public void fireEvent(Name fullName, Binding oldb, Binding newb, int type,
         String changeInfo, Set<Integer> scopes)
   {
      if(listeners != null)
      {
         if(trace)
         {
            log.trace("fireEvent, fullName:"+fullName+" type: "+type
                  +", changeInfo:"+changeInfo+", scopes:"+scopes);
         }
         String name = fullName.toString();
         NamingEvent event = new NamingEvent(context, type, newb, oldb, changeInfo);
         for(EventListenerInfo info : listeners)
         {
            if(scopes.contains(info.getScope()))
            {
               String targetName = info.getFullTargetName();
               int scope = info.getScope();
               boolean matches = false;
               if(scope == EventContext.SUBTREE_SCOPE)
               {
                  // SUBTREE_SCOPE matches the target or subcontext
                  matches = name.startsWith(targetName);
               }
               else if(scope == EventContext.ONELEVEL_SCOPE)
               {
                  // ONELEVEL_SCOPE matches immediate children of the context
                  matches = fullName.size() == 1;
               }
               else
                  matches = name.equals(targetName);
               if(matches)
                  dispatch(info.getListener(), event);
            }
         }
      }
   }
   public void dispatch(NamingListener listener, NamingEvent event)
   {
      switch (event.getType())
      {
      case NamingEvent.OBJECT_ADDED:
         if(listener instanceof NamespaceChangeListener)
          ((NamespaceChangeListener)listener).objectAdded(event);
          break;

      case NamingEvent.OBJECT_REMOVED:
         if(listener instanceof NamespaceChangeListener)
          ((NamespaceChangeListener)listener).objectRemoved(event);
          break;

      case NamingEvent.OBJECT_RENAMED:
         if(listener instanceof NamespaceChangeListener)
          ((NamespaceChangeListener)listener).objectRenamed(event);
          break;

      case NamingEvent.OBJECT_CHANGED:
         if(listener instanceof ObjectChangeListener)
          ((ObjectChangeListener)listener).objectChanged(event);
          break;
      }
   }

}
