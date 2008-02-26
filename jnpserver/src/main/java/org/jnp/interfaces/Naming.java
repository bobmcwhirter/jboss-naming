/*
  * JBoss, Home of Professional Open Source
  * Copyright 2005, JBoss Inc., and individual contributors as indicated
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

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;


import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;

/**
 *   <description> 
 *      
 *   @see <related>
 *   @author $Author$
 *   @version $Revision$
 */
public interface Naming
   extends Remote
{
   // Public --------------------------------------------------------
   public void bind(Name name, Object obj, String className)
      throws NamingException, RemoteException;

   public void rebind(Name name, Object obj, String className)
      throws NamingException, RemoteException;

   public void unbind(Name name)
      throws NamingException, RemoteException;

   public Object lookup(Name name)
      throws NamingException, RemoteException;

   public Collection list(Name name)
      throws NamingException, RemoteException;

   public Collection listBindings(Name name)
      throws NamingException, RemoteException;
      
   public Context createSubcontext(Name name)
      throws NamingException, RemoteException;
}
