package com.shinyhut.vernacular.viewer;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Optional;
import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shinyhut.vernacular.client.VernacularClient;
import com.shinyhut.vernacular.client.VernacularConfig;
import static com.shinyhut.vernacular.client.rendering.ColorDepth.BPP_16_TRUE;
import static com.shinyhut.vernacular.client.rendering.ColorDepth.BPP_24_TRUE;
import static com.shinyhut.vernacular.client.rendering.ColorDepth.BPP_8_INDEXED;
import static java.awt.BorderLayout.CENTER;
import static java.awt.Color.DARK_GRAY;
import static java.awt.Color.LIGHT_GRAY;
import static java.awt.Cursor.getDefaultCursor;
import static java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment;
import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR;
import static java.awt.event.KeyEvent.VK_C;
import static java.awt.event.KeyEvent.VK_D;
import static java.awt.event.KeyEvent.VK_F;
import static java.awt.event.KeyEvent.VK_O;
import static java.awt.event.KeyEvent.VK_X;
import static java.lang.Integer.parseInt;
import static java.lang.Math.min;
import static java.lang.System.exit;
import static javax.swing.JOptionPane.OK_CANCEL_OPTION;
import static javax.swing.JOptionPane.OK_OPTION;
import static javax.swing.JOptionPane.WARNING_MESSAGE;
import static javax.swing.JOptionPane.showConfirmDialog;
import static javax.swing.JOptionPane.showMessageDialog;

public class ViewerFrame extends JFrame implements ConnectionStateListener
{
    private static final Logger LOG = LoggerFactory.getLogger(VernacularViewer.class);
    
    private final VernacularConfig config;
    private final VernacularClient client;
    private final ConnectionManager connectionManager;
    
    private JMenuItem connectMenuItem;
    private JMenuItem disconnectMenuItem;
    
    private JMenuItem bpp8IndexedColorMenuItem;
    private JMenuItem bpp16TrueColorMenuItem;
    private JMenuItem bpp24TrueColorMenuItem;
    private JMenuItem localCursorMenuItem;
    
    private JMenu encodingsMenu;
    private JMenuItem copyRectMenuItem;
    private JMenuItem rreMenuItem;
    private JMenuItem hextileMenuItem;
    private JMenuItem zlibMenuItem;
    
    private Image lastFrame;
    
    private final AncestorListener focusRequester = new AncestorListener()
    {
        @Override
        public void ancestorAdded(AncestorEvent event)
        {
            event.getComponent().requestFocusInWindow();
        }
        
        @Override
        public void ancestorRemoved(AncestorEvent event)
        {
        }
        
        @Override
        public void ancestorMoved(AncestorEvent event)
        {
        }
    };
    
    public ViewerFrame(VernacularConfig vernacularConfig, VernacularClient client, ConnectionManager connectionManager)
    {
        this.config = vernacularConfig;
        this.client = client;
        this.connectionManager = connectionManager;
    }
    
    @Override
    public void onDisconnected()
    {
        resetUI();
    }
    
    @Override
    public void onConnected()
    {
    
    }
    
    public void resetUI()
    {
        setMenuState(false);
        setCursor(getDefaultCursor());
        setSize(1440, 900);
        setLocationRelativeTo(null);
        lastFrame = null;
        repaint();
    }
    
    public Optional<Tuple<String, Integer>> showConnectDialog()
    {
        final JPanel connectDialog = new JPanel();
        final JTextField hostField = new JTextField(20);
        hostField.addAncestorListener(focusRequester);
        final JTextField portField = new JTextField("5900");
        final JLabel hostLabel = new JLabel("Host");
        hostLabel.setLabelFor(hostField);
        final JLabel portLabel = new JLabel("Port");
        portLabel.setLabelFor(hostLabel);
        connectDialog.add(hostLabel);
        connectDialog.add(hostField);
        connectDialog.add(portLabel);
        connectDialog.add(portField);
        
        final int choice = showConfirmDialog(this, connectDialog, "Connect", OK_CANCEL_OPTION);
        if (choice == OK_OPTION)
        {
            final String host = hostField.getText();
            if (host == null || host.isEmpty())
            {
                showMessageDialog(this, "Please enter a valid host", null, WARNING_MESSAGE);
                return Optional.empty();
            }
            int port;
            try
            {
                port = parseInt(portField.getText());
            }
            catch (NumberFormatException e)
            {
                showMessageDialog(this, "Please enter a valid port", null, WARNING_MESSAGE);
                return Optional.empty();
            }
            return Optional.of(new Tuple<>(host, port));
        }
        return Optional.empty();
    }
    
    public String showUsernameDialog()
    {
        String username = "";
        final JPanel usernameDialog = new JPanel();
        final JTextField usernameField = new JTextField(20);
        usernameField.addAncestorListener(focusRequester);
        usernameDialog.add(usernameField);
        final int choice = showConfirmDialog(this, usernameDialog, "Enter Username", OK_CANCEL_OPTION);
        if (choice == OK_OPTION)
        {
            username = usernameField.getText();
        }
        return username;
    }
    
    public String showPasswordDialog()
    {
        String password = "";
        final JPanel passwordDialog = new JPanel();
        final JPasswordField passwordField = new JPasswordField(20);
        passwordField.addAncestorListener(focusRequester);
        passwordDialog.add(passwordField);
        final int choice = showConfirmDialog(this, passwordDialog, "Enter Password", OK_CANCEL_OPTION);
        if (choice == OK_OPTION)
        {
            password = new String(passwordField.getPassword());
        }
        return password;
    }
    
    public void renderFrame(Image frame)
    {
        if (resizeRequired(frame))
        {
            resizeWindow(frame);
        }
        lastFrame = frame;
        repaint();
    }
    
    public void setMenuState(boolean running)
    {
        if (running)
        {
            connectMenuItem.setEnabled(false);
            disconnectMenuItem.setEnabled(true);
            bpp8IndexedColorMenuItem.setEnabled(false);
            bpp16TrueColorMenuItem.setEnabled(false);
            bpp24TrueColorMenuItem.setEnabled(false);
            localCursorMenuItem.setEnabled(false);
            encodingsMenu.setEnabled(false);
        }
        else
        {
            connectMenuItem.setEnabled(true);
            disconnectMenuItem.setEnabled(false);
            bpp8IndexedColorMenuItem.setEnabled(true);
            bpp16TrueColorMenuItem.setEnabled(true);
            bpp24TrueColorMenuItem.setEnabled(true);
            localCursorMenuItem.setEnabled(true);
            encodingsMenu.setEnabled(true);
        }
    }
    
    public boolean isLocalCursorMenuItemSelected()
    {
        if(localCursorMenuItem != null)
        {
            return localCursorMenuItem.isSelected();
        }
        return false;
    }
    
    public void initUI()
    {
        setTitle("Vernacular VNC");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent event)
            {
                connectionManager.disconnect();
                super.windowClosing(event);
            }
        });
        
        addMenu();
        addMouseListeners();
        addKeyListener();
        addDrawingSurface();
        setMenuState(connectionManager.isConnected());
    }
    
    private void addKeyListener()
    {
        setFocusTraversalKeysEnabled(false);
        addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                if (connectionManager.isConnected())
                {
                    client.handleKeyEvent(e);
                }
            }
            
            @Override
            public void keyReleased(KeyEvent e)
            {
                if (connectionManager.isConnected())
                {
                    client.handleKeyEvent(e);
                }
            }
        });
    }
    
    private void addMouseListeners()
    {
        getContentPane().addMouseMotionListener(new MouseMotionListener()
        {
            @Override
            public void mouseDragged(MouseEvent e)
            {
                mouseMoved(e);
            }
            
            @Override
            public void mouseMoved(MouseEvent e)
            {
                if (connectionManager.isConnected())
                {
                    final int scaleX = scaleMouseX(e.getX());
                    final int scaleY = scaleMouseY(e.getY());
                    System.out.printf("Mouse Move: origX=%d, scaleX=%d, origY=%d, scaleY=%d\n", e.getX(), scaleX, e.getY(), scaleY);
                    client.moveMouse(scaleX, scaleY);
                }
            }
        });
        getContentPane().addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                if (connectionManager.isConnected())
                {
                    client.updateMouseButton(e.getButton(), true);
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e)
            {
                if (connectionManager.isConnected())
                {
                    client.updateMouseButton(e.getButton(), false);
                }
            }
        });
        getContentPane().addMouseWheelListener(e ->
                                               {
                                                   if (connectionManager.isConnected())
                                                   {
                                                       int notches = e.getWheelRotation();
                                                       if (notches < 0)
                                                       {
                                                           client.scrollUp();
                                                       }
                                                       else
                                                       {
                                                           client.scrollDown();
                                                       }
                                                   }
                                               });
    }
    
    private void addDrawingSurface()
    {
        add(new JPanel()
        {
            @Override
            public void paintComponent(Graphics g)
            {
                super.paintComponent(g);
                final Graphics2D g2 = (Graphics2D)g;
                final int width = getContentPane().getWidth();
                final int height = getContentPane().getHeight();
                
                if (lastFrame != null)
                {
                    g2.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
                    g2.drawImage(lastFrame, 0, 0, width, height, null);
                }
                else
                {
                    final String message = "No connection. Use \"File > Connect\" to connect to a VNC server.";
                    final int messageWidth = g2.getFontMetrics().stringWidth(message);
                    g2.setColor(DARK_GRAY);
                    g2.fillRect(0, 0, width, height);
                    g2.setColor(LIGHT_GRAY);
                    g2.drawString(message, width / 2 - messageWidth / 2, height / 2);
                }
            }
        }, CENTER);
    }
    
    private void addMenu()
    {
        final JMenuBar menu = new JMenuBar();
        
        final JMenu file = new JMenu("File");
        file.setMnemonic(VK_F);
        
        final JMenu options = new JMenu("Options");
        options.setMnemonic(VK_O);
        
        connectMenuItem = new JMenuItem("Connect");
        connectMenuItem.setMnemonic(VK_C);
        connectMenuItem.addActionListener(event ->
                                          {
                                              final Optional<Tuple<String, Integer>> hostPortTuple = showConnectDialog();
                                              hostPortTuple.ifPresent(stringIntegerTuple -> connectionManager.connect(stringIntegerTuple.getObjectOne(),
                                                                                                                      stringIntegerTuple.getObjectTwo()));
                                          });
        
        disconnectMenuItem = new JMenuItem("Disconnect");
        disconnectMenuItem.setMnemonic(VK_D);
        disconnectMenuItem.setEnabled(false);
        disconnectMenuItem.addActionListener(event -> connectionManager.disconnect());
        
        final ButtonGroup colorDepths = new ButtonGroup();
        
        bpp8IndexedColorMenuItem = new JRadioButtonMenuItem("8-bit Indexed Color");
        bpp16TrueColorMenuItem = new JRadioButtonMenuItem("16-bit True Color", true);
        bpp24TrueColorMenuItem = new JRadioButtonMenuItem("24-bit True Color");
        colorDepths.add(bpp8IndexedColorMenuItem);
        colorDepths.add(bpp16TrueColorMenuItem);
        colorDepths.add(bpp24TrueColorMenuItem);
        
        bpp8IndexedColorMenuItem.addActionListener(event -> config.setColorDepth(BPP_8_INDEXED));
        bpp16TrueColorMenuItem.addActionListener(event -> config.setColorDepth(BPP_16_TRUE));
        bpp24TrueColorMenuItem.addActionListener(event -> config.setColorDepth(BPP_24_TRUE));
        
        localCursorMenuItem = new JCheckBoxMenuItem("Use Local Cursor", true);
        localCursorMenuItem.addActionListener(event -> config.setUseLocalMousePointer(() -> localCursorMenuItem != null && localCursorMenuItem.isSelected()));
        
        copyRectMenuItem = new JCheckBoxMenuItem("COPYRECT", true);
        copyRectMenuItem.addActionListener(event -> config.setEnableCopyrectEncoding(copyRectMenuItem.isSelected()));
        
        rreMenuItem = new JCheckBoxMenuItem("RRE", true);
        rreMenuItem.addActionListener(event -> config.setEnableRreEncoding(rreMenuItem.isSelected()));
        
        hextileMenuItem = new JCheckBoxMenuItem("HEXTILE", true);
        hextileMenuItem.addActionListener(event -> config.setEnableHextileEncoding(hextileMenuItem.isSelected()));
        
        zlibMenuItem = new JCheckBoxMenuItem("ZLIB", false);
        zlibMenuItem.addActionListener(event -> config.setEnableZLibEncoding(zlibMenuItem.isSelected()));
        
        encodingsMenu = new JMenu("Enabled Encodings");
        encodingsMenu.add(copyRectMenuItem);
        encodingsMenu.add(rreMenuItem);
        encodingsMenu.add(hextileMenuItem);
        encodingsMenu.add(zlibMenuItem);
        
        final JMenuItem exit = new JMenuItem("Exit");
        exit.setMnemonic(VK_X);
        exit.addActionListener(event ->
                               {
                                   connectionManager.disconnect();
                                   exit(0);
                               });
        
        file.add(connectMenuItem);
        file.add(disconnectMenuItem);
        file.add(exit);
        options.add(bpp8IndexedColorMenuItem);
        options.add(bpp16TrueColorMenuItem);
        options.add(bpp24TrueColorMenuItem);
        options.add(localCursorMenuItem);
        options.add(encodingsMenu);
        menu.add(file);
        menu.add(options);
        setJMenuBar(menu);
    }
    
    private boolean resizeRequired(Image frame)
    {
        return lastFrame == null || lastFrame.getWidth(null) != frame.getWidth(null) || lastFrame.getHeight(null) != frame.getHeight(null);
    }
    
    private void resizeWindow(Image frame)
    {
        final int remoteWidth = frame.getWidth(null);
        final int remoteHeight = frame.getHeight(null);
        final Rectangle screenSize = getLocalGraphicsEnvironment().getMaximumWindowBounds();
        final int paddingTop = getHeight() - getContentPane().getHeight();
        final int paddingSides = getWidth() - getContentPane().getWidth();
        final int maxWidth = (int)screenSize.getWidth() - paddingSides;
        final int maxHeight = (int)screenSize.getHeight() - paddingTop;
        if (remoteWidth <= maxWidth && remoteHeight < maxHeight)
        {
            setWindowSize(remoteWidth, remoteHeight);
        }
        else
        {
            final double scale = min((double)maxWidth / remoteWidth, (double)maxHeight / remoteHeight);
            final int scaledWidth = (int)(remoteWidth * scale);
            final int scaledHeight = (int)(remoteHeight * scale);
            setWindowSize(scaledWidth, scaledHeight);
        }
        setLocationRelativeTo(null);
    }
    
    private void setWindowSize(int width, int height)
    {
        getContentPane().setPreferredSize(new Dimension(width, height));
        pack();
    }
    
    private int scaleMouseX(int x)
    {
        if (lastFrame == null)
        {
            return x;
        }
        return (int)(x * ((double)lastFrame.getWidth(null) / getContentPane().getWidth()));
    }
    
    private int scaleMouseY(int y)
    {
        if (lastFrame == null)
        {
            return y;
        }
        return (int)(y * ((double)lastFrame.getHeight(null) / getContentPane().getHeight()));
    }
}
