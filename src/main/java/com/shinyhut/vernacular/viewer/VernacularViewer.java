package com.shinyhut.vernacular.viewer;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import javax.swing.*;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.shinyhut.vernacular.client.VernacularClient;
import com.shinyhut.vernacular.client.VernacularConfig;
import static com.shinyhut.vernacular.client.rendering.ColorDepth.BPP_16_TRUE;
import static java.awt.Toolkit.getDefaultToolkit;
import static java.awt.datatransfer.DataFlavor.stringFlavor;
import static java.lang.Thread.sleep;

public class VernacularViewer implements ConnectionManager, ConnectionStateListener
{
    private static final String OPTION_DB_HOST = "host";
    private static final String OPTION_DB_PORT = "port";
    private static final String OPTION_DB_USER = "user";
    private static final String OPTION_DB_PASS = "pass";
    
    private final List<ConnectionStateListener> connectionStateListeners = new ArrayList<>();
    
    private String initialHost;
    private Integer initialPort;
    private String initialPassword;
    private String initialUsername;
    
    private VernacularConfig config;
    private VernacularClient client;
    
    private ViewerFrame viewerFrame;
    
    private volatile boolean shutdown = false;
    
    public static void main(String[] args)
    {
        final VernacularViewer viewer = new VernacularViewer();
        
        final Options options = new Options();
        options.addOption(Option.builder(OPTION_DB_HOST).argName(OPTION_DB_HOST).desc("host").hasArg(true).required(false).build());
        options.addOption(Option.builder(OPTION_DB_PORT).argName(OPTION_DB_PASS).desc("port").hasArg(true).required(false).build());
        options.addOption(Option.builder(OPTION_DB_USER).argName(OPTION_DB_USER).desc("username").hasArg(true).required(false).build());
        options.addOption(Option.builder(OPTION_DB_PASS).argName(OPTION_DB_PASS).desc("password").hasArg(true).required(false).build());
        
        final CommandLineParser parser = new DefaultParser();
        try
        {
            final CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption(OPTION_DB_HOST))
            {
                viewer.setInitialHost(cmd.getOptionValue(OPTION_DB_HOST));
            }
            if (cmd.hasOption(OPTION_DB_PORT))
            {
                viewer.setInitialPort(Integer.parseInt(cmd.getOptionValue(OPTION_DB_PORT)));
            }
            if (cmd.hasOption(OPTION_DB_USER))
            {
                viewer.setInitialUsername(cmd.getOptionValue(OPTION_DB_USER));
            }
            if (cmd.hasOption(OPTION_DB_PASS))
            {
                viewer.setInitialPassword(cmd.getOptionValue(OPTION_DB_PASS));
            }
        }
        catch (ParseException e)
        {
            System.err.println("Exception parsing cmd line: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
        
        viewer.init();
    }
    
    @Override
    public void connect(String host, int port)
    {
        client.start(host, port);
        if (client.isRunning())
        {
            synchronized (connectionStateListeners)
            {
                final ArrayList<ConnectionStateListener> listenersCopy = new ArrayList<>(connectionStateListeners);
                listenersCopy.forEach(ConnectionStateListener::onConnected);
            }
        }
    }
    
    @Override
    public boolean isConnected()
    {
        return client != null && client.isRunning();
    }
    
    @Override
    public void disconnect()
    {
        if (isConnected())
        {
            client.stop();
            config.setUsernameSupplier(viewerFrame::showUsernameDialog);
            config.setPasswordSupplier(viewerFrame::showPasswordDialog);
            if (!client.isRunning())
            {
                synchronized (connectionStateListeners)
                {
                    final ArrayList<ConnectionStateListener> listenersCopy = new ArrayList<>(connectionStateListeners);
                    listenersCopy.forEach(ConnectionStateListener::onDisconnected);
                }
            }
        }
    }
    
    @Override
    public void onDisconnected()
    {
    }
    
    @Override
    public void onConnected()
    {
        if (viewerFrame == null)
        {
            viewerFrame = new ViewerFrame(config, client, this);
            synchronized (connectionStateListeners)
            {
                connectionStateListeners.add(viewerFrame);
            }
            viewerFrame.initUI();
            startClipboardMonitor();
        }
        SwingUtilities.invokeLater(() -> viewerFrame.setVisible(true));
    }
    
    public String getInitialHost()
    {
        return initialHost;
    }
    
    public void setInitialHost(String initialHost)
    {
        this.initialHost = initialHost;
    }
    
    public Integer getInitialPort()
    {
        return initialPort;
    }
    
    public void setInitialPort(Integer initialPort)
    {
        this.initialPort = initialPort;
    }
    
    public String getInitialPassword()
    {
        return initialPassword;
    }
    
    public void setInitialPassword(String initialPassword)
    {
        this.initialPassword = initialPassword;
    }
    
    public String getInitialUsername()
    {
        return initialUsername;
    }
    
    public void setInitialUsername(String initialUsername)
    {
        this.initialUsername = initialUsername;
    }
    
    public void init()
    {
        synchronized (connectionStateListeners)
        {
            connectionStateListeners.add(this);
        }
        initialiseVernacularClient();
        
        if (initialHost != null && initialPort != null)
        {
            connect(initialHost, initialPort);
        }
        else
        {
            viewerFrame = new ViewerFrame(config, client, this);
            SwingUtilities.invokeLater(() ->
                                       {
                                           viewerFrame.initUI();
                                           viewerFrame.setVisible(true);
                                           final Optional<Tuple<String, Integer>> stringIntegerTuple = viewerFrame.showConnectDialog();
                                           stringIntegerTuple.ifPresent(integerTuple -> client.start(integerTuple.getObjectOne(), integerTuple.getObjectTwo()));
                                       });
        }
    }
    
    private void initialiseVernacularClient()
    {
        config = new VernacularConfig();
        config.setColorDepth(BPP_16_TRUE);
        config.setErrorListener(e ->
                                {
                                    // showMessageDialog(this, e.getMessage(), "Error", ERROR_MESSAGE);
                                    System.err.println("Error: " + e.getMessage());
                                    e.printStackTrace(System.err);
                                    if (viewerFrame != null)
                                    {
                                        viewerFrame.resetUI();
                                    }
                                });
        config.setUsernameSupplier(this::provideUsername);
        config.setPasswordSupplier(this::providePassword);
        config.setScreenUpdateListener((image) ->
                                       {
                                           if (viewerFrame != null)
                                           {
                                               viewerFrame.renderFrame(image);
                                           }
                                       });
        config.setMousePointerUpdateListener((p, h) ->
                                             {
                                                 if (viewerFrame != null)
                                                 {
                                                     viewerFrame.setCursor(getDefaultToolkit().createCustomCursor(p, h, "vnc"));
                                                 }
                                             });
        config.setBellListener(v -> getDefaultToolkit().beep());
        config.setRemoteClipboardListener(t -> getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(t), null));
        config.setUseLocalMousePointer(() ->
                                       {
                                           if (viewerFrame != null)
                                           {
                                               return viewerFrame.isLocalCursorMenuItemSelected();
                                           }
                                           return false;
                                       });
        client = new VernacularClient(config);
    }
    
    private void startClipboardMonitor()
    {
        Executors.newSingleThreadExecutor().submit(() ->
                                                   {
                                                       final Clipboard clipboard = getDefaultToolkit().getSystemClipboard();
                                                       String lastText = null;
                                                       while (!shutdown)
                                                       {
                                                           try
                                                           {
                                                               if (isConnected())
                                                               {
                                                                   String text = (String)clipboard.getData(stringFlavor);
                                                                   if (text != null && !text.equals(lastText))
                                                                   {
                                                                       client.copyText(text);
                                                                       lastText = text;
                                                                   }
                                                               }
                                                               sleep(100L);
                                                           }
                                                           catch (Exception ignored)
                                                           {
                                                               System.err.println("Exception sleeping: " + ignored.getMessage());
                                                           }
                                                       }
                                                   });
    }
    
    private String provideUsername()
    {
        if (initialUsername != null)
        {
            return initialUsername;
        }
        else if (viewerFrame != null)
        {
            return viewerFrame.showUsernameDialog();
        }
        return "";
    }
    
    private String providePassword()
    {
        if (initialPassword != null)
        {
            return initialPassword;
        }
        else if (viewerFrame != null)
        {
            return viewerFrame.showPasswordDialog();
        }
        return "";
    }
}
