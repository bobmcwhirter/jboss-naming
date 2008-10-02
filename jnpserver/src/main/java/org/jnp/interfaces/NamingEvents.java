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
package org.jnp.interfaces;

import java.rmi.RemoteException;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.event.EventContext;
import javax.naming.event.NamingListener;

/**
 * An extended naming server/proxy that support events
 * @see javax.naming.event.EventContext
 * 
 * @author Scott.Stark@jboss.org
 * @version $Revision:$
 */
public interface NamingEvents extends Naming
{
   /**
    * Adds a listener for receiving naming events fired
    * when the object(s) identified by a target and scope changes.
    *
    * @param context - the non-null EventContext the listener is registering with
    * @param target A non-null name to be resolved relative to this context.
    * @param scope One of <tt>OBJECT_SCOPE</tt>, <tt>ONELEVEL_SCOPE</tt>, or
    * <tt>SUBTREE_SCOPE</tt>.
    * @param l  The non-null listener.
    * @exception NamingException If a problem was encountered while
    * adding the listener.
    * @see #removeNamingListener
    */
   void addNamingListener(EventContext context, Name target, int scope, NamingListener l) 
      throws NamingException, RemoteException;

   /**
    * Removes a listener from receiving naming events
    * @param l non-null listener.
    * @exception NamingException If a problem was encountered while
    * removing the listener.
    * @see #addNamingListener
    */
   void removeNamingListener(NamingListener l) throws NamingException, RemoteException;

   /**
    * Determines whether a listener can register interest in a target
    * that does not exist.
    *
    * @return true if the target must exist; false if the target need not exist.
    * @exception NamingException If the context's behavior in this regard cannot
    * be determined.
    */
   boolean targetMustExist() throws NamingException, RemoteException;
}
