package com.frostwire.gui.components;

import java.awt.BorderLayout;
import java.awt.Canvas;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

public class BrowserCtl extends JComponent {

    private static final long serialVersionUID = -3684839610832779726L;

    private Canvas canvas;
    private Browser browser;

    public BrowserCtl() {
        setLayout(new BorderLayout());
    }

    public void setUrl(final String url) {
        connect();

        browser.getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                browser.setUrl(url);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        // find the right way to refresh the component
                    }
                });
            }
        });
    }

    private void connect() {
        if (browser == null) {
            canvas = new Canvas();
            add(canvas, BorderLayout.CENTER);

            browser = SWTFramework.instance().createBrowser(canvas);

            browser.getDisplay().asyncExec(new Runnable() {
                @Override
                public void run() {
                    browser.addListener(SWT.MenuDetect, new Listener() {
                        public void handleEvent(Event event) {
                            event.doit = false;
                        }
                    });
                }
            });
        }
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

        public Browser createBrowser(Canvas canvas) {
            CreateBrowser cb = new CreateBrowser(canvas);
            com.apple.concurrent.Dispatch.getInstance().getNonBlockingMainQueueExecutor().execute(cb);

            synchronized (cb) {
                while (cb.getBrowser() == null) {
                    try {
                        cb.wait(100);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }

            return cb.getBrowser();
        }

        private static class CreateBrowser implements Runnable {

            private final Canvas canvas;

            private Browser browser;

            public CreateBrowser(Canvas canvas) {
                this.canvas = canvas;
            }

            public Browser getBrowser() {
                return browser;
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
                    while (!shell.isDisposed()) {
                        if (!display.readAndDispatch()) {
                            display.sleep();
                        }
                    }
                    shell.dispose();
                    display.dispose();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
