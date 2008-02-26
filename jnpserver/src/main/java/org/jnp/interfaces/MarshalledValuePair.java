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

import java.io.IOException;
import java.io.Serializable;
import java.rmi.MarshalledObject;

/** An encapsulation of a JNDI binding as both the raw object and its
 MarshalledObject form. When accessed in the same VM as the JNP server,
 the raw object reference is used to avoid deserialization.

 @author Scott.Stark@jboss.org
 @version $Revision$
 */
public class MarshalledValuePair implements Serializable
{
   /** @since 3.2.0 */
   private static final long serialVersionUID = -3403843515711139134L;

   private static boolean enableCallByReference = true;
   public MarshalledObject marshalledValue;
   public transient Object value;

   /** Get the lookp call by reference flag.
    * @return false if all lookups are unmarshalled using the caller's TCL,
    *    true if in VM lookups return the value by reference.
    */ 
   public static boolean getEnableCallByReference()
   {
      return enableCallByReference;
   }
   /** Set the lookp call by reference flag.
    * @param flag - false if all lookups are unmarshalled using the caller's TCL,
    *    true if in VM lookups return the value by reference.
    */ 
   public static void setEnableCallByReference(boolean flag)
   {
      enableCallByReference = flag;
   }

   /** Creates a new instance of MashalledValuePair */
   public MarshalledValuePair(Object value) throws IOException
   {
      this.value = value;
      this.marshalledValue = new MarshalledObject(value);
   }

   public Object get() throws ClassNotFoundException, IOException
   {
      Object theValue = enableCallByReference ? value : null;
      if( theValue == null && marshalledValue != null )
         theValue = marshalledValue.get();
      return theValue;
   }
}
