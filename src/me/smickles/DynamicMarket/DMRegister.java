package me.smickles.DynamicMarket;

import java.util.logging.Logger;

import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;

public class DMRegister extends ServerListener {
	private DynamicMarket plugin;
	private com.nijikokun.register.payment.Methods Methods = null;
	public static Logger logger = Logger.getLogger("minecraft");
	
	public DMRegister(DynamicMarket plugin) {
		this.plugin = plugin;
		this.Methods = new com.nijikokun.register.payment.Methods();
	}
	
	@SuppressWarnings("static-access")
	public void onPluginDisable(PluginDisableEvent event) {
		if ((this.Methods != null) && (Methods.hasMethod())) {
			if (Methods.checkDisabled(event.getPlugin())) {
				this.plugin.method = null;
				logger.info("[" + plugin.getDescription().getName() + "] Payment method Disabled");
			}
		}
	}
	
	@SuppressWarnings("static-access")
	public void onPluginEnable(PluginEnableEvent event) {
		if ((!Methods.hasMethod()) && (Methods.setMethod(this.plugin.getServer().getPluginManager()))) {
			this.plugin.method = Methods.getMethod();
			if (plugin.method != null) {
				logger.info("[" + plugin.getDescription().getName() + "] Payment method found");
			} else {
				logger.info("[" + plugin.getDescription().getName() + "] Payment method Disabled");
			}
		}
	}

}
