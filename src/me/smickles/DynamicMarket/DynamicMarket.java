package me.smickles.DynamicMarket;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import com.iConomy.iConomy;
import com.iConomy.system.Holdings;

public class DynamicMarket extends JavaPlugin {

	public static DynamicMarket plugin;
	public final Logger logger = Logger.getLogger("Minecraft");
	public Configuration items;
	
	
	@Override
	public void onDisable(){
		PluginDescriptionFile pdfFile = this.getDescription();
		this.logger.info(pdfFile.getName() + " disabled");
	}

	@Override
	public void onEnable() {
		PluginDescriptionFile pdfFile = this.getDescription();
		this.logger.info(pdfFile.getName() + " version " + pdfFile.getVersion() + " enabled");
		
		//item 'config'
		items = getConfiguration();
		items.load();
		String[] itemNames = new String[]{"stone","01","dirt","03","cobblestone","04","sapling","06","sand","12","gravel","13","wood","17","lapis","22","sandstone","24","grass","31","wool","35","dandelion","37","rose","38","brownmushroom","39","redmushroom","40","mossstone","48","obsidian","49","cactus","81","netherrack","87","soulsand","88","vine","106","apple","260","coal","263","diamond","264","iron","265","gold","266","string","287","feather","288","gunpowder","289","seeds","295","flint","318","pork","319","redstone","331","snow","332","leather","334","clay","337","sugarcane","338","slime","341","egg","344","glowstone","348","fish","349","bone","352","pumpkinseeds","361","melonseeds","362","beef","363","chicken","365","rottenflesh","367","enderpearl","368"};
		for(int x = 0; x < itemNames.length; x = x + 2) {
			items.getString(itemNames[x], " ");
			//items.getString(itemNames[x] + ".number", itemNames[x]);
			
		}
		for(int x = 1; x < itemNames.length; x = x + 2) {
			items.getInt(itemNames[x-1] + ".number", Integer.parseInt(itemNames[x]));
			
		}
		for(int x = 0; x < itemNames.length; x = x + 2) {
			items.getDouble(itemNames[x] + ".value", 10);
		}
		items.save();
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		return readCommand((Player) sender, commandLabel, args);
	}
	
	public boolean readCommand(Player player, String command, String[] args) {
		if(command.equalsIgnoreCase("buy")) {
			if(args.length == 2 && Integer.parseInt(args[1]) > 0) {
				//player.sendMessage(ChatColor.RED + "[Server]" + ChatColor.WHITE + "Tell smickles to write the buy code already.");
				logger.info("args0 " + args[0]);
				items.load();
				int id = items.getInt(args[0] + ".number", 0);
				logger.info("id " + id);
				if(id != 0) {
					int amt = Integer.parseInt(args[1]);
					logger.info("amt " + amt);
					double value = items.getDouble(args[0] + ".value", 0);
					logger.info("value before " + value);
					double invoice = 0;
					for(int x = 1; x <= amt; x++) {
						if(value >= .01) {
							invoice = invoice + value;
							invoice = round2(invoice);
						}else{
							invoice = invoice + .01;
							invoice = round2(invoice);
						}
						value = value + .01;
						value = round2(value);
					}
					logger.info("value after " + value);					
					logger.info("invoice " + invoice);
					Holdings holdings = iConomy.getAccount(player.getName()).getHoldings();
					double cash = holdings.balance();
					logger.info("cash " + cash);
					double newBal = cash - invoice;
					logger.info("newBal " + newBal);
					if(cash >= invoice) {
						ItemStack its = new ItemStack(id,amt);
						holdings.subtract(invoice);
						player.getInventory().addItem(its);
						items.setProperty(args[0] + ".value", value);
						items.save();
						player.sendMessage(ChatColor.GREEN + "Old Balance: " + ChatColor.WHITE + cash);
						player.sendMessage(ChatColor.GREEN + "Cost: " + ChatColor.WHITE + invoice);
						player.sendMessage(ChatColor.GREEN + "New Balance: " + ChatColor.WHITE + newBal);
					}else{
						player.sendMessage(ChatColor.RED + "You don't have enough money");
						player.sendMessage(ChatColor.GREEN + "Balance: " + ChatColor.WHITE + cash);
						player.sendMessage(ChatColor.GREEN + "Cost: " + ChatColor.WHITE + invoice);
						player.sendMessage(ChatColor.GREEN + "Difference: " + ChatColor.RED + newBal);
					}
				}else{
					player.sendMessage("Be sure you typed the correct name");
					return false;
				}
				return true;
			}else{
				player.sendMessage("Invalid arguments");
				return false;
			}

		}else if(command.equalsIgnoreCase("sell")) {
			if(args.length == 2 && Integer.parseInt(args[1]) > 0) {
				//player.sendMessage(ChatColor.RED + "[Server]" + ChatColor.WHITE + "Tell smickles to write the buy code already.");
				logger.info("args0 " + args[0]);
				items.load();
				int id = items.getInt(args[0] + ".number", 0);
				logger.info("id " + id);
				if(id != 0) {
					int amt = Integer.parseInt(args[1]);
					logger.info("amt " + amt);
					double value = items.getDouble(args[0] + ".value", 0);
					logger.info("value before " + value);
					double invoice = 0;
					for(int x = 1; x <= amt; x++) {
						if(value >= .01) {
							invoice = invoice + value;
							invoice = round2(invoice);
						}else{
							invoice = invoice + .01;
							invoice = round2(invoice);
						}
						value = value - .01;
						value = round2(value);
					}
					logger.info("value after " + value);					
					logger.info("invoice " + invoice);
					Holdings holdings = iConomy.getAccount(player.getName()).getHoldings();
					double cash = holdings.balance();
					logger.info("cash " + cash);
					double newBal = cash + invoice;
					logger.info("newBal " + newBal);
					// TODO remove logger spam
					int inInventory = 0;
					for(ItemStack is : player.getInventory().getContents()) {
						if(is == null) {
							continue;
						}
						if(is.getTypeId() == id) {
							inInventory += is.getAmount();
						}
					}
					if(inInventory >= amt) {						
						holdings.add(invoice);
						int left = inInventory - amt;						
						player.getInventory().remove(id);
						if(left > 0) {
							ItemStack its = new ItemStack(id,left);
							player.getInventory().addItem(its);
						}
						items.setProperty(args[0] + ".value", value);
						items.save();
						player.sendMessage(ChatColor.GREEN + "Old Balance: " + ChatColor.WHITE + cash);
						player.sendMessage(ChatColor.GREEN + "Sale: " + ChatColor.WHITE + invoice);
						player.sendMessage(ChatColor.GREEN + "New Balance: " + ChatColor.WHITE + newBal);
					}else{
						player.sendMessage(ChatColor.RED + "You don't have enough " + args[0]);
						player.sendMessage(ChatColor.GREEN + "In Inventory: " + ChatColor.WHITE + inInventory);
						player.sendMessage(ChatColor.GREEN + "Attempted Amount: " + ChatColor.WHITE + amt);
					}
				}else{
					player.sendMessage("Be sure you typed the correct name");
					return false;
				}
				return true;
			}else{
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
				double price = items.getDouble(args[0] + ".value", -2000000000);
				if(price != -2000000000) {
					// We received an argument which resolved to an item on our list.
					// The price could register as a negative or below .01
					// in this case we should return .01 as the price.
					if(price < .01) {
						price = .01;
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
						// names
						board[x][1] = names.get(x);
						// prices
						board[x][0] = String.valueOf(items.getDouble(names.get(x) + ".value", -200000000));
					}
					//sort 'em
					Arrays.sort(board, new Comparator<String[]>() {

						@Override
						public int compare(String[] entry1, String[] entry2) {
							final BigDecimal value1 = BigDecimal.valueOf(Double.valueOf(entry1[0]));
							final BigDecimal value2 = BigDecimal.valueOf(Double.valueOf(entry2[0]));
							return value2.compareTo(value1);
						}

						
					});
					// Send them to the player
					for(int x = 0; x < 10; x++) {
						player.sendMessage(board[x][0] + " " + board[x][1]);
					}
					return true;
				}else if(args[0].equalsIgnoreCase("bottom")) {
					// load the item list
					items.load();
					// make  'arrays', a name, a price 
					List<String> names = items.getKeys();
					String board[][] = new String[names.size()][2];
					for(int x = 0; x < names.size(); x++) {
						// names
						board[x][1] = names.get(x);
						// prices
						board[x][0] = String.valueOf(items.getDouble(names.get(x) + ".value", -200000000));
					}
					//sort 'em
					Arrays.sort(board, new Comparator<String[]>() {

						@Override
						public int compare(String[] entry1, String[] entry2) {
							final BigDecimal value1 = BigDecimal.valueOf(Double.valueOf(entry1[0]));
							final BigDecimal value2 = BigDecimal.valueOf(Double.valueOf(entry2[0]));
							return value1.compareTo(value2);
						}

						
					});
					// Send them to the player
					for(int x = 0; x < 10; x++) {
						player.sendMessage(board[x][0] + " " + board[x][1]);
					}
					return true;					
				}
			}
			player.sendMessage("Invalid number of arguments");
		}
		return false;
	}
	
	public static double round2(double num) {
		double result = num * 100;
		result = Math.round(result);
		result = result / 100;
		return result;		
	}
}
