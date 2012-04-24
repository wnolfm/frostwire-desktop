package com.frostwire.gui.components;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.limewire.util.OSUtils;

import com.limegroup.gnutella.gui.GUIMediator;

public class BrowserCtl extends JPanel {

    private static final long serialVersionUID = -3684839610832779726L;

    private final JFrame frame;
    private final String url;

    private Canvas canvas;
    private CreateBrowser createBrowser;
    private Browser browser;

    private TestFunction testFunction;
    private StartDownload startDownload;

    public BrowserCtl(JFrame frame, String url) {
        this.frame = frame;
        this.url = url;

        setLayout(new FlowLayout());
        setOpaque(false);
    }

    @Override
    public void invalidate() {
        super.invalidate();

        if (browser != null) {
            updateCanvasSize();
        } else {
            setUrl(url);
        }
    }

    private void setUrl(final String url) {
        connect();

        browser.getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                browser.setUrl(url);
            }
        });
    }

    private void connect() {
        if (browser == null) {
            canvas = new Canvas();
            canvas.setBackground(Color.WHITE);
            updateCanvasSize();
            add(canvas);

            createBrowser = SWTFramework.instance().createBrowser(canvas);
            browser = createBrowser.getBrowser();
            frame.repaint();

            browser.getDisplay().asyncExec(new Runnable() {
                @Override
                public void run() {
                    browser.addListener(SWT.MenuDetect, new Listener() {
                        @Override
                        public void handleEvent(Event event) {
                            event.doit = false;
                        }
                    });

                    browser.addProgressListener(new ProgressListener() {
                        @Override
                        public void completed(ProgressEvent event) {
                            refreshCtl();

                            //test js
                            //browser.evaluate("alert(testFunction('testing java-js integration'))");
                            
                            //browser.evaluate("startDownload('magnet:?xt=urn:btih:4d09aa8fbf18433559d8ffcb9fd6ec75fb695eb0&dn=Californication+S03E01+DVDSCR+XviD-FFNDVD+%5Beztv%5D&tr=udp%3A%2F%2Ftracker.openbittorrent.com%3A80&tr=udp%3A%2F%2Ftracker.publicbt.com%3A80&tr=udp%3A%2F%2Ftracker.ccc.de%3A80')");
                        }

                        @Override
                        public void changed(ProgressEvent event) {
                        }
                    });

                    testFunction = new TestFunction(browser);
                    startDownload = new StartDownload(browser);
                }
            });

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    disconnect();
                }
            });
        }
    }

    public void disconnect() {
        if (createBrowser != null) {
            createBrowser.finish();

            if (!browser.isDisposed()) {
                browser.getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        if (!testFunction.isDisposed()) {
                            testFunction.dispose();
                        }
                        
                        if (!startDownload.isDisposed()) {
                            startDownload.dispose();
                        }
                        
                        browser.getDisplay().dispose();
                    }
                });
            }
        }
    }

    private void updateCanvasSize() {
        if (canvas != null) {
            Rectangle r = getVisibleRect();
            Dimension d = new Dimension(r.width, r.height - 8);
            canvas.setPreferredSize(d);
            canvas.setSize(d);
            canvas.setMinimumSize(d);
            canvas.setMaximumSize(d);
        }
    }

    private void refreshCtl() {
        browser.getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                browser.getShell().setSize(canvas.getWidth(), canvas.getHeight());
            }
        });
    }

    private static final class SWTFramework {

        static {
            System.setProperty("swt.library.path", "lib/native");
            Display.setAppName("FrostWire");
        }

        private static SWTFramework instance;

        public static SWTFramework instance() {
            if (instance == null) {
                instance = new SWTFramework();
            }
            return instance;
        }

        private SWTFramework() {
        }

        public CreateBrowser createBrowser(Canvas canvas) {
            CreateBrowser cb = new CreateBrowser(canvas);
            if (OSUtils.isMacOSX()) {
                com.apple.concurrent.Dispatch.getInstance().getNonBlockingMainQueueExecutor().execute(cb);
            } else {
                Thread t = new Thread(cb, "SWT-Thread");
                t.start();
            }

            synchronized (cb) {
                while (cb.getBrowser() == null) {
                    try {
                        cb.wait(100);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }

            return cb;
        }
    }

    private static class CreateBrowser implements Runnable {

        private final Canvas canvas;

        private boolean finished;
        private Browser browser;

        public CreateBrowser(Canvas canvas) {
            this.canvas = canvas;
        }

        public Browser getBrowser() {
            return browser;
        }

        public void finish() {
            finished = true;
        }

        @Override
        public void run() {
            try {
                Display display = new Display();
                Shell shell = SWT_AWT.new_Shell(display, canvas);
                shell.setLayout(new FillLayout());

                synchronized (this) {
                    browser = new Browser(shell, SWT.NONE);
                    this.notifyAll();
                }

                shell.open();
                while (!finished && !shell.isDisposed()) {
                    if (!display.readAndDispatch()) {
                        display.sleep();
                    }
                }
                //shell.dispose();
                //display.dispose();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    private static class StartDownload extends BrowserFunction {

        public StartDownload(Browser browser) {
            super(browser, "startDownload");
        }
        
        @Override
        public Object function(Object[] arguments) {
            System.out.println("StartDownload invoked. ("+arguments[0]+")");
              GUIMediator.instance().openTorrentURI((String) arguments[0]);
              return true;
        }
    }
    
    private static class TestFunction extends BrowserFunction {

        public TestFunction(Browser browser) {
            super(browser, "testFunction");
        }

        @Override
        public Object function(Object[] arguments) {
            System.out.println("BrowserCtl.TestFunction invoked.");
            return arguments[0];
        }
    }
}