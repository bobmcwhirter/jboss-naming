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

import java.util.Set;

import javax.naming.Binding;
import javax.naming.Name;

/**
 * A plugin for the manager which dispatches EventContext events to listeners 
 * 
 * @see {@linkplain ExecutorEventMgr}
 * @author Scott.Stark@jboss.org
 * @version $Revision:$
 */
public interface EventMgr
{
   /**
    * Dispatch an event to the listeners.
    * 
    * @param fullName - the full path of name of the event location
    * @param oldb - the possibly old binding of the event
    * @param newb - the possibly new binding of the event
    * @param type - one of {@link javax.naming.event.NamingEvent#OBJECT_ADDED},
    * {@link javax.naming.event.NamingEvent#OBJECT_CHANGED},
    * {@link javax.naming.event.NamingEvent#OBJECT_REMOVED} events.
    * @param changeInfo - the provider specific change information. The current
    * impl passes in the name of the operation that generated the event.
    * @param listeners - the list of NamingListener info for the EventContext
    * associated with the event.
    * @param scopes - a set of the EventContext scopes that apply.
    */
   void fireEvent(Name fullName, Binding oldb, Binding newb, int type,
         String changeInfo, EventListeners listeners, Set<Integer> scopes);
}
