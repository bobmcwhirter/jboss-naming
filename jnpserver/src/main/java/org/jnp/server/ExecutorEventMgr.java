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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.naming.Binding;
import javax.naming.Name;
import javax.naming.event.NamingEvent;

import org.jboss.logging.Logger;
/**
 * An EventMgr implementation that uses an Executor to dispatch the events
 * in the background.
 * 
 * @author Scott.Stark@jboss.org
 * @version $Revision:$
 */
public class ExecutorEventMgr implements EventMgr
{
   private static Logger log = Logger.getLogger(ExecutorEventMgr.class);
   private Executor executor = Executors.newSingleThreadExecutor();

   public ExecutorEventMgr()
   {
   }

   public Executor getExecutor()
   {
      return executor;
   }
   public void setExecutor(Executor executor)
   {
      this.executor = executor;
   }

   public void fireEvent(Name fullName, Binding oldb, Binding newb, int type,
         String changeInfo, EventListeners listeners, Set<Integer> scopes)
   {
      Dispatcher d = new Dispatcher(fullName, newb, oldb, type,
            changeInfo, listeners, scopes);
      executor.execute(d);
   }

   static class Dispatcher implements Runnable
   {
      Name fullName;
      Binding oldb;
      Binding newb;
      int type;
      String changeInfo;
      EventListeners listeners;
      Set<Integer> scopes;

      public Dispatcher(Name fullName, Binding newb, Binding oldb, int type,
           String changeInfo, EventListeners listeners, Set<Integer> scopes)
      {
         this.fullName = fullName;
         this.newb = newb;
         this.oldb = oldb;
         this.type = type;
         this.listeners = listeners;
         this.changeInfo = changeInfo;
         this.scopes = scopes;
      }
      public void run()
      {
         listeners.fireEvent(fullName, oldb, newb, type, changeInfo, scopes);
      }
   }
 }
