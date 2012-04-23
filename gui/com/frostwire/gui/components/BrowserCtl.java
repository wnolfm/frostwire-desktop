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
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.limewire.util.OSUtils;

public class BrowserCtl extends JPanel {

    private static final long serialVersionUID = -3684839610832779726L;

    private final JFrame frame;
    private final String url;

    private Canvas canvas;
    private CreateBrowser createBrowser;
    private Browser browser;

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
                        }

                        @Override
                        public void changed(ProgressEvent event) {
                        }
                    });
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

            browser.getDisplay().asyncExec(new Runnable() {
                @Override
                public void run() {
                    browser.getDisplay().dispose();
                }
            });
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
}
