import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;


public class MPlayerVideoProofForWindows {
	public static void main(String[] args) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, ClassNotFoundException {
		
		JFrame frame = new JFrame();
		Dimension d = new Dimension(1024,768);
		frame.setPreferredSize(d);
		frame.setMinimumSize(d);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle("MPlayer on JLayeredPane Proof of Concept");

		//The canvas should hold the hWnd where the mplayer will live.
		Canvas overlay = new Canvas();
		overlay.setMinimumSize(new Dimension(100,100));
		overlay.setPreferredSize(new Dimension(100,100));
		overlay.setVisible(true);
		overlay.setBackground(Color.RED);
		overlay.setEnabled(true);
		overlay.setLocation(10,10);
		
		JPanel p = new JPanel(new FlowLayout());
		p.setMinimumSize(new Dimension(400,400));
		p.add(new JButton("Some Button Here"));
		p.add(new JButton("Another Button"));
		p.setVisible(true);
		
		//The container for the video and other overlays.
		JLayeredPane layeredPane = new JLayeredPane();
		layeredPane.setPreferredSize(d);
		layeredPane.setMinimumSize(d);
		layeredPane.setOpaque(true);
		layeredPane.setVisible(true);
		layeredPane.add(overlay,JLayeredPane.DEFAULT_LAYER);
		

		layeredPane.add(p,1);
		
		frame.getContentPane().add(layeredPane,BorderLayout.CENTER);
		frame.setVisible(true);
		
		//This is to get the hWnd of the window.
		Window[] windows = JFrame.getWindows();
		Window window = windows[0];
		
		final Class<?> cl = Class.forName("sun.awt.windows.WComponentPeer");
	    java.lang.reflect.Field f = cl.getDeclaredField("hwnd");
	    f.setAccessible(true); //little reflection hack to access the hwnd from windows.
	    
	    long hWnd = f.getLong(window.getPeer());
		//long hWnd = f.getLong(overlay.getPeer());
	    
		try{ 
			  String[] cmd = {"mplayer.exe","-wid",String.valueOf(hWnd),"-colorkey","0x010101","video.mp4"};
		      ProcessBuilder processBuilder = new ProcessBuilder(cmd);
		      processBuilder.start();
		  
		}catch(Exception e){
			e.printStackTrace();
		}

		;
		
	}
}
