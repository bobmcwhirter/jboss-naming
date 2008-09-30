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
package org.jnp.test.support;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.naming.event.NamespaceChangeListener;
import javax.naming.event.NamingEvent;
import javax.naming.event.NamingExceptionEvent;
import javax.naming.event.ObjectChangeListener;

/**
 * @author Scott.Stark@jboss.org
 * @version $Revision:$
 */
public class QueueEventListener implements ObjectChangeListener,
      NamespaceChangeListener
{
   private ArrayList<NamingEvent> events = new ArrayList<NamingEvent>();
   private Semaphore eventCount = new Semaphore(0);
   private NamingExceptionEvent ex;

   public boolean waitOnEvent() throws InterruptedException
   {
      return eventCount.tryAcquire(2, TimeUnit.SECONDS);
   }
   public NamingEvent getEvent(int index)
   {
      return events.get(index);
   }

   public void objectChanged(NamingEvent evt)
   {
      System.out.println("Begin objectChanged, "+evt);
      events.add(evt);
      eventCount.release();
      System.out.println("End objectChanged, "+evt);
   }

   public void namingExceptionThrown(NamingExceptionEvent evt)
   {
      ex = evt;
   }

   public void objectAdded(NamingEvent evt)
   {
      System.out.println("Begin objectAdded, "+evt);
      events.add(evt);
      eventCount.release();
      System.out.println("End objectAdded, "+evt);
   }

   public void objectRemoved(NamingEvent evt)
   {
      events.add(evt);
      eventCount.release();
   }

   public void objectRenamed(NamingEvent evt)
   {
      events.add(evt);
      eventCount.release();
   }
   public int getEventCount()
   {
      return events.size();
   }

}
