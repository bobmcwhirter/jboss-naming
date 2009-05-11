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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.MarshalledObject;
import java.rmi.Remote;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ServerSocketFactory;

import org.jboss.logging.Logger;
import org.jboss.net.sockets.DefaultSocketFactory;
import org.jboss.util.threadpool.ThreadPool;
import org.jnp.interfaces.MarshalledValuePair;
import org.jnp.interfaces.Naming;

/** 
 * A main() entry point for running the jnp naming service implementation as
 * a standalone process.
 * 
 * @author Rickard Oberg
 * @author Scott.Stark@jboss.org
 * @version $Revision$
 */
public class Main implements MainMBean
{
   // Constants -----------------------------------------------------
   
   // Attributes ----------------------------------------------------
   /** The Naming interface server implementation */
   protected NamingBean theServer;
   protected MarshalledObject serverStub;
   protected boolean isStubExported;
   /** The jnp server socket through which the NamingServer stub is vended */
   protected ServerSocket serverSocket;
   /** An optional custom client socket factory */
   protected RMIClientSocketFactory clientSocketFactory;
   /** An optional custom server socket factory */
   protected RMIServerSocketFactory serverSocketFactory;
   /** An optional custom server socket factory */
   protected ServerSocketFactory jnpServerSocketFactory;
   /** The class name of the optional custom client socket factory */
   protected String clientSocketFactoryName;
   /** The class name of the optional custom server socket factory */
   protected String serverSocketFactoryName;
   /** The class name of the optional custom JNP server socket factory */
   protected String jnpServerSocketFactoryName;
   /** The interface to bind to for the lookup socket. This is useful for
    * multi-homed hosts that want control over which interfaces accept
    * connections */
   protected InetAddress bindAddress;
   protected List<InetAddress> bindAddresses;
   /** The interface to bind to for the Naming RMI server */ 
   protected InetAddress rmiBindAddress;
   /** Should the java.rmi.server.hostname property to rmiBindAddress */
   private boolean enableRmiServerHostname;
   /** The serverSocket listen queue depth */
   protected int backlog = 50;
   /** The jnp protocol listening port. The default is 1099, the same as
    the RMI registry default port. */
   protected int port = 1099;
   /** The RMI port on which the Naming implementation will be exported. The
    default is 0 which means use any available port. */
   protected int rmiPort = 0;
   /** URLs that clients can use to connect to the bootstrap socket */
   protected List<String> bootstrapURLs;
   /** A flag indicating if theServer will be set as the NamingContext.setLocal value */
   protected boolean InstallGlobalService = true;
   /** A flag indicating if theServer will try to use the NamingContext.setLocal value */
   protected boolean UseGlobalService = true;
   protected Logger log;
   /** The thread pool used to handle jnp stub lookup requests */
   private Executor lookupExector;
   /** The exception seen when creating the lookup listening port */
   private Exception lookupListenerException;

   // Static --------------------------------------------------------
   public static void main(String[] args)
      throws Exception
   {
      new Main().start();
   }
 
   // Constructors --------------------------------------------------
   public Main()
   {
      this("org.jboss.naming.Naming");
   }
   public Main(String categoryName)
   {
      // Load properties from properties file
      try
      {
         ClassLoader loader = getClass().getClassLoader();
         InputStream is = loader.getResourceAsStream("jnp.properties");
         System.getProperties().load(is);
      }
      catch (Exception e)
      {
         // Ignore
      }

      // Set configuration from the system properties
      setPort(Integer.getInteger("jnp.port",getPort()).intValue());
      setRmiPort(Integer.getInteger("jnp.rmiPort",getRmiPort()).intValue());
      log = Logger.getLogger(categoryName);
      log.debug("isTraceEnabled: "+log.isTraceEnabled());
   }

   // Public --------------------------------------------------------
   public NamingBean getNamingInfo()
   {
      return theServer;
   }
   /**
    * Set the NamingBean/Naming implementation
    * @param info
    */
   public void setNamingInfo(NamingBean info)
   {
      this.theServer = info;
   }

   @Deprecated
   public void setLookupPool(ThreadPool lookupPool)
   {
      this.lookupExector = new ThreadPoolToExecutor(lookupPool);
   }

   public Executor getLookupExector()
   {
      return lookupExector;
   }
   /**
    * Set the Executor to use for bootstrap socket lookup handling. Note
    * that this must support at least 2 thread to avoid hanging the AcceptHandler
    * accept loop.
    * @param lookupExector - An Executor that supports at least 2 threads
    */
   public void setLookupExector(Executor lookupExector)
   {
      this.lookupExector = lookupExector;
   }

   /**
    * Get any exception seen during the lookup listening port creation
    * @return
    */
   public Exception getLookupListenerException()
   {
      return lookupListenerException;
   }

   /** Get the call by value flag for jndi lookups.
    * 
    * @return true if all lookups are unmarshalled using the caller's TCL,
    *    false if in VM lookups return the value by reference.
    */ 
   public boolean getCallByValue()
   {
      return MarshalledValuePair.getEnableCallByReference() == false;
   }
   /** Set the call by value flag for jndi lookups.
    *
    * @param flag - true if all lookups are unmarshalled using the caller's TCL,
    *    false if in VM lookups return the value by reference.
    */
   public void setCallByValue(boolean flag)
   {
      boolean callByValue = ! flag;
      MarshalledValuePair.setEnableCallByReference(callByValue);
   }

   public Object getNamingProxy()
      throws Exception
   {
      return serverStub.get();
   }
   public void setNamingProxy(Object proxy)
      throws IOException
   {
      serverStub = new MarshalledObject(proxy);
   }

   public void setRmiPort(int p)
   {
      rmiPort = p;
   }
   public int getRmiPort()
   {
      return rmiPort;
   }

   public void setPort(int p)
   {
      port = p;
   }
   public int getPort()
   {
      return port;
   }

   public String getBindAddress()
   {
      String address = null;
      if( bindAddress != null )
         address = bindAddress.getHostAddress();
      return address;
   }
   public void setBindAddress(String host) throws UnknownHostException
   {
      if( host == null || host.length() == 0 )
         bindAddress = null;
      else
         bindAddress = InetAddress.getByName(host);
   }

   
   public List<InetAddress> getBindAddresses()
   {
      return bindAddresses;
   }
   public void setBindAddresses(List<InetAddress> bindAddresses)
   {
      this.bindAddresses = bindAddresses;
   }

   public String getRmiBindAddress()
   {
      String address = null;
      if( rmiBindAddress != null )
         address = rmiBindAddress.getHostAddress();
      return address;
   }
   public void setRmiBindAddress(String host) throws UnknownHostException
   {
      if( host == null || host.length() == 0 )
         rmiBindAddress = null;
      else
         rmiBindAddress = InetAddress.getByName(host);
   }

   
   /**
    * Returns a URL suitable for use as a java.naming.provider.url value in
    * a set of naming environment properties; i.e. one that can be used to 
    * connect to the lookup socket.
    * <p>
    * If there are {@link #getBootstrapURLs() multiple bootstrap URLs}, returns
    * the first one in the list. TODO: that is is pretty arbitrary
    * </p>
    * 
    * @return the URL, or <code>null</code> if no bound lookup socket exists
    */
   public String getBootstrapURL()
   {    
      if (bootstrapURLs != null && bootstrapURLs.size() > 0)
      {
         return bootstrapURLs.get(0);
      }
      return null;
   }
   
   /**
    * Returns a list of URLs suitable for use as a java.naming.provider.url 
    * value in a set of naming environment properties; i.e. ones that can be used to 
    * connect to the lookup socket. There will be one URL per configured
    * {@link #getBindAddresses() bind address}.
    * 
    * @return the URLs, or <code>null</code> if no bound lookup socket exists
    */
   public List<String> getBootstrapURLs()
   {
      return bootstrapURLs;
   }
   
   public boolean isEnableRmiServerHostname()
   {
      return enableRmiServerHostname;
   }
   public void setEnableRmiServerHostname(boolean enableRmiServerHostname)
   {
      this.enableRmiServerHostname = enableRmiServerHostname;
   }

   public int getBacklog()
   {
      return backlog;
   }
   public void setBacklog(int backlog)
   {
      if( backlog <= 0 )
         backlog = 50;
      this.backlog = backlog;
   }

   public boolean getInstallGlobalService()
   {
      return InstallGlobalService;
   }
   public void setInstallGlobalService(boolean flag)
   {
      this.InstallGlobalService = flag;
   }
   public boolean getUseGlobalService()
   {
      return UseGlobalService;
   }
   public void setUseGlobalService(boolean flag)
   {
      this.UseGlobalService = flag;
   }
   
   public String getClientSocketFactory()
   {
      return clientSocketFactoryName;
   }
   public void setClientSocketFactory(String factoryClassName)
      throws ClassNotFoundException, InstantiationException, IllegalAccessException
   {
      this.clientSocketFactoryName = factoryClassName;
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      Class<?> clazz = loader.loadClass(clientSocketFactoryName);
      clientSocketFactory = (RMIClientSocketFactory) clazz.newInstance();
   }
   
   public RMIClientSocketFactory getClientSocketFactoryBean()
   {
      return clientSocketFactory;
   }
   public void setClientSocketFactoryBean(RMIClientSocketFactory factory)
   {
      this.clientSocketFactory = factory;
   }

   public String getServerSocketFactory()
   {
      return serverSocketFactoryName;
   }
   public void setServerSocketFactory(String factoryClassName)
      throws ClassNotFoundException, InstantiationException, IllegalAccessException
   {
      this.serverSocketFactoryName = factoryClassName;
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      Class<?> clazz = loader.loadClass(serverSocketFactoryName);
      serverSocketFactory = (RMIServerSocketFactory) clazz.newInstance();
   }

   public RMIServerSocketFactory getServerSocketFactoryBean()
   {
      return serverSocketFactory;
   }
   public void setServerSocketFactoryBean(RMIServerSocketFactory factory)
   {
      this.serverSocketFactory = factory;
   }

   public String getJNPServerSocketFactory()
   {
      return jnpServerSocketFactoryName;
   }
   public void setJNPServerSocketFactory(String factoryClassName)
      throws ClassNotFoundException, InstantiationException, IllegalAccessException
   {
      this.jnpServerSocketFactoryName = factoryClassName;
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      Class<?> clazz = loader.loadClass(jnpServerSocketFactoryName);
      jnpServerSocketFactory = (ServerSocketFactory) clazz.newInstance();
   }

   public ServerSocketFactory getJNPServerSocketFactoryBean()
   {
      return jnpServerSocketFactory;
   }
   public void setJNPServerSocketFactoryBean(ServerSocketFactory factory)
   {
      this.jnpServerSocketFactory = factory;
   }

   /**
    * Access the 
    */
   public Naming getNamingInstance()
   {
      return theServer.getNamingInstance();
   }

   public void start()
      throws Exception
   {
      log.debug("Begin start");
      // Set the java.rmi.server.hostname to the bind address if not set
      if(rmiBindAddress != null && System.getProperty("java.rmi.server.hostname") == null)
         System.setProperty("java.rmi.server.hostname", rmiBindAddress.getHostAddress());

      // Initialize the custom socket factories with any bind address
      initCustomSocketFactories();
      /* Only export server RMI interface and setup the listening socket if
        the port is >= 0 and an external proxy has not been installed.
        A value < 0 indicates no socket based access
      */
      if( this.serverStub == null && port >= 0 )
      {
         initJnpInvoker();
      }
      // Only bring up the bootstrap listener if there is a naming proxy
      if( this.serverStub != null )
      {
         initBootstrapListener();
      }
      log.debug("End start");
   }

   public void stop()
   {
      try
      {
         // Stop listener and unexport the RMI object
         if( serverSocket != null )
         {
            ServerSocket s = serverSocket;
            serverSocket = null;
            s.close();
         }
         if( isStubExported == true )
            UnicastRemoteObject.unexportObject(theServer.getNamingInstance(), false);
      }
      catch (Exception e)
      {
         log.error("Exception during shutdown", e);
      }
      finally
      {
         bootstrapURLs = null;
      }
   }

   /** This code should be moved to a seperate invoker in the org.jboss.naming
    *package.
    */
   protected void initJnpInvoker() throws IOException
   {
      log.debug("Creating NamingServer stub, theServer="+theServer
         +",rmiPort="+rmiPort
         +",clientSocketFactory="+clientSocketFactory
         +",serverSocketFactory="+serverSocketFactory);
      Naming instance = getNamingInstance();
      Remote stub = UnicastRemoteObject.exportObject(instance,
            rmiPort, clientSocketFactory, serverSocketFactory);
      log.debug("NamingServer stub: "+stub);
      serverStub = new MarshalledObject(stub);
      isStubExported = true;
   }

   /** Bring up the bootstrap lookup port for obtaining the naming service
    * proxy
    */ 
   protected void initBootstrapListener()
   {
      // Start listener
      try
      {
         // Get the default ServerSocketFactory is one was not specified
         if( jnpServerSocketFactory == null )
            jnpServerSocketFactory = ServerSocketFactory.getDefault();
         List<InetAddress> addresses = bindAddresses;
         if(addresses == null)
            addresses = Collections.singletonList(bindAddress);
         // Setup the exectuor with addresses + 1 threads
         if( lookupExector == null  )
         {
            int count = addresses.size() + 1;
            log.debug("Using default newFixedThreadPool("+count+")");
            lookupExector = Executors.newFixedThreadPool(count, BootstrapThreadFactory.getInstance());
         }

         bootstrapURLs = new ArrayList<String>(addresses.size());
         for(InetAddress address : addresses)
         {
            serverSocket = jnpServerSocketFactory.createServerSocket(port, backlog, address);
            // If an anonymous port was specified get the actual port used
            if( port == 0 )
               port = serverSocket.getLocalPort();
            
            bootstrapURLs.add(createBootstrapURL(serverSocket, port));
            
            String msg = "JNDI bootstrap JNP=" + address + ":" + port
               + ", RMI=" + address + ":" + rmiPort
               + ", backlog="+backlog;
   
             if (clientSocketFactory == null)
               msg+= ", no client SocketFactory";
             else
               msg+= ", Client SocketFactory="+clientSocketFactory.toString();
   
             if (serverSocketFactory == null)
               msg+= ", no server SocketFactory";
             else
               msg+= ", Server SocketFactory="+serverSocketFactory.toString();
   
            log.debug(msg);

            AcceptHandler handler = new AcceptHandler();
            lookupExector.execute(handler);
         }
      }
      catch (IOException e)
      {
         lookupListenerException = e;
         log.error("Could not start on port " + port, e);
         return;
      }

   }

   /** 
    * Init the clientSocketFactory, serverSocketFactory using the bind address.
    */
   protected void initCustomSocketFactories()
   {
      // Use either the rmiBindAddress or bindAddress for the RMI service
      InetAddress addr = rmiBindAddress != null ? rmiBindAddress : bindAddress;

      if( clientSocketFactory != null && addr != null )
      {
         // See if the client socket supports setBindAddress(String)
         try
         {
            Class<?> csfClass = clientSocketFactory.getClass();
            Class<?>[] parameterTypes = {String.class};
            Method m = csfClass.getMethod("setBindAddress", parameterTypes);
            Object[] args = {addr.getHostAddress()};
            m.invoke(clientSocketFactory, args);
         }
         catch (NoSuchMethodException e)
         {
            log.warn("Socket factory does not support setBindAddress(String)");
            // Go with default address
         }
         catch (Exception e)
         {
            log.warn("Failed to setBindAddress="+addr+" on socket factory", e);
            // Go with default address
         }
      }

      try
      {
         if (serverSocketFactory == null)
            serverSocketFactory = new DefaultSocketFactory(addr);
         else
         {
            if (addr != null)
            {
               // See if the server socket supports setBindAddress(String)
               try
               {
                  Class<?> ssfClass = serverSocketFactory.getClass();
                  Class<?>[] parameterTypes = {String.class};
                  Method m = ssfClass.getMethod("setBindAddress", parameterTypes);
                  Object[] args = {addr.getHostAddress()};
                  m.invoke(serverSocketFactory, args);
               }
               catch (NoSuchMethodException e)
               {
                  log.warn("Socket factory does not support setBindAddress(String)");
                  // Go with default address
               }
               catch (Exception e)
               {
                  log.warn("Failed to setBindAddress="+addr+" on socket factory", e);
                  // Go with default address
               }
            }
         }
      }
      catch (Exception e)
      {
         log.error("operation failed", e);
         serverSocketFactory = null;
      }
   }
   
   private static String createBootstrapURL(ServerSocket serverSocket, int port)
   {   
      if (serverSocket == null || serverSocket.getInetAddress() == null)
         return null;
      
      // Determine the bootstrap URL
      StringBuilder sb = new StringBuilder("jnp://");
      InetAddress addr = serverSocket.getInetAddress();
      if (addr instanceof Inet6Address)
      {
         sb.append('[');
         sb.append(addr.getHostAddress());
         sb.append(']');
      }
      else
      {
         sb.append(addr.getHostAddress());
      }
      sb.append(':');
      sb.append(port);
      return sb.toString();
      
   }

   private class AcceptHandler implements Runnable
   {
      public void run()
      {
         boolean trace = log.isTraceEnabled();
         while( serverSocket != null )
         {
            Socket socket = null;
            // Accept a connection
            try
            {
               if( trace )
                  log.trace("Enter accept on: "+serverSocket);
               socket = serverSocket.accept();
               if( trace )
                  log.trace("Accepted bootstrap client: "+socket);
               BootstrapRequestHandler handler = new BootstrapRequestHandler(socket);
               lookupExector.execute(handler);
            }
            catch (IOException e)
            {
               // Stopped by normal means
               if (serverSocket == null)
                  return;
               log.error("Naming accept handler stopping", e);
            }
            catch(Throwable e)
            {
               log.error("Unexpected exception during accept", e);
            }
         }
      }
   }

   private class BootstrapRequestHandler implements Runnable
   {
      private Socket socket;
      BootstrapRequestHandler(Socket socket)
      {
         this.socket = socket;
      }
      public void run()
      {
         // Return the naming server stub
         try
         {
            if(log.isTraceEnabled())
               log.trace("BootstrapRequestHandler.run start");
            OutputStream os = socket.getOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(os);
            out.writeObject(serverStub);
            out.close();
            if(log.isTraceEnabled())
               log.trace("BootstrapRequestHandler.run end");
         }
         catch (IOException ex)
         {
            log.debug("Error writing response to " + socket.getInetAddress(), ex);
         }
         finally
         {
            try
            {
               socket.close();
            } catch (IOException e)
            {
            }
         }
      }
   }
   private static class BootstrapThreadFactory implements ThreadFactory
   {
      private static final AtomicInteger tnumber = new AtomicInteger(1);
      static BootstrapThreadFactory instance;
      static synchronized ThreadFactory getInstance()
      {
         if(instance == null)
            instance = new BootstrapThreadFactory();
         return instance;
      }
      public Thread newThread(Runnable r)
      {
         Thread t = new Thread(r, "Naming Bootstrap#"+tnumber.getAndIncrement());
         return t;
      }
   }
}
