/**DynaMark
 * Copyright 2011 Michael Carver
 * 
 * This file is part of DynaMark.
 *
 *  DynaMark is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  DynaMark is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with DynaMark.  If not, see <http://www.gnu.org/licenses/>.
 **/
package me.smickles.DynamicMarket;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class DynamicMarket extends JavaPlugin {

    public DynamicMarket plugin;
    public final Logger logger = Logger.getLogger("Minecraft");
    public static BigDecimal MINVALUE = BigDecimal.valueOf(.01).setScale(2, RoundingMode.HALF_UP);
    public static BigDecimal MAXVALUE = BigDecimal.valueOf(10000).setScale(2, RoundingMode.HALF_UP);
    public static BigDecimal CHANGERATE = BigDecimal.valueOf(.01).setScale(2, RoundingMode.HALF_UP);
    public static BigDecimal SPREAD = CHANGERATE;
    public static File directory;
    PluginDescriptionFile pdfFile;
    
    /*
     * Vault Method stuffs
     */
    public static Permission permission = null;
    public static Economy economy = null;
    
    @Override
    public void onDisable() {
        
        this.logger.info(pdfFile.getName() + " disabled");
    }

    @Override
    public void onEnable() {
        
        plugin = this;
        directory = plugin.getDataFolder();
        pdfFile = plugin.getDescription();

        checkEbean();
        setupDatabase();
        setupFiles();
        setupPermissions(); //Smickles thinks this is what we're supposed to do for permissions via vault
        setupEconomy(); //Smickles thinks this is what we're supposed to do for economy via vault

        logger.info(pdfFile.getName() + " version " + pdfFile.getVersion() + " enabled");
    }
    
    private void checkEbean() {

        File ep = new File(directory + File.separator + ".." + File.separator + ".." + File.separator + "ebean.properties");

        try {
            if (!ep.exists()) {

                ep.createNewFile();
                logger.info("[" + pdfFile.getName() + "] Had to create ebean.properties. It may be a good idea to restart your server now.");
            }
        } catch (IOException e) {
            
            logger.warning("failed to create ebean.properties");
        }
    }

    /**
     * Check for DynaMark files. Distribute them if needed.
     * Also, check for the old way of storing commodity data, dealing with that as necessary.
     */
    private void setupFiles() {

        boolean licenseIsThere = false;
        boolean readmeIsThere = false;

        
        if (directory.exists()) {
            for (String f : directory.list()) {
                if (f.equalsIgnoreCase("config.yml"))
                    switchToDatabase();
                if (f.equalsIgnoreCase("LICENSE"))
                    licenseIsThere = true;
                if (f.equalsIgnoreCase("README"))
                    readmeIsThere = true;
            }
        }
        
        
        if (!licenseIsThere | !readmeIsThere) {
            //distribute the license and readme
            for (int x = 0; x <= 1; x++) {
                boolean writeFile = true;
                
                try {
                    InputStream defaultStream = null;
                    File conf = null;
                    
                    switch (x) {
                    case 0:
                        defaultStream = this.getClass().getResourceAsStream("/LICENSE");
                        conf = new File(directory + File.separator +"LICENSE");
                        if (licenseIsThere)
                            writeFile = false;
                        break;
                    case 1:
                        defaultStream = this.getClass().getResourceAsStream("/README");
                        conf = new File(directory + File.separator +"README");
                        if (readmeIsThere)
                            writeFile = false;
                        break;
                    }
                    
                    if (writeFile) {
                        directory.mkdir();
                        conf.createNewFile();
                        
                        OutputStream confStream = new FileOutputStream(conf);
                        
                        byte buf[] = new byte[1024];
                        int len;
                        
                        while ((len = defaultStream.read(buf)) > 0) {
                            confStream.write(buf, 0, len);
                        }
                        
                        defaultStream.close();
                        confStream.close();
                    }
                    
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * use this in the case that we need to update from the old way of storing data in config.yml
     */
    @SuppressWarnings("deprecation")
    private void switchToDatabase() {

        logger.info("[" + pdfFile.getName() + "] Converting flatfile to database...");
        
        //load old config file
        org.bukkit.util.config.Configuration items = plugin.getConfiguration();
        
        // populate the database with existing values
        logger.info("[" + plugin.getDescription().getName() + "] Populating database ...");
        for (String item : items.getKeys()) {
            
            Commodities commodity = plugin.getDatabase().find(Commodities.class)
            		.where()
            		.ieq("name", item)
            		.ieq("number", items.getString(item + ".number"))
            		.findUnique();

            if (commodity == null) {
                
                commodity = new Commodities();
                commodity.setName(item);
                
                for (String key : items.getKeys(item)) {
                    
                    String value = items.getString(item + "." + key);
                    
                    if (key.equalsIgnoreCase("value"))
                        commodity.setValue(Double.valueOf(value));
                    if (key.equalsIgnoreCase("number"))
                        commodity.setNumber(Integer.valueOf(value));
                    if (key.equalsIgnoreCase("minValue"))
                        commodity.setMinValue(Double.valueOf(value));
                    if (key.equalsIgnoreCase("maxValue"))
                        commodity.setMaxValue(Double.valueOf(value));
                    if (key.equalsIgnoreCase("changeRate"))
                        commodity.setChangeRate(Double.valueOf(value));
                    if (key.equalsIgnoreCase("data"))
                        commodity.setData(Integer.valueOf(value));
                    if (key.equalsIgnoreCase("spread"))
                        commodity.setSpread(Double.valueOf(value));
                }
            } else {
                logger.warning("[" + pdfFile.getName() + "] Duplicate commodity found, that can't be good. You may want to restore the config.yml backup, delete Dynamark.db (or equivilant), then check the file for commodities with the same \"number\", correct the issue, and then restart your server to try again.");
                continue;
            }
            
            plugin.getDatabase().save(commodity);
            commodity = null;
        }
        
        // mv config.yml to config.yml.bak
        logger.info("[" + plugin.getDescription().getName() + "] backing up config.yml...");
        
        File configFlatFile = new File(directory + File.separator + "config.yml");
        File backupName = new File(directory + File.separator + "config.yml.bak");
        
        configFlatFile.renameTo(backupName);
        
        //  rm config.yml.example
        File toDelete = new File(directory + File.separator + "config.yml.EXAMPLE");
        
        toDelete.delete();
        
        logger.info("[" + plugin.getDescription().getName() + "] Successfully converted flatfile to database");

    }
    
    /**
     * Basically taken from http://pastebin.com/8YrDUqcV
     */
    private void setupDatabase() {
        
        try {
            getDatabase().find(Commodities.class).findRowCount();
        } catch (PersistenceException ex) {
            System.out.println("Installing database for " + getDescription().getName() + " due to first time usage");
            installDDL();
        }
    }
    
    /**
     * Basically taken from http://pastebin.com/8YrDUqcV
     */
    @Override
    public List<Class<?>> getDatabaseClasses() {
        List<Class<?>> list = new ArrayList<Class<?>>();
        list.add(Commodities.class);
        return list;
    }

    /**
     * Copy-pasted from http://dev.bukkit.org/server-mods/vault/
     * I hope we're doing this right
     * @return
     */
    private Boolean setupPermissions()
    {
        RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) {
            permission = permissionProvider.getProvider();
        }
        return (permission != null);
    }
    
    /**
     * Copy-pasted from http://dev.bukkit.org/server-mods/vault/
     * I hope we're doing this right
     * @return
     */
    private Boolean setupEconomy()
    {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }
    
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        return readCommand(sender, commandLabel, args);
    }

    public boolean readCommand(CommandSender sender, String command, String[] args) {
        if((command.equalsIgnoreCase("buy")) && (sender instanceof Player)) {
            
            if (!permission.has(sender, "dynamark.buy")) {
                sender.sendMessage(ChatColor.RED + "You need permission to use this command");
                return true;
            }
            
            if(args.length == 2) {
                String item = args[0];
                int amount = 0;
                try {
                    amount = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid amount.");
                    sender.sendMessage("Be sure you typed a whole number.");
                    return false;
                }
                return buy((Player) sender, item, amount);
            } else {
                sender.sendMessage("Invalid number of arguments");
                return false;
            }
    
        } else if ((command.equalsIgnoreCase("sell")) && (sender instanceof Player)) {
            
            if (!permission.has(sender, "dynamark.sell")) {
                sender.sendMessage(ChatColor.RED + "You need permission to use this command");
                return true;
            }
            
            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("all")) {
                    return sellAll((Player) sender);
                }
            } else if (args.length == 2) {
                String item = args[0];
                int amount = 0;
                try {
                    amount = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid amount.");
                    sender.sendMessage("Be sure you typed a whole number.");
                    return false;
                }
                return sell((Player) sender, item, amount);
            } else {
                sender.sendMessage("Invalid number of arguments");
                return false;
            }
        // Command Example: /price cobblestone
        // should return: cobblestone: .01
        } else if (command.equalsIgnoreCase("price")){
            
            if (!permission.has(sender, "dynamark.price")) {
                sender.sendMessage(ChatColor.RED + "You need permission to use this command");
                return true;
            }
            // We expect one argument
            if (args.length == 1){
                String item = args[0];
                
                BigDecimal price = price(item);
                
                if (price == null) {
                    sender.sendMessage(ChatColor.RED + "Invalid item.");
                    return false;
                }
    
                sender.sendMessage(ChatColor.GRAY + item +ChatColor.GREEN + ": " + ChatColor.WHITE + price);
                return true;
    
            } else if (args.length == 2) {
                String item = args[0];
                int amt = Integer.valueOf(args[1]);
                
                return price(sender, item, amt);
            } else {
                // We received too many or too few arguments.
                sender.sendMessage("Invalid Arguments");
                return false;
            }
        // Example: '/market top' should return the top 5 most expensive items on the market
        // '/market bottom' should do the dame for the least expensive items.
        } else if(command.equalsIgnoreCase("market")) {
            
                return market(sender, args);            
        }
        return false;
    }

    public boolean market(CommandSender sender, String[] args) {

        /* 
         * Here we are going to determine just which market command is being called
         * 
         * an args length of one indicates that we have likely received a 'simple' market command
         * The 'simple' market commands are top, bottom, list, and -even a basic- help
         * The complicated command is help. 
         */
        if(args.length > 0) {
            if(args[0].equalsIgnoreCase("top")) {
                
                if (sender.hasPermission("DynaMark.market.top"))
                    return marketTop(sender);
                
                sender.sendMessage(ChatColor.RED + "You need permission to use this command");
                return false;
            } else if (args[0].equalsIgnoreCase("bottom")) {
                
                if (sender.hasPermission("DynaMark.market.bottom"))
                    return marketBottom (sender);
                
                sender.sendMessage(ChatColor.RED + "You need permission to use this command");
                return false;
            } else if (args[0].equalsIgnoreCase("list")) {
                
                if (sender.hasPermission("DynaMark.market.list"))
                    return marketList(sender);
                
                sender.sendMessage(ChatColor.RED + "You need permission to use this command");
                return false;
            } else if (args[0].equalsIgnoreCase("help") | args[0].equalsIgnoreCase("?")) {
                
                return marketHelp(sender);
            } else if (args[0].equalsIgnoreCase("add")) {
                
                if (sender.hasPermission("DynaMark.market.add") && sender instanceof Player)
                    return marketAdd(sender, args);
                
                sender.sendMessage(ChatColor.RED + "You need permission to use this command");
                return false;                
            } else if (args[0].equalsIgnoreCase("remove")) {
                
                if (sender.hasPermission("DynaMark.market.remove") && sender instanceof Player)
                    return marketRemove(sender, args);
                
                sender.sendMessage(ChatColor.RED + "You need permission to use this command");
                return false;

            }
        }
        sender.sendMessage("Invalid number of arguments");
        return false;
    }

    /**
     * Takes the name of a commodity and removes a commodity with that name from the database
     * 
     * @param sender
     * @param args
     * @return
     */
    private boolean marketRemove(CommandSender sender, String[] args) {
        // TODO Auto-generated method stub
    	if (args.length < 2) {
    		
    		new CommandHelper("too few arguments", sender)
    				.marketRemoveHelp();
    		return false;
    	}
    	
    	String name = args[1];
    	
    	try {
    		
    		new CommodityDBRemover(this)
		    		.removeCommodityByName(name);
    	} catch (CommodityNotFoundException e) {
    		
    		new CommandHelper(e.getMessage(), sender)
    				.marketRemoveHelp();
    		return false;
    	}
    	
    	return true;
    }

    /** see CommandHelper.marketAdd() for now
     * 
     * @param sender
     * @param args
     * @return
     */
    private boolean marketAdd(CommandSender sender, String[] args) {
        
    	String addendNumber = null,
    			addendData = null,
    			addendName = null,
    			addendValue = null,
    			addendMinValue = null,
    			addendMaxValue = null,
    			addendChangeRate = null,
    			addendSpread = null;
		try {
    		
    		addendNumber = args[1];
			addendData = args[2];
			addendName = args[3];
			addendValue = args[4];
			addendMinValue = args[5];
			addendMaxValue = args[6];
			addendChangeRate = args[7];
			addendSpread = args[8];
    		
    	} catch (ArrayIndexOutOfBoundsException e) {
    		
    		new CommandHelper("Too few arguments", sender)
    				.marketAddHelp();
    		return false;
    	}
        
    	Commodities commodity = new Commodities();
    	
    	try {
    		
			commodity.setNumber(Integer.valueOf(addendNumber));
    		commodity.setData(Integer.valueOf(addendData));
    		commodity.setName(addendName);
    		commodity.setValue(Double.valueOf(addendValue));
    		commodity.setMaxValue(Double.valueOf(addendMaxValue));
    		commodity.setMinValue(Double.valueOf(addendMinValue));
    		commodity.setChangeRate(Double.valueOf(addendChangeRate));
    		commodity.setSpread(Double.valueOf(addendSpread));
    		
    	} catch (NumberFormatException e) {
    		
    		new CommandHelper("Invalid Arguments", sender)
    				.marketAddHelp();
    		return false;
    	}
    	System.out.println(commodity.toString());
		try {
			
			new CommodityDBAdder(this)
					.addCommodity(commodity);
			
		} catch (DuplicateCommodityException e) {
			
			sender.sendMessage(e.getMessage());
			return false;
		}
    	
		sender.sendMessage(commodity.getName() + " successfully added to the database");
		
        return true;
    }

    /**
     * This is the general player help. it should be used for when a player
     * types "/market ?" 
     * 
     * this should give a brief explanation of DyanMark and each available 
     * command. further information on any individual command should be give 
     * in responce to a "/market ? <cmd>" from the player
     * 
     * @param sender
     * @return
     */
    private boolean marketHelp(CommandSender sender) {
        //TODO '/market ? <command>' and '/<command> ?' as well as '/market <command> ?' -just for good measure-
        sender.sendMessage(ChatColor.GRAY + "-----------------------------------------------------");
        sender.sendMessage(ChatColor.GREEN + "DyanaMark v " + this.getDescription().getVersion().toString());
        sender.sendMessage("An easy way to buy and sell your stuff");
        sender.sendMessage("Use \"" + ChatColor.GREEN + "/buy" + ChatColor.WHITE + "\"" + ChatColor.GRAY + " <item> <amount>" + ChatColor.WHITE + " to buy some stuff.");
        sender.sendMessage("Use \"" + ChatColor.GREEN + "/sell" + ChatColor.WHITE + "\"" + ChatColor.GRAY + " <item> <amount>" + ChatColor.WHITE + " to sell some stuff.");
        sender.sendMessage("Use \"" + ChatColor.GREEN + "/price" + ChatColor.WHITE + "\"" + ChatColor.GRAY + " <item> (amount)" + ChatColor.WHITE + " to check the price of some stuff. (amount optional)");
        sender.sendMessage("Use \"" + ChatColor.GREEN + "/market" + ChatColor.WHITE + "\"" + ChatColor.GRAY + " <top | bottom | list>" + ChatColor.WHITE + " to get some market info.");
        return true;
    }

    public boolean marketTop(CommandSender sender) {// We received '/market top'
        
        // gather the list of commodities
        List<Commodities> commodities = plugin.getDatabase()
                .find(Commodities.class)
                .findList();
        
        // sort 'em and return only the top 10
        List<Commodities> top10 = plugin.getDatabase()
                .filter(Commodities.class)
                .sort("value desc")
                .maxRows(10)
                .filter(commodities);
        
        // calculate elasticity
        List<Integer> elasticities = new LinkedList<Integer>();
        
        for (int index = 0; index < top10.size(); index++) {
            
            Commodities c = top10.get(index);
            double value = c.getValue();
            double changeRate = c.getChangeRate();
            double maxValue = c.getMaxValue();
            if (value > maxValue) {
                
                double newValue = value - maxValue;
                double elasticity = (newValue / changeRate);
                int el =(int) Math.round(elasticity);
                
                elasticities.add(index, el);
                c.setValue(maxValue);
            } else {
                elasticities.add(index, 0);
            }
        }

        // Send them to the player
        for(int x = 0; x < top10.size(); x++) {
            int rank = x + 1;
            
            sender.sendMessage(ChatColor.GREEN + String.valueOf(rank) + ". " + ChatColor.WHITE + top10.get(x).getName() + " " + ChatColor.GRAY + top10.get(x).getValue() + " " + ChatColor.DARK_GREEN + elasticities.get(x));
        }
        
        return true;
    }

    public boolean marketBottom(CommandSender sender) {
        
        // gather the list of commodities
        List<Commodities> commodities = plugin.getDatabase()
                .find(Commodities.class)
                .findList();
        
        // sort 'em and return only the top 10
        List<Commodities> top10 = plugin.getDatabase()
                .filter(Commodities.class)
                .sort("value asc")
                .maxRows(10)
                .filter(commodities);
        
        // calculate elasticity
        List<Integer> elasticities = new LinkedList<Integer>();
        
        for (int index = 0; index < top10.size(); index++) {
            
            Commodities c = top10.get(index);
            double value = c.getValue();
            double changeRate = c.getChangeRate();
            double minValue = c.getMinValue();
            if (value < minValue) {
                
                double newValue = Math.abs(value - minValue);
                double elasticity = (newValue / changeRate);
                int el =(int) Math.round(elasticity);
                
                elasticities.add(index, el);
                c.setValue(minValue);
            } else {
                elasticities.add(index, 0);
            }
        }

        // Send them to the player
        for(int x = 0; x < top10.size(); x++) {
            int rank = x + 1;
            
            sender.sendMessage(ChatColor.GREEN + String.valueOf(rank) + ". " + ChatColor.WHITE + top10.get(x).getName() + " " + ChatColor.GRAY + top10.get(x).getValue() + " " + ChatColor.DARK_GREEN + elasticities.get(x));
        }
        
        return true;
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
        if (amount < 1) {
            player.sendMessage(ChatColor.RED + "Invalid amount.");
            player.sendMessage("No negative numbers or zero, please.");
            return false;
        }
        
        // retrieve the commodity in question
        Commodities commodity = plugin.getDatabase().find(Commodities.class)
            .where()
            .ieq("name", item)
            .findUnique();
        
        // check that we found something
        if (commodity == null) {
            player.sendMessage(ChatColor.RED + "Not allowed to buy that item.");
            player.sendMessage("Be sure you typed the correct name");
            return false;
        }
        
        // determine what it will cost
        Invoice invoice = generateInvoice(1, commodity, amount);
        
        // check the player's wallet
        if (economy.has(player.getName(), invoice.getTotal())) {
            
            // give 'em the items and drop any extra
            Byte byteData = Byte.valueOf(String.valueOf(commodity.getData()));
            int id = commodity.getNumber();
            
            HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(new ItemStack(id, amount, (short) 0, byteData));
            for (int a : overflow.keySet()) {
                player.getWorld().dropItem(player.getLocation(), overflow.get(a));
            }
            
            // save the new value
            commodity.setValue(invoice.getValue());
            getDatabase().save(commodity);
            
            // use BigDecimal to format value for output
            double v = commodity.getValue();
            double max = commodity.getMaxValue();
            double min = commodity.getMinValue();
            BigDecimal value;
            if (v < max && v > min) {
                value = BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
            } else if (v <= min) {
                value = BigDecimal.valueOf(min).setScale(2, RoundingMode.HALF_UP);
            } else {
                value = BigDecimal.valueOf(max).setScale(2, RoundingMode.HALF_UP);
            }
            
            // Give some nice output.
            player.sendMessage(ChatColor.GREEN + "--------------------------------");
            player.sendMessage(ChatColor.GREEN + "Old Balance: " + ChatColor.WHITE + BigDecimal.valueOf(economy.getBalance(player.getName())).setScale(2, RoundingMode.HALF_UP));
            // Subtract the invoice (this is an efficient place to do this)
            economy.withdrawPlayer(player.getName(), invoice.getTotal());

            player.sendMessage(
                    ChatColor.GREEN + "Cost: " +
                    ChatColor.WHITE + BigDecimal.valueOf(invoice.getTotal()).setScale(2, RoundingMode.HALF_UP));
            player.sendMessage(ChatColor.GREEN + "New Balance: " + ChatColor.WHITE + BigDecimal.valueOf(economy.getBalance(player.getName())).setScale(2, RoundingMode.HALF_UP));
            player.sendMessage(ChatColor.GREEN + "--------------------------------");
            player.sendMessage(ChatColor.GRAY + item + ChatColor.GREEN + " New Price: " + ChatColor.WHITE + value);
            return true;
        }  else {// Otherwise, give nice output anyway ;)
           
            // The idea here is to show how much more money is needed.
            BigDecimal difference = BigDecimal.valueOf(economy.getBalance(player.getName()) - invoice.getTotal()).setScale(2, RoundingMode.HALF_UP);
            player.sendMessage(ChatColor.RED + "You don't have enough money");
            player.sendMessage(ChatColor.GREEN + "Balance: " + ChatColor.WHITE + BigDecimal.valueOf(economy.getBalance(player.getName())).setScale(2, RoundingMode.HALF_UP));
            player.sendMessage(
                    ChatColor.GREEN + "Cost: " + 
                    ChatColor.WHITE + BigDecimal.valueOf(invoice.getTotal()).setScale(2, RoundingMode.HALF_UP));
            player.sendMessage(ChatColor.GREEN + "Difference: " + ChatColor.RED + difference);
            return true;
        }
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
        if (amount <  1) {
            player.sendMessage(ChatColor.RED + "Invalid amount.");
            player.sendMessage("No negative numbers or zero, please.");
            return false;
        }
        
        // retrieve the commodity in question
        Commodities commodity = plugin.getDatabase().find(Commodities.class)
            .where()
            .ieq("name", item)
            .findUnique();
        
        if (commodity == null) {
            player.sendMessage(ChatColor.RED + "Not allowed to buy that item.");
            player.sendMessage("Be sure you typed the correct name");
            return false;
        }
        
        // determine what it will pay
        Invoice invoice = generateInvoice(0, commodity, amount);
        
        // If the player has enough of the item, perform the transaction.
        int id = commodity.getNumber();
        Byte byteData = Byte.valueOf(String.valueOf(commodity.getData()));
        
        ItemStack its = new ItemStack(
                id,
                amount,
                (short) 0,
                byteData);
        
        if (player.getInventory().contains(id)) {
            
            // Figure out how much is left over
            int left = getAmountInInventory(player, its) - amount;
            
            if (left < 0) {// this indicates the correct id, but the wrong bytedata value
                // give nice output even if they gave a bad name.
                player.sendMessage(ChatColor.RED + "You don't have enough " + item);
                player.sendMessage(ChatColor.GREEN + "In Inventory: " + ChatColor.WHITE + getAmountInInventory(player, its));
                player.sendMessage(ChatColor.GREEN + "Attempted Amount: " + ChatColor.WHITE + amount);
                return false;
            }
            
            // Take out all of the item
            int x = 0;
            
            for (@SuppressWarnings("unused") ItemStack stack : player.getInventory().getContents()) {// we do it this way incase a user has an expanded inventory via another plugin
                
                ItemStack slot = player.getInventory().getItem(x);
                Byte slotData = Byte.valueOf("0");
                
                try {
                    slotData = slot.getData().getData();
                } catch (NullPointerException e) {
                    
                }
                
                if ((slot.getTypeId() == id) && (slotData.compareTo(byteData) == 0)) {
                    player.getInventory().clear(x);
                }
                x++;
            }

            // put back what was left over
            if(left > 0) {
                ItemStack itsLeft = its;
                itsLeft.setAmount(left);
                player.getInventory().addItem(itsLeft);
            }
            
            // record the change in value
            commodity.setValue(invoice.getValue());
            plugin.getDatabase().save(commodity);
            
            // use BigDecimal to format value for output
            double v = commodity.getValue();
            double max = commodity.getMaxValue();
            double min = commodity.getMinValue();
            BigDecimal value;
            if (v < max && v > min) {
                value = BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
            } else if (v <= min) {
                value = BigDecimal.valueOf(min).setScale(2, RoundingMode.HALF_UP);
            } else {
                value = BigDecimal.valueOf(max).setScale(2, RoundingMode.HALF_UP);
            }
            BigDecimal spread = BigDecimal.valueOf(commodity.getSpread());
            
            // give some nice output
            BigDecimal sale = BigDecimal.valueOf((invoice.getTotal() + spread.doubleValue())).setScale(2, RoundingMode.HALF_UP);
            
            player.sendMessage(ChatColor.GREEN + "--------------------------------");
            player.sendMessage(ChatColor.GREEN + "Old Balance: " + ChatColor.WHITE + BigDecimal.valueOf(economy.getBalance(player.getName())).setScale(2, RoundingMode.HALF_UP));
            
            // deposit the money
            economy.depositPlayer(player.getName(), invoice.getTotal());
            
            player.sendMessage(ChatColor.GREEN + "Sale: " + ChatColor.WHITE + sale);
            player.sendMessage(ChatColor.GREEN + "Selling Fee: " + ChatColor.WHITE + spread);
            player.sendMessage(ChatColor.GREEN + "--------------------------------");
            player.sendMessage(ChatColor.GREEN + "Net Gain: " + ChatColor.WHITE + BigDecimal.valueOf(invoice.getTotal()).setScale(2, RoundingMode.HALF_UP));
            player.sendMessage(ChatColor.GREEN + "New Balance: " + ChatColor.WHITE + BigDecimal.valueOf(economy.getBalance(player.getName())).setScale(2, RoundingMode.HALF_UP));
            player.sendMessage(ChatColor.GREEN + "--------------------------------");
            player.sendMessage(ChatColor.GRAY + item + ChatColor.GREEN + " New Price: " + ChatColor.WHITE + value);
            return true;
        } else {// give nice output even if they gave a bad number.
            
            player.sendMessage(ChatColor.RED + "You don't have enough " + item);
            player.sendMessage(ChatColor.GREEN + "In Inventory: " + ChatColor.WHITE + getAmountInInventory(player, its));
            player.sendMessage(ChatColor.GREEN + "Attempted Amount: " + ChatColor.WHITE + amount);
            return false;
        }
    }

    public boolean sellAll(Player player) {
        
        // make a list of all commodities
        List<Commodities> commodities =
                plugin.getDatabase().find(Commodities.class).findList();
                
        // run thru each slot in the player's inventory for commodities
        int index = 0;
        BigDecimal sale = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        
        for (@SuppressWarnings("unused") ItemStack stack : player.getInventory().getContents()) {// we do it this way incase a user has an expanded inventory via another plugin
            ItemStack slot = player.getInventory().getItem(index);
            int slotId = slot.getTypeId();
            BigDecimal slotAmount = new BigDecimal(slot.getAmount()).
                    setScale(0, RoundingMode.HALF_UP);
            
            Byte slotByteData = Byte.valueOf("0");
            try {
                slotByteData = slot.getData().getData();
            } catch (NullPointerException e) {
                slotByteData = Byte.valueOf("0");
            }
            
            for (int x = 0; x < commodities.size(); x++) {
                
                if ((commodities.get(x).getNumber() == slotId) && 
                        (Byte.valueOf(String.valueOf(commodities.get(x).getData())).
                                compareTo(slotByteData) == 0)) {
                    
                    Invoice thisSale = generateInvoice(0,// perform sale of this slot
                            commodities.get(x),
                            slotAmount.intValue());
                    sale = sale.add(BigDecimal.valueOf(thisSale.getTotal()));// rack up our total
                    
                    // save the new value
                    commodities.get(x).setValue(thisSale.getValue());
                    plugin.getDatabase().save(commodities.get(x));
                    
                    player.getInventory().clear(index);// remove the item(s)
                    economy.depositPlayer(player.getName(),// "pay the man"
                            thisSale.getTotal());
                    
                    // give nice output
                    player.sendMessage(
                            ChatColor.GREEN + "Sold " + 
                            ChatColor.WHITE + slotAmount + " " + 
                            ChatColor.GRAY + commodities.get(x).getName() + 
                            ChatColor.GREEN + " for " + 
                            ChatColor.WHITE + BigDecimal.valueOf(thisSale.getTotal()).setScale(2, RoundingMode.HALF_UP));
                    break;
                }
            }
            index++;
        }
        
        // give a nice total column
        if (sale == BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
            player.sendMessage("Nothing to Sell");
        player.sendMessage(ChatColor.GREEN + "--------------------------------");
        player.sendMessage(ChatColor.GREEN + "Total Sale: " + ChatColor.WHITE + sale.setScale(2, RoundingMode.HALF_UP));
        return true;
    }

    public BigDecimal price(String item) {
        
        // retrieve the commodity in question
        Commodities commodity = plugin.getDatabase().find(Commodities.class).
                where().
                ieq("name", item).
                findUnique();
        
        if (commodity == null)
            return null;
        
        double price = commodity.getValue();
        double minValue = commodity.getMinValue();
        double maxValue = commodity.getMaxValue();
        
        if (price > maxValue)
            return BigDecimal.valueOf(maxValue).setScale(2, RoundingMode.HALF_UP);
        if (price < minValue)
            return BigDecimal.valueOf(minValue).setScale(2, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP);
    }
    
    public boolean price (CommandSender sender, String item, int amt) {
        
        // retrieve the commodity in question
        Commodities commodity = plugin.getDatabase().find(Commodities.class).
                where().
                ieq("name", item).
                findUnique();
        
        if (commodity == null) {
            sender.sendMessage(ChatColor.RED + "Invalit commodity name.");
            return false;
        }
        
        // get the buy and sell price of the item
        Invoice sellPrice = generateInvoice(0, commodity, amt);
        Invoice buyPrice = generateInvoice(1, commodity, amt);
        
        // send output
        sender.sendMessage(
                ChatColor.GRAY + item +
                ChatColor.GREEN + " If sold: " + 
                ChatColor.WHITE + BigDecimal.valueOf(sellPrice.getTotal()).setScale(2, RoundingMode.HALF_UP));
        sender.sendMessage(
                ChatColor.GRAY + item +
                ChatColor.GREEN + " If bought: " +
                ChatColor.WHITE + BigDecimal.valueOf(buyPrice.getTotal()).setScale(2, RoundingMode.HALF_UP));
        return true;
    }

    public boolean marketList(CommandSender sender) {
        
        List<Commodities> commodities = plugin.getDatabase().find(Commodities.class)
                .findList();

        String list[] = new String[20];
        list[0] = "";
        int row = 0;
        
        for (int index = 0; index < commodities.size(); index++) {
            
            // console is 55 characters wide, 20 tall
            list[row] = list[row] + commodities.get(index).getName() + ",  ";
            
            if (list[row].length() > 55) {
                
                int split = list[row].lastIndexOf(" ", 54);
                
                list[row] = list[row].substring(0, split);
                row++;
                if (row > 18) {
                    
                    sender.sendMessage("tell smickles that he needs to actually do something about this");
                    break;
                }
                list[row] = commodities.get(index).getName() + ",  ";
            }
        }
        list[row] = list[row].substring(0, list[row].lastIndexOf(","));
        
        //sender.sendMessage(ChatColor.GREEN + "All items on the market");
        for (int x = 0; x <= row; x++) {
            sender.sendMessage(ChatColor.WHITE + list[x]);
        }
        return true;
    }

    /**
     * Determine the cost of a given number of an item and calculate a new value for the item accordingly.
     * @param oper 1 for buying, 0 for selling.
     * @param commodity the commodity in question
     * @param amount the desired amount of the item in question
     * @return the total cost and the calculated new value as an Invoice
     */
    public Invoice generateInvoice(int oper, Commodities commodity, int amount) {
        
        // get the initial value of the item, 0 for not found
        Invoice inv = new Invoice(0.0, 0.0);
        inv.value = commodity.getValue();
        
        // get the spread so we can do one initial decrement of the value if we are selling
        BigDecimal spread = BigDecimal.valueOf(commodity.getSpread());
        
        // determine the total cost
        inv.total = 0.0;
        
        for(int x = 1; x <= amount; x++) {
            BigDecimal minValue = BigDecimal.valueOf(commodity.getMinValue());
            BigDecimal maxValue = BigDecimal.valueOf(commodity.getMaxValue());
            BigDecimal changeRate = BigDecimal.valueOf(commodity.getChangeRate());

            // work the spread on the first one.
            if ((oper == 0) && (x == 1)) {
                inv.subtractValue(spread.doubleValue());
            } else if ((oper == 0) && (x > 1)) { // otherwise, do the usual decriment.
                inv.subtractValue(changeRate.doubleValue());
            }
            
            // check the current value
            if (inv.getValue() >= minValue.doubleValue()) {// current value is @ or above minValue
                // be sure value is not above maxValue
                if (inv.getValue() < maxValue.doubleValue()) {// current value is "just right"
                    inv.addTotal(inv.getValue());// add current value to total
                } else {// current value is above the max
                    inv.addTotal(maxValue.doubleValue()); // add maxValue to total
                }
            } else {// current value is below the minimum
                
                inv.addTotal(minValue.doubleValue());// add the minimum to total
                
                if ((oper == 0) && (x == 1)) {
                    inv.subtractTotal(spread.doubleValue());// subtract the spread if we're selling and this is the first run
                }   
            }
            
            // Change our stored value for the item
            // we don't care about min/maxValue here because we don't want the value to 'bounce' off of them.
            if (oper == 1) {
                inv.addValue(changeRate.doubleValue());
            }
        }
        return inv;
    }
    

    /**
     * Figure out how much of a given item is in the player's inventory
     * @param player The player entity in question.
     * @param id The Data Value of the item in question.
     * @return The amount of the item in the player's inventory as an integer.
     */
    public int getAmountInInventory(Player player, ItemStack it) {
        int inInventory = 0;
        int x = 0;
        ItemStack slot;
        // we do it this way incase a user has an expanded inventory via another plugin
        for (@SuppressWarnings("unused") ItemStack stack : player.getInventory().getContents()) {
            slot = player.getInventory().getItem(x);
        
            if (slot != null) {
                Byte slotData = Byte.valueOf("0");
                Byte itData = Byte.valueOf("0");
                
                try {
                    slotData = slot.getData().getData();
                } catch (NullPointerException e) {
                    
                }
                try {
                    itData = it.getData().getData();
                } catch (NullPointerException e) {
                    
                }
            
                if ((slot.getTypeId() == it.getTypeId()) && (slotData.compareTo(itData) == 0)) {
                    inInventory += slot.getAmount();
                }
            } else {
                return 0;
            }
            x++;
        }
        return inInventory;
    }
}
