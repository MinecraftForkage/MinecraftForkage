package net.minecraft.launchwrapper;

import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.LogManager;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.apache.logging.log4j.core.impl.Log4jContextFactory;

import joptsimple.ArgumentAcceptingOptionSpec;
import net.minecraft.client.Minecraft;

import com.mojang.authlib.Agent;
import com.mojang.authlib.AuthenticationService;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;

import cpw.mods.fml.common.launcher.FMLInjectionAndSortingTweaker;
import cpw.mods.fml.common.launcher.FMLTweaker;

/**
 * This class is specified by Mojang's launchwrapper.
 */
public class Launch {
	/**
	 * The {@link LaunchClassLoader} that Minecraft is loaded with.
	 * This field is specified by Mojang's launchwrapper.
	 */
	public static LaunchClassLoader classLoader;
	
	/**
	 * A globally-accessible Map containing unspecified objects.
	 * This field is specified by Mojang's launchwrapper.
	 */
	public static Map<String, Object> blackboard = new HashMap<String, Object>();
	
	public static File minecraftHome;
	
	/**
	 * This method is specified by Mojang's launchwrapper.
	 */
	public static void main(final String[] args_) throws Exception {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				blackboard.put("Tweaks", Collections.emptyList());
				blackboard.put("TweakClasses", Collections.emptyList());
				
				List<String> args = new ArrayList<String>(Arrays.asList(args_));
				
				if(args.size() > 0 && args.get(0).equals("--login")) {
					args.remove(0);
					
					final JDialog frame = new JDialog(null, ModalityType.DOCUMENT_MODAL);
					JButton btnOK;
					JTextField txtUsername;
					JPasswordField txtPassword;
					frame.getContentPane().setLayout(new GridLayout(3, 2));
					frame.getContentPane().add(new JLabel("Username/email:"));
					frame.getContentPane().add(txtUsername = new JTextField());
					frame.getContentPane().add(new JLabel("Password:"));
					frame.getContentPane().add(txtPassword = new JPasswordField());
					frame.getContentPane().add(btnOK = new JButton("Log in"));
					frame.setTitle("MCForkage test login");
					frame.pack();
					frame.setLocationRelativeTo(null);
					
					btnOK.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							frame.setVisible(false);
						}
					});
					
					frame.setVisible(true);
					
					String clientToken = UUID.randomUUID().toString();
					AuthenticationService authsvc = new YggdrasilAuthenticationService(Proxy.NO_PROXY, clientToken);
					UserAuthentication auth = authsvc.createUserAuthentication(Agent.MINECRAFT);
					auth.setUsername(txtUsername.getText());
					auth.setPassword(txtPassword.getText());
					frame.dispose();
					try {
						auth.logIn();
					} catch (AuthenticationException e) {
						JOptionPane.showMessageDialog(null, e.toString());
						return;
					}
					GameProfile profile = auth.getSelectedProfile();
					args.add("--username");
					args.add(profile.getName());
					args.add("--uuid");
					args.add(profile.getId().toString());
					args.add("--accessToken");
					args.add(auth.getAuthenticatedToken());
					args.add("--userType");
					args.add(auth.getUserType().toString());
				}
				
				minecraftHome = new File(".");
				for(int k = 0; k < args.size() - 1; k++) {
					if(args.get(k).equals("--gameDir")) {
						minecraftHome = new File(args.get(k+1));
						break;
					}
				}
				
				URL[] urls = ((URLClassLoader)Launch.class.getClassLoader()).getURLs();
				classLoader = new LaunchClassLoader(urls);
				injectCascadingTweak(new FMLTweaker());
				try {
					classLoader.loadClass("net.minecraft.client.main.Main").getMethod("main", String[].class).invoke(null, (Object)args.toArray(new String[0]));
				} catch (Exception e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(null, e.toString());
					return;
				}
			}
		});
	}

	public static void injectCascadingTweak(ITweaker tweaker) {
		tweaker.acceptOptions(new ArrayList<String>(), minecraftHome, null, null);
		tweaker.injectIntoClassLoader(classLoader);
	}
}
