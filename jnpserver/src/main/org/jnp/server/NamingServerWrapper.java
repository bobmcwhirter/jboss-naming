/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

import java.rmi.RemoteException;
import java.util.Collection;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;

import org.jnp.interfaces.Naming;

/**
 * A delegating wrapper that can be used to create a unique rmi server endpoint
 * that shares the an underlying Naming server implementation.
 * 
 * @author Scott.Stark@jboss.org
 * @version $Revision:$
 */
public class NamingServerWrapper
   implements Naming
{
   Naming delegate;
   NamingServerWrapper(Naming delegate)
   {
      this.delegate = delegate;
   }

   public void bind(Name name, Object obj, String className)
      throws NamingException, RemoteException
   {
      delegate.bind(name, obj, className);
   }
   public Context createSubcontext(Name name)
      throws NamingException, RemoteException
   {
      return delegate.createSubcontext(name);
   }
   public Collection list(Name name)
      throws NamingException, RemoteException
   {
      return delegate.list(name);
   }
   public Collection listBindings(Name name)
      throws NamingException, RemoteException
   {
      return delegate.listBindings(name);
   }
   public Object lookup(Name name)
      throws NamingException, RemoteException
   {
      return delegate.lookup(name);
   }
   public void rebind(Name name, Object obj, String className)
      throws NamingException, RemoteException
   {
      delegate.rebind(name, obj, className);
   }
   public void unbind(Name name)
      throws NamingException, RemoteException
   {
      delegate.unbind(name);
   }
}
