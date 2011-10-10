package me.smickles.DynamicMarket;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import com.nijikokun.register.payment.Methods;
import com.nijikokun.register.payment.Method.MethodAccount;

public class DynamicMarket extends JavaPlugin {

	public static DynamicMarket plugin;
	public final Logger logger = Logger.getLogger("Minecraft");
	public Configuration items;
	public static BigDecimal MINVALUE = BigDecimal.valueOf(.01).setScale(2);
	public static BigDecimal MAXVALUE = BigDecimal.valueOf(10000).setScale(2);
	public static BigDecimal CHANGERATE = BigDecimal.valueOf(.01).setScale(2);
	public static BigDecimal SPREAD = CHANGERATE;
	public static File directory;
	
	@Override
	public void onDisable(){
		PluginDescriptionFile pdfFile = this.getDescription();
		this.logger.info(pdfFile.getName() + " disabled");
	}

	@Override
	public void onEnable() {
		PluginDescriptionFile pdfFile = this.getDescription();
		directory = getDataFolder();
		boolean configIsThere = false;
		items = getConfiguration();
		
		if (directory.exists()) {
			for (String f : directory.list()) {
				if (f.equalsIgnoreCase("config.yml")) {
					configIsThere = true;
					break;
				}
			}
		}
		
		
		if (!configIsThere) {
			logger.info("[" + pdfFile.getName() +"] Could not find config, building default");			
			
			// default item 'config'
			items.load();
			
			String[] itemNames = new String[]{"stone","01","dirt","03","cobblestone","04","sapling","06","sand","12","gravel","13","wood","17","lapis","22","sandstone","24","grass","31","wool","35","dandelion","37","rose","38","brownmushroom","39","redmushroom","40","mossstone","48","obsidian","49","cactus","81","netherrack","87","soulsand","88","vine","106","apple","260","coal","263","diamond","264","iron","265","gold","266","string","287","feather","288","gunpowder","289","seeds","295","flint","318","pork","319","redstone","331","snow","332","leather","334","clay","337","sugarcane","338","slime","341","egg","344","glowstone","348","fish","349","bone","352","pumpkinseeds","361","melonseeds","362","beef","363","chicken","365","rottenflesh","367","enderpearl","368"};
			
			for(int x = 0; x < itemNames.length; x = x + 2) {
				items.getString(itemNames[x], " ");
				items.getDouble(itemNames[x] + ".value", 10);
				items.getDouble(itemNames[x] + ".minValue", MINVALUE.doubleValue());
				items.getDouble(itemNames[x] + ".maxValue", MAXVALUE.doubleValue());
				items.getDouble(itemNames[x] + ".changeRate", CHANGERATE.doubleValue());
				items.getDouble(itemNames[x] + ".spread", SPREAD.doubleValue());
			}

			for (int x = 1; x < itemNames.length; x = x + 2) {
				items.getInt(itemNames[x-1] + ".number", Integer.parseInt(itemNames[x]));
				MaterialData mat = new MaterialData(Integer.parseInt(itemNames[x]));
				items.getString(itemNames[x-1] + ".data", Byte.toString(mat.getData()));
			}
			
					
			items.save();
		} else {
			logger.info("[" + pdfFile.getName() +"] Found config, making sure it's up to date.");
			
			items.load();
			for (String n : items.getKeys()) {
				items.getDouble(n + ".value", 10);
				items.getDouble(n + ".minValue", MINVALUE.doubleValue());
				items.getDouble(n + ".maxValue", MAXVALUE.doubleValue());
				items.getDouble(n + ".changeRate", CHANGERATE.doubleValue());
				MaterialData mat = new MaterialData(items.getInt(n + ".number", 0));
				items.getString(n + ".data", Byte.toString(mat.getData()));
				items.getDouble(n + ".spread", SPREAD.doubleValue());
			}
			
			items.save();
		}
		
		
		// setup economy
		PluginManager pm = this.getServer().getPluginManager();
		Plugin register = pm.getPlugin("Register");
		
		if (register != null && register.isEnabled()) {
			Methods.setMethod(pm);
			if (Methods.getMethod() != null) {
				logger.info("[" + pdfFile.getName() + "] Economy plugin found.");
			} else {
				logger.severe("[" + pdfFile.getName() + "] Could not find Economy plugin. " + pdfFile.getName() + " will be disabled.");
				pm.disablePlugin(this);
				return;
			}
		} else {
			logger.severe("[" + pdfFile.getName() + "] Could not find Register. " + pdfFile.getName() + " will be retry.");
			pm.enablePlugin(register);
			pm.disablePlugin(this);
			pm.enablePlugin(this);
			return;
		}
		
		this.logger.info(pdfFile.getName() + " version " + pdfFile.getVersion() + " enabled");
	}
	

	
	/**
	 * Determine the cost of a given number of an item and calculate a new value for the item accordingly.
	 * @param oper 1 for buying, 0 for selling.
	 * @param item the item in question
	 * @param amount the desired amount of the item in question
	 * @return the total cost and the calculated new value as an Invoice
	 */
	public Invoice generateInvoice(int oper, String item, int amount) {
		items.load();
		
		// get the initial value of the item, 0 for not found
		Invoice inv = new Invoice(BigDecimal.valueOf(0),BigDecimal.valueOf(0));
		inv.value = BigDecimal.valueOf(items.getDouble(item + ".value", 0));
		
		// if the we are selling, do one initial decrement of the value
		BigDecimal spread = BigDecimal.valueOf(items.getDouble(item + ".spread", SPREAD.doubleValue()));
		
		// determine the total cost
		inv.total = BigDecimal.valueOf(0);
		
		for(int x = 1; x <= amount; x++) {
			BigDecimal minValue = BigDecimal.valueOf(items.getDouble(item + ".minValue", MINVALUE.doubleValue()));
			BigDecimal maxValue = BigDecimal.valueOf(items.getDouble(item + ".maxValue", MAXVALUE.doubleValue()));
			BigDecimal changeRate = BigDecimal.valueOf(items.getDouble(item + ".changeRate", CHANGERATE.doubleValue()));

			// work the spread on the first one.
			if ((oper == 0) && (x == 1)) {
				inv.subtractValue(spread);
			// otherwise, do the usual decriment.
			} else if ((oper == 0) && (x > 1)) {
				inv.subtractValue(changeRate);
			}
			
			// check the current value
			if((inv.getValue().compareTo(minValue) == 1) | (inv.getValue().compareTo(minValue) == 0)) {
				// current value is @ or above minValue
				// be sure value is not above maxValue
				if (inv.getValue().compareTo(maxValue) == -1) {
					// current value is "just right"
					// add current value to total
					inv.addTotal(inv.getValue());
				} else {
					// current value is above the max
					// add maxValue to total
					inv.addTotal(maxValue);
				}
			} else {
				// current value is below the minimum
				// add the minimum to total
				inv.addTotal(minValue);
			}
			
			// Change our stored value for the item
			// we don't care about min/maxValue here because we don't want the value to 'bounce' off of them.
			if (oper == 1) {
				inv.addValue(changeRate);
			}
		}
		return inv;
	}
	

	/**
	 * Buy a specified amount of an item for the player.
	 * 
	 * @param player The player on behalf of which these actions will be carried out. 
	 * @param item The desired item in the form of the item name. 
	 * @param amount The desired amount of the item to purchase.
	 * @return true on success, false on failure. 
	 */
	public boolean buy (Player player, String item, int amount) {
		
		// Be sure we have a positive amount
		if (amount < 0) {
			player.sendMessage(ChatColor.RED + "Invalid amount.");
			player.sendMessage("No negative numbers, please.");
			return false;
		}
		items.load();
		int id = items.getInt(item + ".number", 0);
		// a value of 0 would indicate that we did not find an item with that name
		if(id != 0) {
			// determine what it will cost 
			Invoice invoice = generateInvoice(1, item, amount);
			MethodAccount cash = Methods.getMethod().getAccount(player.getName());
			if (cash.hasEnough(invoice.getTotal().doubleValue())) {
				Byte byteData = Byte.valueOf(items.getString(item + ".data"));
				
				player.getInventory().addItem(new ItemStack(id, amount, (short) 0, byteData));
				items.setProperty(item + ".value", invoice.getValue());
				items.save();
				// Give some nice output.
				player.sendMessage(ChatColor.GREEN + "--------------------------------");
				player.sendMessage(ChatColor.GREEN + "Old Balance: " + ChatColor.WHITE + BigDecimal.valueOf(cash.balance()).setScale(2, RoundingMode.HALF_UP));
				// Subtract the invoice (this is an efficient place to do this)
				cash.subtract(invoice.getTotal().doubleValue());

				player.sendMessage(ChatColor.GREEN + "Cost: " + ChatColor.WHITE + invoice.getTotal());
				player.sendMessage(ChatColor.GREEN + "New Balance: " + ChatColor.WHITE + BigDecimal.valueOf(cash.balance()).setScale(2, RoundingMode.HALF_UP));
				player.sendMessage(ChatColor.GREEN + "--------------------------------");
				player.sendMessage(ChatColor.GRAY + item + ChatColor.GREEN + " New Price: " + ChatColor.WHITE + invoice.getValue());
				return true;
			} else {
				// Otherwise, give nice output anyway ;)
				// The idea here is to show how much more money is needed.
				BigDecimal difference = BigDecimal.valueOf(cash.balance() - invoice.getTotal().doubleValue()).setScale(2, RoundingMode.HALF_UP);
				player.sendMessage(ChatColor.RED + "You don't have enough money");
				player.sendMessage(ChatColor.GREEN + "Balance: " + ChatColor.WHITE + BigDecimal.valueOf(cash.balance()).setScale(2, RoundingMode.HALF_UP));
				player.sendMessage(ChatColor.GREEN + "Cost: " + ChatColor.WHITE + invoice.getTotal());
				player.sendMessage(ChatColor.GREEN + "Difference: " + ChatColor.RED + difference);
				return false;
			}
		}else{
			player.sendMessage(ChatColor.RED + "Not allowed to buy that item.");
			player.sendMessage("Be sure you typed the correct name");
			return false;
		}
	}
	
	/**
	 * Figure out how much of a given item is in the player's inventory
	 * @param player The player entity in question.
	 * @param id The Data Value of the item in question.
	 * @return The amount of the item in the player's inventory as an integer.
	 */
	public int getAmountInInventory(Player player, ItemStack it) {
		int inInventory = 0;

		
		for (int x = 0; x <= 35; x++) {
			ItemStack slot = player.getInventory().getItem(x);
			Byte slotData = slot.getData().getData();
			
			if ((slot.getTypeId() == it.getTypeId()) && (slotData.compareTo(it.getData().getData()) == 0)) {
				inInventory += slot.getAmount();
			}
			
		}
		return inInventory;
	}
	
	/**
	 * Sell a specified amount of an item for the player.
	 * 
	 * @param player The player on behalf of which these actions will be carried out. 
	 * @param item The desired item in the form of the item name. 
	 * @param amount The desired amount of the item to sell.
	 * @return true on success, false on failure. 
	 */
	public boolean sell (Player player, String item, int amount) {
		
		// Be sure we have a positive amount
		if (amount < 0) {
			player.sendMessage(ChatColor.RED + "Invalid amount.");
			player.sendMessage("No negative numbers, please.");
			return false;
		}
		items.load();
		int id = items.getInt(item + ".number", 0);
		
		// a value of 0 would indicate that we did not find an item with that name
		if(id != 0) {
			Byte byteData = Byte.valueOf(items.getString(item + ".data"));
			
			// determine what it will pay 
			Invoice invoice = generateInvoice(0, item, amount);
			MethodAccount cash = Methods.getMethod().getAccount(player.getName());
			// If the player has enough of the item, perform the transaction.
			ItemStack its = new ItemStack(id, amount, (short) 0, byteData);
			if (player.getInventory().contains(id)) {
				
				// Figure out how much is left over.
				int left = getAmountInInventory(player, its) - amount;
				if (left < 0) { // this indicates the correct id, but wrong bytedata value
					// give nice output even if they gave a bad number.
					player.sendMessage(ChatColor.RED + "You don't have enough " + item);
					player.sendMessage(ChatColor.GREEN + "In Inventory: " + ChatColor.WHITE + getAmountInInventory(player, its));
					player.sendMessage(ChatColor.GREEN + "Attempted Amount: " + ChatColor.WHITE + amount);
					return false;
				}
					
				// Take out all of the item
				for (int x = 0; x <= 35; x++) {
					ItemStack slot = player.getInventory().getItem(x);
					Byte slotData = slot.getData().getData();
					
					if ((slot.getTypeId() == id) && (slotData.compareTo(byteData) == 0)) {
						player.getInventory().clear(x);
					}
				}

				// put back what was left over
				if(left > 0) {
					ItemStack itsLeft = its;
					itsLeft.setAmount(left);
					player.getInventory().addItem(itsLeft);
				}
				items.setProperty(item + ".value", invoice.getValue());
				// record the change in value
				items.save();
				// give some nice output
				player.sendMessage(ChatColor.GREEN + "--------------------------------");
				player.sendMessage(ChatColor.GREEN + "Old Balance: " + ChatColor.WHITE + BigDecimal.valueOf(cash.balance()).setScale(2, RoundingMode.HALF_UP));
				cash.add(invoice.getTotal().doubleValue());
				player.sendMessage(ChatColor.GREEN + "Sale: " + ChatColor.WHITE + invoice.total);
				player.sendMessage(ChatColor.GREEN + "New Balance: " + ChatColor.WHITE + BigDecimal.valueOf(cash.balance()).setScale(2, RoundingMode.HALF_UP));
				player.sendMessage(ChatColor.GREEN + "--------------------------------");
				player.sendMessage(ChatColor.GRAY + item + ChatColor.GREEN + " New Price: " + ChatColor.WHITE + invoice.getValue());
				return true;
			}else{
				// give nice output even if they gave a bad number.
				player.sendMessage(ChatColor.RED + "You don't have enough " + item);
				player.sendMessage(ChatColor.GREEN + "In Inventory: " + ChatColor.WHITE + getAmountInInventory(player, its));
				player.sendMessage(ChatColor.GREEN + "Attempted Amount: " + ChatColor.WHITE + amount);
				return false;
			}
		}else{
			player.sendMessage(ChatColor.RED + "Not allowed to buy that item.");
			player.sendMessage("Be sure you typed the correct name");
			return false;
		}
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		return readCommand((Player) sender, commandLabel, args);
	}
	
	public boolean readCommand(Player player, String command, String[] args) {
		if(command.equalsIgnoreCase("buy")) {
			if(args.length == 2) {
				String item = args[0];
				int amount = 0;
				try {
					amount = Integer.parseInt(args[1]);
				} catch (NumberFormatException e) {
					player.sendMessage(ChatColor.RED + "Invalid amount.");
					player.sendMessage("Be sure you typed a whole number.");
					return false;
				}
				return buy(player, item, amount);
			} else {
				player.sendMessage("Invalid number of arguments");
				return false;
			}

		} else if (command.equalsIgnoreCase("sell")) {
			if (args.length == 1) {
				if (args[0].equalsIgnoreCase("all")) {
					return sellAll(player);
				}
			} else if (args.length == 2) {
				String item = args[0];
				int amount = 0;
				try {
					amount = Integer.parseInt(args[1]);
				} catch (NumberFormatException e) {
					player.sendMessage(ChatColor.RED + "Invalid amount.");
					player.sendMessage("Be sure you typed a whole number.");
					return false;
				}
				return sell(player, item, amount);
			} else {
				player.sendMessage("Invalid number of arguments");
				return false;
			}
		// Command Example: /price cobblestone
		// should return: cobblestone: .01
		}else if(command.equalsIgnoreCase("price")){
			// We expect one argument
			if(args.length == 1){
				// Load the item list
				items.load();
				// get the price of the given item, if it's an invalid item set our variable to -2000000000 (an unlikely number to receive 'naturally')
				BigDecimal price = BigDecimal.valueOf(items.getDouble(args[0] + ".value", -2000000000));
				BigDecimal minValue = BigDecimal.valueOf(items.getDouble(args[0] + ".minValue", MINVALUE.doubleValue()));
				if(price.intValue() != -2000000000) {
					// We received an argument which resolved to an item on our list.
					// The price could register as a negative or below .01
					// in this case we should return .01 as the price.
					if(price.compareTo(minValue) == -1) {
						price = minValue;
					}
					player.sendMessage(ChatColor.GREEN + args[0] + ": " + ChatColor.WHITE + price);
					return true;
				}else{
					// We received an argument which did not resolve to a known item.
					player.sendMessage(ChatColor.RED + "Be sure you typed the correct name");
					player.sendMessage(args[0] + ChatColor.RED + " is invalid");
					return false;
				}
			}else{
				// We received too many or too few arguments.
				player.sendMessage("Invalid Arguments");
				return false;
			}
		// Example: '/market top' should return the top 5 most expensive items on the market
		// '/market bottom' should do the dame for the least expensive items.
		}else if(command.equalsIgnoreCase("market")) {
			// we expect one argument
			if(args.length == 1) {
				// We received '/market top'
				if(args[0].equalsIgnoreCase("top")) {
					// load the item list
					items.load();
					// make  'arrays', a name, a price 
					List<String> names = items.getKeys();
					String board[][] = new String[names.size()][2];
					for(int x = 0; x < names.size(); x++) {
						BigDecimal maxValue = BigDecimal.valueOf(items.getDouble(names.get(x) + ".maxValue", MAXVALUE.doubleValue()));
						BigDecimal value = BigDecimal.valueOf(items.getDouble(names.get(x) + ".value", -200000000));
						
						// names
						board[x][1] = names.get(x);
						// prices, but we want max prices if the value is above maxValue
						if (value.compareTo(maxValue) == 1) {
							BigDecimal elasticity;
							BigDecimal changeRate = BigDecimal.valueOf(items.getDouble(names.get(x) + ".changeRate", CHANGERATE.doubleValue()));
							
							// determine how many changeRate above the max it is
							elasticity = value.subtract(maxValue).divide(changeRate).setScale(0, RoundingMode.UP);
							
							board[x][0] = maxValue.toString() + " [" + elasticity + "]";
						} else {
							board[x][0] = value.toString() + " [0]";							
						}
					}
					//sort 'em
					Arrays.sort(board, new Comparator<String[]>() {

						@Override
						public int compare(String[] entry1, String[] entry2) {
							final BigDecimal value1 = BigDecimal.valueOf(Double.valueOf(entry1[0].split(" ")[0]));
							final BigDecimal value2 = BigDecimal.valueOf(Double.valueOf(entry2[0].split(" ")[0]));
							return value2.compareTo(value1);
						}

						
					});
					// Send them to the player
					for(int x = 0; x < 10; x++) {
						int rank = x + 1;
						BigDecimal value = BigDecimal.valueOf(Double.parseDouble(board[x][0].split(" ")[0])).setScale(2, RoundingMode.HALF_UP);
						String elasticity = board[x][0].split(" ")[1];
						
						player.sendMessage(ChatColor.GREEN + String.valueOf(rank) + ". " + ChatColor.WHITE + board[x][1] + " " + ChatColor.GRAY + value + " " + ChatColor.DARK_GREEN + elasticity);
					}
					return true;
				}else if(args[0].equalsIgnoreCase("bottom")) {
					// load the item list
					items.load();
					// make  'arrays', a name, a price 
					List<String> names = items.getKeys();
					String board[][] = new String[names.size()][2];
					for(int x = 0; x < names.size(); x++) {
						BigDecimal minValue = BigDecimal.valueOf(items.getDouble(names.get(x) + ".minValue", MINVALUE.doubleValue()));
						BigDecimal value = BigDecimal.valueOf(items.getDouble(names.get(x) + ".value", -200000000));
						
						// names
						board[x][1] = names.get(x);
						// prices, but we want min prices if the value is above maxValue
						if (value.compareTo(minValue) == -1) {
							BigDecimal elasticity;
							BigDecimal changeRate = BigDecimal.valueOf(items.getDouble(names.get(x) + ".changeRate", CHANGERATE.doubleValue()));
							
							// determine how many changeRate below the min it is
							elasticity = value.subtract(minValue).abs().divide(changeRate).setScale(0, RoundingMode.DOWN);
							
							board[x][0] = minValue.toString() + " [" + elasticity + "]";
						} else {
							board[x][0] = value.toString() + " [0]";							
						}
					}
					//sort 'em
					Arrays.sort(board, new Comparator<String[]>() {

						@Override
						public int compare(String[] entry1, String[] entry2) {
							final BigDecimal value1 = BigDecimal.valueOf(Double.valueOf(entry1[0].split(" ")[0]));
							final BigDecimal value2 = BigDecimal.valueOf(Double.valueOf(entry2[0].split(" ")[0]));
							return value1.compareTo(value2);
						}

						
					});
					// Send them to the player
					for(int x = 0; x < 10; x++) {
						int rank = x + 1;
						BigDecimal value = BigDecimal.valueOf(Double.parseDouble(board[x][0].split(" ")[0])).setScale(2, RoundingMode.HALF_UP);
						String elasticity = board[x][0].split(" ")[1];
																		
						player.sendMessage(ChatColor.GREEN + String.valueOf(rank) + ". " + ChatColor.WHITE + board[x][1] + " " + ChatColor.GRAY + value + " " + ChatColor.DARK_GREEN + elasticity);
					}
					return true;					
				}
			}
			player.sendMessage("Invalid number of arguments");
		}
		return false;
	}
	
	private boolean sellAll(Player player) {
		items.load();
		List<String> names = items.getKeys();
		int[] id = new int[names.size()];
		BigDecimal[] value = new BigDecimal[names.size()];
		Byte[] byteData = new Byte[names.size()];
		BigDecimal sale = BigDecimal.ZERO.setScale(2);
		
		// make a 'list' of all sellable items with their id's and values
		for (int x = 0; x < names.size(); x++) {
			id[x] = items.getInt(names.get(x) + ".number", 0);
			value[x] = BigDecimal.valueOf(items.getDouble(names.get(x) + ".value", 0)).setScale(2, RoundingMode.HALF_UP);
			byteData[x] = Byte.valueOf(items.getString(names.get(x) + ".data"));
		}
		
		// run thru each slot and sell any sellable items
		for (int index = 0; index <= 35; index++) {
			ItemStack slot = player.getInventory().getItem(index);
			int slotId = slot.getTypeId();
			BigDecimal slotAmount = new BigDecimal(slot.getAmount()).setScale(0, RoundingMode.HALF_UP);
			Byte slotByteData = slot.getData().getData();
			
			for (int x = 0; x < names.size(); x++) {
				if ((id[x] == slotId) && (byteData[x].compareTo(slotByteData) == 0)) {
					// perform sale of this slot
					Invoice thisSale = generateInvoice(0, names.get(x), slotAmount.intValue());
					// rack up our total
					sale = sale.add(thisSale.getTotal());
					// save the new value
					items.setProperty(names.get(x) + ".value", thisSale.getValue());
					items.save();
					// remove the item(s)
					player.getInventory().clear(index);
					// "pay the man"
					MethodAccount cash = Methods.getMethod().getAccount(player.getName());
					cash.add(thisSale.getTotal().doubleValue());
					// give nice output
					player.sendMessage(ChatColor.GREEN + "Sold " + ChatColor.WHITE + slotAmount + " " + ChatColor.GRAY + names.get(x) + ChatColor.GREEN + " for " + ChatColor.WHITE + thisSale.getTotal());
					break;
				}
			}
			
		}
		
		// give a nice total collumn
		if (sale == BigDecimal.ZERO.setScale(2))
			player.sendMessage("Nothing to Sell");
		player.sendMessage(ChatColor.GREEN + "--------------------------------");
		player.sendMessage(ChatColor.GREEN + "Total Sale: " + ChatColor.WHITE + sale);
		return true;
	}
}
