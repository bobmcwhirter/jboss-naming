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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.rmi.ConnectException;
import java.rmi.MarshalledObject;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import javax.naming.Binding;
import javax.naming.CannotProceedException;
import javax.naming.CommunicationException;
import javax.naming.ConfigurationException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.InvalidNameException;
import javax.naming.LinkRef;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NotContextException;
import javax.naming.ContextNotEmptyException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.ServiceUnavailableException;
import javax.naming.event.EventContext;
import javax.naming.event.NamingListener;
import javax.naming.spi.NamingManager;
import javax.naming.spi.ResolveResult;
import javax.net.SocketFactory;

import org.jboss.logging.Logger;

/**
 * This class provides the jnp provider Context implementation. It is a Context
 * interface wrapper for a RMI Naming instance that is obtained from either the
 * local server instance or by locating the server given by the
 * Context.PROVIDER_URL value.
 *
 * This class also serves as the jnp url resolution context. jnp style urls
 * passed to the
 * @author oberg
 * @author scott.stark@jboss.org
 * @version $Revision$
 */
public class NamingContext
   implements EventContext, java.io.Serializable
{
   // Constants -----------------------------------------------------
   /**
    * @since 1.7
    */
   static final long serialVersionUID = 8906455608484282128L;
   /**
    * The javax.net.SocketFactory impl to use for the bootstrap socket
    */
   public static final String JNP_SOCKET_FACTORY = "jnp.socketFactory";
   /**
    * The local address to bind the connected bootstrap socket to
    */
   public static final String JNP_LOCAL_ADDRESS = "jnp.localAddress";
   /**
    * The local port to bind the connected bootstrap socket to
    */
   public static final String JNP_LOCAL_PORT = "jnp.localPort";
   /**
    * A flag to disable the broadcast discovery queries
    */
   public static final String JNP_DISABLE_DISCOVERY = "jnp.disableDiscovery";
   /**
    * The cluster partition discovery should be restricted to
    */
   public static final String JNP_PARTITION_NAME = "jnp.partitionName";
   /**
    * The multicast IP/address to which the discovery query is sent
    */
   public static final String JNP_DISCOVERY_GROUP = "jnp.discoveryGroup";
   /**
    * The port to which the discovery query is sent
    */
   public static final String JNP_DISCOVERY_PORT = "jnp.discoveryPort";

   /** The time-to-live for the multicast discovery packets */
   public static final String JNP_DISCOVERY_TTL = "jnp.discoveryTTL";

   /**
    * The time in MS to wait for a discovery query response
    */
   public static final String JNP_DISCOVERY_TIMEOUT = "jnp.discoveryTimeout";
   /**
    * An internal property added by parseNameForScheme if the input name uses a
    * url prefix that was removed during cannonicalization. This is needed to
    * avoid modification of the incoming Name.
    */
   public static final String JNP_PARSED_NAME = "jnp.parsedName";
   /**
    * A flag indicating the style of names passed to NamingManager method.
    * True for api expected relative names, false for absolute names as used
    * historically by the jboss naming implementation.
    */
   public static final String JNP_USE_RELATIVE_NAME = "jnp.useRelativeName";
   /**
    * An integer that controls the number of connection retry attempts will
    * be made on the initial connection to the naming server. This only applies
    * to ConnectException failures. A value <= 1 means that only one attempt
    * will be made.
    */ 
   public static final String JNP_MAX_RETRIES = "jnp.maxRetries";

   /**
    * The default discovery multicast information
    */
   public final static String DEFAULT_DISCOVERY_GROUP_ADDRESS = "230.0.0.4";
   public final static int DEFAULT_DISCOVERY_GROUP_PORT = 1102;
   public final static int DEFAULT_DISCOVERY_TIMEOUT = 5000;

   /**
    * An obsolete constant replaced by the JNP_MAX_RETRIES value
    */
   public static int MAX_RETRIES = 1;
   /**
    * The JBoss logging interface
    */
   private static Logger log = Logger.getLogger(NamingContext.class);

   // Static --------------------------------------------------------
   
   public static Hashtable haServers = new Hashtable();

   public static void setHANamingServerForPartition(String partitionName, Naming haServer)
   {
      haServers.put(partitionName, haServer);
   }

   public static void removeHANamingServerForPartition(String partitionName)
   {
      haServers.remove(partitionName);
   }

   public static Naming getHANamingServerForPartition(String partitionName)
   {
      return (Naming) haServers.get(partitionName);
   }

   public static Naming localServer;

   // Attributes ----------------------------------------------------
   Naming naming;
   Hashtable env;
   Name prefix;

   NameParser parser = new NamingParser();
   
   // Static --------------------------------------------------------
   
   // Cache of naming server stubs
   // This is a critical optimization in the case where new InitialContext
   // is performed often. The server stub will be shared between all those
   // calls, which will improve performance.
   // Weak references are used so if no contexts use a particular server
   // it will be removed from the cache.
   static HashMap cachedServers = new HashMap();

   static void addServer(String name, Naming server)
   {
      // Add server to map
      synchronized (NamingContext.class)
      {
         cachedServers.put(name, new WeakReference(server));
      }
   }

   static Naming getServer(String host, int port, Hashtable serverEnv)
      throws NamingException
   {
      // Check the server cache for a host:port entry
      String hostKey = host + ":" + port;
      WeakReference ref = (WeakReference) cachedServers.get(hostKey);
      Naming server;
      if (ref != null)
      {
         server = (Naming) ref.get();
         if (server != null)
         {
            // JBAS-4622. Ensure the env for the request has the
            // hostKey so we can remove the cache entry if there is a failure
            serverEnv.put("hostKey", hostKey);
            return server;
         }
      }

      // Server not found; add it to cache
      try
      {
         SocketFactory factory = loadSocketFactory(serverEnv);
         Socket s;

         try
         {
            InetAddress localAddr = null;
            int localPort = 0;
            String localAddrStr = (String) serverEnv.get(JNP_LOCAL_ADDRESS);
            String localPortStr = (String) serverEnv.get(JNP_LOCAL_PORT);
            if (localAddrStr != null)
               localAddr = InetAddress.getByName(localAddrStr);
            if (localPortStr != null)
               localPort = Integer.parseInt(localPortStr);
            s = factory.createSocket(host, port, localAddr, localPort);
         }
         catch (IOException e)
         {
            NamingException ex = new ServiceUnavailableException("Failed to connect to server " + hostKey);
            ex.setRootCause(e);
            throw ex;
         }

         // Get stub from naming server
         BufferedInputStream bis = new BufferedInputStream(s.getInputStream());
         ObjectInputStream in = new ObjectInputStream(bis);
         MarshalledObject stub = (MarshalledObject) in.readObject();
         server = (Naming) stub.get();
         s.close();

         // Add it to cache
         addServer(hostKey, server);
         serverEnv.put("hostKey", hostKey);

         return server;
      }
      catch (IOException e)
      {
         NamingException ex = new CommunicationException("Failed to retrieve stub from server " + hostKey);
         ex.setRootCause(e);
         throw ex;
      }
      catch (Exception e)
      {
         NamingException ex = new CommunicationException("Failed to connect to server " + hostKey);
         ex.setRootCause(e);
         throw ex;
      }
   }

   /**
    * Create a SocketFactory based on the JNP_SOCKET_FACTORY property in the
    * given env. If JNP_SOCKET_FACTORY is not specified default to the
    * TimedSocketFactory.
    */
   static SocketFactory loadSocketFactory(Hashtable serverEnv)
      throws ClassNotFoundException, IllegalAccessException,
      InstantiationException, InvocationTargetException
   {
      SocketFactory factory = null;

      // Get the socket factory classname
      String socketFactoryName = (String) serverEnv.get(JNP_SOCKET_FACTORY);
      if (socketFactoryName == null ||
         socketFactoryName.equals(TimedSocketFactory.class.getName()))
      {
         factory = new TimedSocketFactory(serverEnv);
         return factory;
      }

      /* Create the socket factory. Look for a ctor that accepts a
       Hashtable and if not found use the default ctor.
       */
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      Class factoryClass = loader.loadClass(socketFactoryName);
      try
      {
         Class[] ctorSig = {Hashtable.class};
         Constructor ctor = factoryClass.getConstructor(ctorSig);
         Object[] ctorArgs = {serverEnv};
         factory = (SocketFactory) ctor.newInstance(ctorArgs);
      }
      catch (NoSuchMethodException e)
      {
         // Use the default ctor
         factory = (SocketFactory) factoryClass.newInstance();
      }
      return factory;
   }

   static void removeServer(Hashtable serverEnv)
   {
      String host = "localhost";
      int port = 1099;
      
      // Locate naming service
      if (serverEnv.get(Context.PROVIDER_URL) != null)
      {
         String providerURL = (String) serverEnv.get(Context.PROVIDER_URL);

         StringTokenizer tokenizer = new StringTokenizer(providerURL, ", ");
         while (tokenizer.hasMoreElements())
         {
            String url = tokenizer.nextToken();

            try
            {
               // Parse the url into a host:port form, stripping any protocol
               Name urlAsName = new NamingParser().parse(url);
               String server = parseNameForScheme(urlAsName, null);
               if (server != null)
                  url = server;
               int colon = url.indexOf(':');
               if (colon < 0)
               {
                  host = url.trim();
               }
               else
               {
                  host = url.substring(0, colon).trim();
                  try
                  {
                     port = Integer.parseInt(url.substring(colon + 1).trim());
                  }
                  catch (Exception ex)
                  {
                     // Use default;
                  }
               }

               // Remove server from map
               synchronized (NamingContext.class)
               {
                  cachedServers.remove(host + ":" + port);
               }
            }
            catch (NamingException ignored)
            {
            }
         }
      }
      
      // JBAS-4622. Always do this.
      Object hostKey = serverEnv.remove("hostKey");
      if (hostKey != null)
      {
         synchronized (NamingContext.class)
         {
            cachedServers.remove(hostKey);
         }
      }
   }

   /**
    * Called to remove any url scheme atoms and extract the naming service
    * hostname:port information.
    * @param n the name component to the parsed. After returning n will have all
    * scheme related atoms removed.
    * @return the naming service hostname:port information string if name
    *         contained the host information.
    */
   static String parseNameForScheme(Name n, Hashtable nameEnv)
      throws InvalidNameException
   {
      String serverInfo = null;
      if (n.size() > 0)
      {
         String scheme = n.get(0);
         int schemeLength = 0;
         if (scheme.startsWith("java:"))
            schemeLength = 5;
         else if (scheme.startsWith("jnp:"))
            schemeLength = 4;
         else if (scheme.startsWith("jnps:"))
            schemeLength = 5;
         else if (scheme.startsWith("jnp-http:"))
            schemeLength = 9;
         else if (scheme.startsWith("jnp-https:"))
            schemeLength = 10;
         if (schemeLength > 0)
         {
            // Make a copy of the name to avoid 
            n = (Name) n.clone();
            String suffix = scheme.substring(schemeLength);
            if (suffix.length() == 0)
            {
               // Scheme was "url:/..."
               n.remove(0);
               if (n.size() > 1 && n.get(0).equals(""))
               {
                  // Scheme was "url://hostname:port/..."
                  // Get hostname:port value for the naming server
                  serverInfo = n.get(1);
                  n.remove(0);
                  n.remove(0);
                  // If n is a empty atom remove it or else a '/' will result
                  if (n.size() == 1 && n.get(0).length() == 0)
                     n.remove(0);
               }
            }
            else
            {
               // Scheme was "url:foo" -> reinsert "foo"
               n.remove(0);
               n.add(0, suffix);
            }
            if (nameEnv != null)
               nameEnv.put(JNP_PARSED_NAME, n);
         }
      }
      return serverInfo;
   }

   public static void setLocal(Naming server)
   {
      localServer = server;
   }

   // Constructors --------------------------------------------------
   public NamingContext(Hashtable e, Name baseName, Naming server)
      throws NamingException
   {
      if (baseName == null)
         this.prefix = parser.parse("");
      else
         this.prefix = baseName;

      if (e != null)
         this.env = (Hashtable) e.clone();
      else
         this.env = new Hashtable();

      this.naming = server;
   }

   // Public --------------------------------------------------------
   public Naming getNaming()
   {
      return this.naming;
   }

   public void setNaming(Naming server)
   {
      this.naming = server;
   }

   // Context implementation ----------------------------------------
   public void rebind(String name, Object obj)
      throws NamingException
   {
      rebind(getNameParser(name).parse(name), obj);
   }

   public void rebind(Name name, Object obj)
      throws NamingException
   {
      Hashtable refEnv = getEnv(name);
      checkRef(refEnv);
      Name parsedName = (Name) refEnv.get(JNP_PARSED_NAME);
      if (parsedName != null)
         name = parsedName;

      // Allow state factories to change the stored object
      obj = getStateToBind(obj, name, refEnv);

      try
      {
         String className = null;
         
         // Referenceable
         if (obj instanceof Referenceable)
            obj = ((Referenceable) obj).getReference();

         if (!(obj instanceof Reference))
         {
            if( obj != null )
               className = obj.getClass().getName();
            // Normal object - serialize using a MarshalledValuePair
            obj = new MarshalledValuePair(obj);
         }
         else
         {
            className = ((Reference) obj).getClassName();
         }
         try
         {
            naming.rebind(getAbsoluteName(name), obj, className);
         }
         catch (RemoteException re)
         {
            // Check for JBAS-4574.
            if (handleStaleNamingStub(re, refEnv))
            {
               // try again with new naming stub                  
               naming.rebind(getAbsoluteName(name), obj, className);
            }
            else
            {
               // Not JBAS-4574. Throw exception and let outer logic handle it.
               throw re;
            }            
         }
      }
      catch (CannotProceedException cpe)
      {
         cpe.setEnvironment(refEnv);
         Context cctx = NamingManager.getContinuationContext(cpe);
         cctx.rebind(cpe.getRemainingName(), obj);
      }
      catch (IOException e)
      {
         naming = null;
         removeServer(refEnv);
         NamingException ex = new CommunicationException();
         ex.setRootCause(e);
         throw ex;
      }
   }

   public void bind(String name, Object obj)
      throws NamingException
   {
      bind(getNameParser(name).parse(name), obj);
   }

   public void bind(Name name, Object obj)
      throws NamingException
   {
      Hashtable refEnv = getEnv(name);
      checkRef(refEnv);
      Name parsedName = (Name) refEnv.get(JNP_PARSED_NAME);
      if (parsedName != null)
         name = parsedName;

      // Allow state factories to change the stored object
      obj = getStateToBind(obj, name, refEnv);

      try
      {
         String className = null;
         
         // Referenceable
         if (obj instanceof Referenceable)
            obj = ((Referenceable) obj).getReference();

         if (!(obj instanceof Reference))
         {
            if( obj != null )
               className = obj.getClass().getName();

            // Normal object - serialize using a MarshalledValuePair
            obj = new MarshalledValuePair(obj);
         }
         else
         {
            className = ((Reference) obj).getClassName();
         }
         name = getAbsoluteName(name);
         
         try
         {
            naming.bind(name, obj, className);
         }
         catch (RemoteException re)
         {
            // Check for JBAS-4574.
            if (handleStaleNamingStub(re, refEnv))
            {
               // try again with new naming stub                  
               naming.bind(name, obj, className);
            }
            else
            {
               // Not JBAS-4574. Throw exception and let outer logic handle it.
               throw re;
            }            
         }
      }
      catch (CannotProceedException cpe)
      {
         cpe.setEnvironment(refEnv);
         Context cctx = NamingManager.getContinuationContext(cpe);
         cctx.bind(cpe.getRemainingName(), obj);
      }
      catch (IOException e)
      {
         naming = null;
         removeServer(refEnv);
         NamingException ex = new CommunicationException();
         ex.setRootCause(e);
         throw ex;
      }
   }

   public Object lookup(String name)
      throws NamingException
   {
      return lookup(getNameParser(name).parse(name));
   }

   public Object lookup(Name name)
      throws NamingException
   {
      Hashtable refEnv = getEnv(name);
      checkRef(refEnv);
      Name parsedName = (Name) refEnv.get(JNP_PARSED_NAME);
      if (parsedName != null)
         name = parsedName;

      // Empty?
      if (name.isEmpty())
         return new NamingContext(refEnv, prefix, naming);

      try
      {
         int maxTries = 1;
         try
         {
            String n = (String) refEnv.get(JNP_MAX_RETRIES);
            if( n != null )
               maxTries = Integer.parseInt(n);
            if( maxTries <= 0 )
               maxTries = 1;
         }
         catch(Exception e)
         {
            log.debug("Failed to get JNP_MAX_RETRIES, using 1", e);
         }
         Name n = getAbsoluteName(name);
         Object res = null;
         boolean trace = log.isTraceEnabled();
         for (int i = 0; i < maxTries; i++)
         {
            try
            {
               try
               {
                  res = naming.lookup(n);
               }
               catch (RemoteException re)
               {
                  // Check for JBAS-4574.
                  if (handleStaleNamingStub(re, refEnv))
                  {
                     // try again with new naming stub                  
                     res = naming.lookup(n);
                  }
                  else
                  {
                     // Not JBAS-4574. Throw exception and let outer logic handle it.
                     throw re;
                  }
               }
               // If we got here, we succeeded, so break the loop
               break;
            }
            catch (ConnectException ce)
            {
               int retries = maxTries - i - 1;
               if( trace )
                  log.trace("Connect failed, retry count: "+retries, ce);
               // We may overload server so sleep and retry
               if (retries > 0)
               {
                  try
                  {
                     Thread.sleep(1);
                  }
                  catch (InterruptedException ignored)
                  {
                  }
                  continue;
               }
               // Throw the exception to flush the bad server
               throw ce;
            }
         }
         if (res instanceof MarshalledValuePair)
         {
            MarshalledValuePair mvp = (MarshalledValuePair) res;
            Object storedObj = mvp.get();
            return getObjectInstanceWrapFailure(storedObj, name, refEnv);
         }
         else if (res instanceof MarshalledObject)
         {
            MarshalledObject mo = (MarshalledObject) res;
            return mo.get();
         }
         else if (res instanceof Context)
         {
            // Add env
            Enumeration keys = refEnv.keys();
            while (keys.hasMoreElements())
            {
               String key = (String) keys.nextElement();
               ((Context) res).addToEnvironment(key, refEnv.get(key));
            }
            return res;
         }
         else if (res instanceof ResolveResult)
         {
            // Dereference partial result
            ResolveResult rr = (ResolveResult) res;
            Object resolveRes = rr.getResolvedObj();
            Object context;
            Object instanceID;

            if (resolveRes instanceof LinkRef)
            {
               context = resolveLink(resolveRes, null);
               instanceID = ((LinkRef) resolveRes).getLinkName();
            }
            else
            {
               context = getObjectInstanceWrapFailure(resolveRes, name, refEnv);
               instanceID = context;
            }

            if ((context instanceof Context) == false)
            {
               throw new NotContextException(instanceID + " is not a Context");
            }
            Context ncontext = (Context) context;
            return ncontext.lookup(rr.getRemainingName());
         }
         else if (res instanceof LinkRef)
         {
            // Dereference link
            res = resolveLink(res, refEnv);
         }
         else if (res instanceof Reference)
         {
            // Dereference object
            res = getObjectInstanceWrapFailure(res, name, refEnv);
            if (res instanceof LinkRef)
               res = resolveLink(res, refEnv);
         }

         return res;
      }
      catch (CannotProceedException cpe)
      {
         cpe.setEnvironment(refEnv);
         Context cctx = NamingManager.getContinuationContext(cpe);
         return cctx.lookup(cpe.getRemainingName());
      }
      catch (IOException e)
      {
         naming = null;
         removeServer(refEnv);
         NamingException ex = new CommunicationException();
         ex.setRootCause(e);
         throw ex;
      }
      catch (ClassNotFoundException e)
      {
         NamingException ex = new CommunicationException();
         ex.setRootCause(e);
         throw ex;
      }
   }

   public void unbind(String name)
      throws NamingException
   {
      unbind(getNameParser(name).parse(name));
   }


   public void unbind(Name name)
      throws NamingException
   {
      Hashtable refEnv = getEnv(name);
      checkRef(refEnv);
      Name parsedName = (Name) refEnv.get(JNP_PARSED_NAME);
      if (parsedName != null)
         name = parsedName;

      try
      {
         try
         {
            naming.unbind(getAbsoluteName(name));
         }
         catch (RemoteException re)
         {
            // Check for JBAS-4574.
            if (handleStaleNamingStub(re, refEnv))
            {
               // try again with new naming stub                  
               naming.unbind(getAbsoluteName(name));
            }
            else
            {
               // Not JBAS-4574. Throw exception and let outer logic handle it.
               throw re;
            }             
         }
      }
      catch (CannotProceedException cpe)
      {
         cpe.setEnvironment(refEnv);
         Context cctx = NamingManager.getContinuationContext(cpe);
         cctx.unbind(cpe.getRemainingName());
      }
      catch (IOException e)
      {
         naming = null;
         removeServer(refEnv);
         NamingException ex = new CommunicationException();
         ex.setRootCause(e);
         throw ex;
      }
   }

   public void rename(String oldname, String newname)
      throws NamingException
   {
      rename(getNameParser(oldname).parse(oldname), getNameParser(newname).parse(newname));
   }

   public void rename(Name oldName, Name newName)
      throws NamingException
   {
      bind(newName, lookup(oldName));
      unbind(oldName);
   }

   public NamingEnumeration list(String name)
      throws NamingException
   {
      return list(getNameParser(name).parse(name));
   }

   public NamingEnumeration list(Name name)
      throws NamingException
   {
      Hashtable refEnv = getEnv(name);
      checkRef(refEnv);
      Name parsedName = (Name) refEnv.get(JNP_PARSED_NAME);
      if (parsedName != null)
         name = parsedName;

      try
      {
         Collection c = null;
         try
         {
            c = naming.list(getAbsoluteName(name));
         }
         catch (RemoteException re)
         {
            // Check for JBAS-4574.
            if (handleStaleNamingStub(re, refEnv))
            {
               // try again with new naming stub                  
               c = naming.list(getAbsoluteName(name));
            }
            else
            {
               // Not JBAS-4574. Throw exception and let outer logic handle it.
               throw re;
            }            
         }
         return new NamingEnumerationImpl(c);
      }
      catch (CannotProceedException cpe)
      {
         cpe.setEnvironment(refEnv);
         Context cctx = NamingManager.getContinuationContext(cpe);
         return cctx.list(cpe.getRemainingName());
      }
      catch (IOException e)
      {
         naming = null;
         removeServer(refEnv);
         NamingException ex = new CommunicationException();
         ex.setRootCause(e);
         throw ex;
      }
   }

   public NamingEnumeration listBindings(String name)
      throws NamingException
   {
      return listBindings(getNameParser(name).parse(name));
   }

   public NamingEnumeration listBindings(Name name)
      throws NamingException
   {
      Hashtable refEnv = getEnv(name);
      checkRef(refEnv);
      Name parsedName = (Name) refEnv.get(JNP_PARSED_NAME);
      if (parsedName != null)
         name = parsedName;

      try
      {
         // Get list
         Collection bindings = null;
         try
         {
            // Get list
            bindings = naming.listBindings(getAbsoluteName(name));
         }
         catch (RemoteException re)
         {
            // Check for JBAS-4574.
            if (handleStaleNamingStub(re, refEnv))
            {
               // try again with new naming stub                  
               bindings = naming.listBindings(getAbsoluteName(name));
            }
            else
            {
               // Not JBAS-4574. Throw exception and let outer logic handle it.
               throw re;
            }            
         }
         Collection realBindings = new ArrayList(bindings.size());
         
         // Convert marshalled objects
         Iterator i = bindings.iterator();
         while (i.hasNext())
         {
            Binding binding = (Binding) i.next();
            Object obj = binding.getObject();
            if (obj instanceof MarshalledValuePair)
            {
               try
               {
                  obj = ((MarshalledValuePair) obj).get();
               }
               catch (ClassNotFoundException e)
               {
                  NamingException ex = new CommunicationException();
                  ex.setRootCause(e);
                  throw ex;
               }
            }
            else if (obj instanceof MarshalledObject)
            {
               try
               {
                  obj = ((MarshalledObject) obj).get();
               }
               catch (ClassNotFoundException e)
               {
                  NamingException ex = new CommunicationException();
                  ex.setRootCause(e);
                  throw ex;
               }
            }
            realBindings.add(new Binding(binding.getName(), binding.getClassName(), obj));
         }
         
         // Return transformed list of bindings
         return new NamingEnumerationImpl(realBindings);
      }
      catch (CannotProceedException cpe)
      {
         cpe.setEnvironment(refEnv);
         Context cctx = NamingManager.getContinuationContext(cpe);
         return cctx.listBindings(cpe.getRemainingName());
      }
      catch (IOException e)
      {
         naming = null;
         removeServer(refEnv);
         NamingException ex = new CommunicationException();
         ex.setRootCause(e);
         throw ex;
      }
   }

   public String composeName(String name, String prefix)
      throws NamingException
   {
      Name result = composeName(parser.parse(name),
         parser.parse(prefix));
      return result.toString();
   }

   public Name composeName(Name name, Name prefix)
      throws NamingException
   {
      Name result = (Name) (prefix.clone());
      result.addAll(name);
      return result;
   }

   public NameParser getNameParser(String name)
      throws NamingException
   {
      return parser;
   }

   public NameParser getNameParser(Name name)
      throws NamingException
   {
      return getNameParser(name.toString());
   }

   public Context createSubcontext(String name)
      throws NamingException
   {
      return createSubcontext(getNameParser(name).parse(name));
   }

   public Context createSubcontext(Name name)
      throws NamingException
   {
      if (name.size() == 0)
         throw new InvalidNameException("Cannot pass an empty name to createSubcontext");

      Hashtable refEnv = getEnv(name);
      checkRef(refEnv);
      Name parsedName = (Name) refEnv.get(JNP_PARSED_NAME);
      if (parsedName != null)
         name = parsedName;

      try
      {
         name = getAbsoluteName(name);
         try
         {
            return naming.createSubcontext(name);
         }
         catch (RemoteException re)
         {
            // Check for JBAS-4574.
            if (handleStaleNamingStub(re, refEnv))
            {
               // try again with new naming stub                  
               return naming.createSubcontext(name);
            }
            else
            {
               // Not JBAS-4574. Throw exception and let outer logic handle it.
               throw re;
            }            
         }
      }
      catch (CannotProceedException cpe)
      {
         cpe.setEnvironment(refEnv);
         Context cctx = NamingManager.getContinuationContext(cpe);
         return cctx.createSubcontext(cpe.getRemainingName());
      }
      catch (IOException e)
      {
         naming = null;
         removeServer(refEnv);
         NamingException ex = new CommunicationException();
         ex.setRootCause(e);
         throw ex;
      }
   }

   public Object addToEnvironment(String propName, Object propVal)
      throws NamingException
   {
      Object old = env.get(propName);
      env.put(propName, propVal);
      return old;
   }

   public Object removeFromEnvironment(String propName)
      throws NamingException
   {
      return env.remove(propName);
   }

   public Hashtable getEnvironment()
      throws NamingException
   {
      return env;
   }

   public void close()
      throws NamingException
   {
      env = null;
      naming = null;
   }

   public String getNameInNamespace()
      throws NamingException
   {
      return prefix.toString();
   }

   public void destroySubcontext(String name)
      throws NamingException
   {
      destroySubcontext(getNameParser(name).parse(name));
   }

   public void destroySubcontext(Name name)
      throws NamingException
   {
      if (!list(name).hasMore())
      {
         unbind(name);
      }
      else
         throw new ContextNotEmptyException();
   }

   public Object lookupLink(String name)
      throws NamingException
   {
      return lookupLink(getNameParser(name).parse(name));
   }

   /**
    * Lookup the object referred to by name but don't dereferrence the final
    * component. This really just involves returning the raw value returned by
    * the Naming.lookup() method.
    * @return the raw object bound under name.
    */
   public Object lookupLink(Name name)
      throws NamingException
   {
      Hashtable refEnv = getEnv(name);
      checkRef(refEnv);
      Name parsedName = (Name) refEnv.get(JNP_PARSED_NAME);
      if (parsedName != null)
         name = parsedName;

      if (name.isEmpty())
         return lookup(name);

      Object link = null;
      try
      {
         Name n = getAbsoluteName(name);
         try
         {
            link = naming.lookup(n);
         }
         catch (RemoteException re)
         {
            // Check for JBAS-4574.
            if (handleStaleNamingStub(re, refEnv))
            {
               // try again with new naming stub                  
               link = naming.lookup(n);
            }
            else
            {
               // Not JBAS-4574. Throw exception and let outer logic handle it.
               throw re;
            }            
         }
         if (!(link instanceof LinkRef) && link instanceof Reference)
            link = getObjectInstance(link, name, null);
         ;
      }
      catch (IOException e)
      {
         naming = null;
         removeServer(refEnv);
         NamingException ex = new CommunicationException();
         ex.setRootCause(e);
         throw ex;
      }
      catch (Exception e)
      {
         NamingException ex = new NamingException("Could not lookup link");
         ex.setRemainingName(name);
         ex.setRootCause(e);
         throw ex;
      }
      return link;
   }

   // Begin EventContext methods
   public void addNamingListener(Name target, int scope, NamingListener l)
      throws NamingException
   {
      // TODO Auto-generated method stub
      throw new UnsupportedOperationException("This is not supported currently");
   }

   public void addNamingListener(String target, int scope, NamingListener l)
      throws NamingException
   {
      // TODO Auto-generated method stub
      throw new UnsupportedOperationException("This is not supported currently");      
   }

   public void removeNamingListener(NamingListener l)
      throws NamingException
   {
      // TODO Auto-generated method stub
      throw new UnsupportedOperationException("This is not supported currently");
   }

   public boolean targetMustExist()
      throws NamingException
   {
      // TODO Auto-generated method stub
      return false;
   }
   // End EventContext methods

   protected Object resolveLink(Object res, Hashtable refEnv)
      throws NamingException
   {
      Object linkResult = null;
      try
      {
         LinkRef link = (LinkRef) res;
         String ref = link.getLinkName();
         if (ref.startsWith("./"))
            linkResult = lookup(ref.substring(2));
         else if (refEnv != null)
            linkResult = new InitialContext(refEnv).lookup(ref);
         else
            linkResult = new InitialContext().lookup(ref);
      }
      catch (Exception e)
      {
         NamingException ex = new NamingException("Could not dereference object");
         ex.setRootCause(e);
         throw ex;
      }
      return linkResult;
   }

   // Private -------------------------------------------------------

   /**
    * Determine the form of the name to pass to the NamingManager operations.
    * This is supposed to be a context relative name according to the javaodcs
    * for NamingManager, but historically the absolute name of the target
    * context has been passed in. 
    * 
    * @param env - the env of NamingContext that op was called on
    * @return true if the legacy and technically incorrect absolute name should
    * be used, false if the context relative name should be used.
    */ 
   private boolean useAbsoluteName(Hashtable env)
   {
      if (env == null)
         return true;
      String useRelativeName = (String) env.get(JNP_USE_RELATIVE_NAME);
      return Boolean.valueOf(useRelativeName) == Boolean.FALSE;
   }

   /**
    * Use the NamingManager.getStateToBind to obtain the actual object to bind
    * into jndi.
    * @param obj - the value passed to bind/rebind
    * @param name - the name passed to bind/rebind
    * @param env - the env of NamingContext that bind/rebind was called on
    * @return the object to bind to the naming server
    * @throws NamingException
    */
   private Object getStateToBind(Object obj, Name name, Hashtable env)
      throws NamingException
   {
      if (useAbsoluteName(env))
         name = getAbsoluteName(name);
      return NamingManager.getStateToBind(obj, name, this, env);
   }

   /**
    * Use the NamingManager.getObjectInstance to resolve the raw object obtained
    * from the naming server.
    * @param obj - raw value obtained from the naming server
    * @param name - the name passed to the lookup op
    * @param env - the env of NamingContext that the op was called on
    * @return the fully resolved object
    * @throws Exception
    */
   private Object getObjectInstance(Object obj, Name name, Hashtable env)
      throws Exception
   {
      if (useAbsoluteName(env))
         name = getAbsoluteName(name);
      return NamingManager.getObjectInstance(obj, name, this, env);
   }

   /**
    * Resolve the final object and wrap any non-NamingException errors in a
    * NamingException with the cause passed as the root cause.
    * @param obj - raw value obtained from the naming server
    * @param name - the name passed to the lookup op
    * @param env - the env of NamingContext that the op was called on
    * @return the fully resolved object
    * @throws NamingException
    */
   private Object getObjectInstanceWrapFailure(Object obj, Name name, Hashtable env)
      throws NamingException
   {
      try
      {
         return getObjectInstance(obj, name, env);
      }
      catch (NamingException e)
      {
         throw e;
      }
      catch (Exception e)
      {
         NamingException ex = new NamingException("Could not dereference object");
         ex.setRootCause(e);
         throw ex;
      }
   }

   /**
    * This methods sends a broadcast message on the network and asks and HA-JNDI
    * server to sent it the HA-JNDI stub
    */
   private Naming discoverServer(Hashtable serverEnv) throws NamingException
   {
      boolean trace = log.isTraceEnabled();
      // Check if discovery should be done
      String disableDiscovery = (String) serverEnv.get(JNP_DISABLE_DISCOVERY);
      if (Boolean.valueOf(disableDiscovery) == Boolean.TRUE)
      {
         if (trace)
            log.trace("Skipping discovery due to disable flag");
         return null;
      }
      
      // we first try to discover the server locally
      //
      String partitionName = (String) serverEnv.get(JNP_PARTITION_NAME);
      Naming server = null;
      if (partitionName != null)
      {
         server = getHANamingServerForPartition(partitionName);
         if (server != null)
            return server;
      }
      
      // We next broadcast a HelloWorld datagram (multicast)
      // Any listening server will answer with its IP address:port in another datagram
      // we will then use this to make a standard "lookup"
      //
      MulticastSocket s = null;
      InetAddress iaGroup = null;
      try
      {
         String group = DEFAULT_DISCOVERY_GROUP_ADDRESS;
         int port = DEFAULT_DISCOVERY_GROUP_PORT;
         int timeout = DEFAULT_DISCOVERY_TIMEOUT;
         int ttl = 16;

         String discoveryGroup = (String) serverEnv.get(JNP_DISCOVERY_GROUP);
         if (discoveryGroup != null)
            group = discoveryGroup;

         String discoveryTTL = (String) serverEnv.get(JNP_DISCOVERY_TTL);
         if(discoveryTTL != null)
            ttl = Integer.parseInt(discoveryTTL);

         String discoveryTimeout = (String) serverEnv.get(JNP_DISCOVERY_TIMEOUT);
         if (discoveryTimeout == null)
         {
            // Check the old property name
            discoveryTimeout = (String) serverEnv.get("DISCOVERY_TIMEOUT");
         }
         if (discoveryTimeout != null && !discoveryTimeout.equals(""))
            timeout = Integer.parseInt(discoveryTimeout);

         String discoveryGroupPort = (String) serverEnv.get(JNP_DISCOVERY_PORT);
         if (discoveryGroupPort == null)
         {
            // Check the old property name
            discoveryGroupPort = (String) serverEnv.get("DISCOVERY_GROUP");
         }
         if (discoveryGroupPort != null && !discoveryGroupPort.equals(""))
         {
            int colon = discoveryGroupPort.indexOf(':');
            if (colon < 0)
            {
               // No group given, just the port
               try
               {
                  port = Integer.parseInt(discoveryGroupPort);
               }
               catch (Exception ex)
               {
                  log.warn("Failed to parse port: " + discoveryGroupPort, ex);
               }
            }
            else
            {
               // The old group:port syntax was given
               group = discoveryGroupPort.substring(0, colon);
               String portStr = discoveryGroupPort.substring(colon + 1);
               try
               {
                  port = Integer.parseInt(portStr);
               }
               catch (Exception ex)
               {
                  log.warn("Failed to parse port: " + portStr, ex);
               }
            }
         }

         iaGroup = InetAddress.getByName(group);
         String localAddrStr = (String) serverEnv.get(JNP_LOCAL_ADDRESS);
         String localPortStr = (String) serverEnv.get(JNP_LOCAL_PORT);
         int localPort = 0;
         if (localPortStr != null)
            localPort = Integer.parseInt(localPortStr);
         if (localAddrStr != null)
         {
            InetSocketAddress localAddr = new InetSocketAddress(localAddrStr, localPort);
            s = new MulticastSocket(localAddr);
         }
         else
         {
            s = new MulticastSocket(localPort);
         }
         s.setSoTimeout(timeout);
         s.setTimeToLive(ttl);
         if(log.isTraceEnabled())
            log.trace("TTL on multicast discovery socket is " + ttl);
         s.joinGroup(iaGroup);
         if (trace)
            log.trace("MulticastSocket: " + s);
         DatagramPacket packet;
         // Send a request optionally restricted to a cluster partition
         StringBuffer data = new StringBuffer("GET_ADDRESS");
         if (partitionName != null)
            data.append(":" + partitionName);
         byte[] buf = data.toString().getBytes();
         packet = new DatagramPacket(buf, buf.length, iaGroup, port);
         if (trace)
            log.trace("Sending discovery packet(" + data + ") to: " + iaGroup + ":" + port);
         s.send(packet);
         // Look for a reply
         // IP address + port number = 128.128.128.128:65535 => (12+3) + 1 + (5) = 21

         buf = new byte[50];
         packet = new DatagramPacket(buf, buf.length);
         s.receive(packet);
         String myServer = new String(packet.getData()).trim();
         if (trace)
            log.trace("Received answer packet: " + myServer);
         while (myServer != null && myServer.startsWith("GET_ADDRESS"))
         {
            Arrays.fill(buf, (byte) 0);
            packet.setLength(buf.length);
            s.receive(packet);
            byte[] reply = packet.getData();
            myServer = new String(reply).trim();
            if (trace)
               log.trace("Received answer packet: " + myServer);
         }
         String serverHost;
         int serverPort;

         int colon = myServer.indexOf(':');
         if (colon >= 0)
         {
            serverHost = myServer.substring(0, colon);
            serverPort = Integer.valueOf(myServer.substring(colon + 1)).intValue();
            server = getServer(serverHost, serverPort, serverEnv);
         }
         return server;
      }
      catch (IOException e)
      {
         if (trace)
            log.trace("Discovery failed", e);
         NamingException ex = new CommunicationException(e.getMessage());
         ex.setRootCause(e);
         throw ex;
      }
      finally
      {
         try
         {
            if (s != null)
               s.leaveGroup(iaGroup);
         }
         catch (Exception ignore)
         {
         }
         try
         {
            if (s != null)
               s.close();
         }
         catch (Exception ignore)
         {
         }
      }
   }

   private void checkRef(Hashtable refEnv)
      throws NamingException
   {
      if (naming == null)
      {
         String host = "localhost";
         int port = 1099;
         Exception serverEx = null;
         
         // Locate first available naming service
         String urls = (String) refEnv.get(Context.PROVIDER_URL);
         if (urls != null && urls.length() > 0)
         {
            StringTokenizer tokenizer = new StringTokenizer(urls, ",");

            while (naming == null && tokenizer.hasMoreElements())
            {
               String url = tokenizer.nextToken();
               // Parse the url into a host:port form, stripping any protocol
               Name urlAsName = getNameParser("").parse(url);
               String server = parseNameForScheme(urlAsName, null);
               if (server != null)
                  url = server;
               int colon = url.indexOf(':');
               if (colon < 0)
               {
                  host = url;
               }
               else
               {
                  host = url.substring(0, colon).trim();
                  try
                  {
                     port = Integer.parseInt(url.substring(colon + 1).trim());
                  }
                  catch (Exception ex)
                  {
                     // Use default;
                  }
               }
               try
               {
                  // Get server from cache
                  naming = getServer(host, port, refEnv);
               }
               catch (Exception e)
               {
                  serverEx = e;
                  log.debug("Failed to connect to " + host + ":" + port, e);
               }
            }

            // If there is still no
            Exception discoveryFailure = null;
            if (naming == null)
            {
               try
               {
                  naming = discoverServer(refEnv);
               }
               catch (Exception e)
               {
                  discoveryFailure = e;
                  if (serverEx == null)
                     serverEx = e;
               }
               if (naming == null)
               {
                  StringBuffer buffer = new StringBuffer(50);
                  buffer.append("Could not obtain connection to any of these urls: ").append(urls);
                  if (discoveryFailure != null)
                     buffer.append(" and discovery failed with error: ").append(discoveryFailure);
                  CommunicationException ce = new CommunicationException(buffer.toString());
                  ce.setRootCause(serverEx);
                  throw ce;
               }
            }
         }
         else
         {
            // If we are in a clustering scenario, the client code may request a context
            // for a *specific* HA-JNDI service (i.e. linked to a *specific* partition)
            // EVEN if the lookup is done inside a JBoss VM. For example, a JBoss service
            // may do a lookup on a HA-JNDI service running on another host *without*
            // explicitly providing a PROVIDER_URL but simply by providing a JNP_PARTITON_NAME
            // parameter so that dynamic discovery can be used
            //
            String jnpPartitionName = (String) refEnv.get(JNP_PARTITION_NAME);
            if (jnpPartitionName != null)
            {
               // the client is requesting for a specific partition name
               // 
               naming = discoverServer(refEnv);
               if (naming == null)
                  throw new ConfigurationException
                     ("No valid context could be build for jnp.partitionName=" + jnpPartitionName);
            }
            else
            {
               // Use server in same JVM
               naming = localServer;

               if (naming == null)
               {
                  naming = discoverServer(refEnv);
                  if (naming == null)
                  // Local, but no local JNDI provider found!
                     throw new ConfigurationException("No valid Context.PROVIDER_URL was found");
               }
            }
         }
      }
   }

   private Name getAbsoluteName(Name n)
      throws NamingException
   {
      if (n.isEmpty())
         return composeName(n, prefix);
      else if (n.get(0).toString().equals("")) // Absolute name
         return n.getSuffix(1);
      else // Add prefix
         return composeName(n, prefix);
   }

   private Hashtable getEnv(Name n)
      throws InvalidNameException
   {
      Hashtable nameEnv = env;
      env.remove(JNP_PARSED_NAME);
      String serverInfo = parseNameForScheme(n, nameEnv);
      if (serverInfo != null)
      {
         // Set hostname:port value for the naming server
         nameEnv = (Hashtable) env.clone();
         nameEnv.put(Context.PROVIDER_URL, serverInfo);
      }
      return nameEnv;
   }
   
   /**
    * JBAS-4574. Check if the given exception is because the server has 
    * been restarted while the cached naming stub hasn't been dgc-ed yet. 
    * If yes, we will flush out the naming stub from our cache and
    * acquire a new stub. BW.
    * 
    * @param e  the exception that may be due to a stale stub
    * @param refEnv the naming environment associated with the failed call
    * 
    * @return <code>true</code> if <code>e</code> indicates a stale
    *         naming stub and we were able to succesfully flush the
    *         cache and acquire a new stub; <code>false</code> otherwise.
    */
   private boolean handleStaleNamingStub(Exception e, Hashtable refEnv)
   {
      if (e instanceof NoSuchObjectException
            || e.getCause() instanceof NoSuchObjectException)
      {
         try
         {
            if( log.isTraceEnabled() )
            {
               log.trace("Call failed with NoSuchObjectException, " +
                         "flushing server cache and retrying", e);
            }
            naming = null;
            removeServer(refEnv);
              
            checkRef(refEnv);
            
            return true;
         }
         catch (Exception e1)
         {
            // Just log and return false; let caller continue processing
            // the original exception passed in to this method
            log.error("Caught exception flushing server cache and " +
                      "re-establish naming after exception " + 
                      e.getLocalizedMessage(), e1);
         }
      }
      return false;
   }

   // Inner classes -------------------------------------------------
}
