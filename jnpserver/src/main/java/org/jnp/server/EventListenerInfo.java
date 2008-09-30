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

import javax.naming.event.NamespaceChangeListener;
import javax.naming.event.NamingEvent;
import javax.naming.event.NamingExceptionEvent;
import javax.naming.event.NamingListener;
import javax.naming.event.ObjectChangeListener;

/**
 * Encapsulation of the NamingListener, scope and EventContext the listener
 * registered with.
 * 
 * @author Scott.Stark@jboss.org
 * @version $Revision:$
 */
public class EventListenerInfo
   implements NamespaceChangeListener, ObjectChangeListener
{
   private NamingListener listener;
   private String fullTargetName;
   private int scope;

   public EventListenerInfo(NamingListener listener, String fullTargetName, int scope)
   {
      super();
      this.listener = listener;
      this.fullTargetName = fullTargetName;
      this.scope = scope;
   }

   public NamingListener getListener()
   {
      return listener;
   }

   public String getFullTargetName()
   {
      return fullTargetName;
   }

   public int getScope()
   {
      return scope;
   }

   public boolean isNamespaceChangeListener()
   {
      return listener instanceof NamespaceChangeListener;
   }
   public boolean isObjectChangeListener()
   {
      return listener instanceof ObjectChangeListener;
   }

   
   public void objectChanged(NamingEvent evt)
   {
      ObjectChangeListener ocl = (ObjectChangeListener) listener;
      ocl.objectChanged(evt);
   }

   public void objectAdded(NamingEvent evt)
   {
      NamespaceChangeListener ncl = (NamespaceChangeListener) listener;
      ncl.objectAdded(evt);
   }

   public void objectRemoved(NamingEvent evt)
   {
      NamespaceChangeListener ncl = (NamespaceChangeListener) listener;
      ncl.objectAdded(evt);
   }

   public void objectRenamed(NamingEvent evt)
   {
      NamespaceChangeListener ncl = (NamespaceChangeListener) listener;
      ncl.objectAdded(evt);
   }

   public void namingExceptionThrown(NamingExceptionEvent evt)
   {
      listener.namingExceptionThrown(evt);
   }

   @Override
   public boolean equals(Object obj)
   {
      boolean equals = false;
      if(obj instanceof EventListenerInfo)
      {
         EventListenerInfo eli = (EventListenerInfo) obj;
         equals = listener.equals(eli.listener);
      }
      return equals;
   }

   @Override
   public int hashCode()
   {
      return listener.hashCode();
   }
}
