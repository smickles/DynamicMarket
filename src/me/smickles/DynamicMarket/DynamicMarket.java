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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
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
import org.bukkit.util.config.Configuration;


@SuppressWarnings("deprecation")
public class DynamicMarket extends JavaPlugin {

    public static DynamicMarket plugin;
    public final Logger logger = Logger.getLogger("Minecraft");
    public Configuration items;
    public static BigDecimal MINVALUE = BigDecimal.valueOf(.01).setScale(2);
    public static BigDecimal MAXVALUE = BigDecimal.valueOf(10000).setScale(2);
    public static BigDecimal CHANGERATE = BigDecimal.valueOf(.01).setScale(2);
    public static BigDecimal SPREAD = CHANGERATE;
    public static File directory = plugin.getDataFolder();
    
    /*
     * Vault Method stuffs
     */
    public static Permission permission = null;
    public static Economy economy = null;
    
    @Override
    public void onDisable(){
        PluginDescriptionFile pdfFile = this.getDescription();
        this.logger.info(pdfFile.getName() + " disabled");
    }

    @Override
    public void onEnable() {
        PluginDescriptionFile pdfFile = this.getDescription();
        
        plugin.setupDatabase();
        plugin.setupFiles();
        plugin.setupPermissions(); //Smickles thinks this is what we're supposed to do for permissions via vault
        plugin.setupEconomy(); //Smickles thinks this is what we're supposed to do for economy via vault
        
        
        /* Old Register stuff
         *  keeping this here because we may want to do something similar to ensure that vault 
         *  starts up properly since I suspect I had to do this b/c of bukkit, not register
         *  // setup economy
         
        PluginManager pm = this.getServer().getPluginManager();
        Plugin register = pm.getPlugin("Register");
        
        if (register != null && register.isEnabled()) {
            Methods.setMethod(pm);
            if (Methods.getMethod() != null) {
                logger.info("[" + pdfFile.getName() + "] Economy plugin found.");
            } else {
                pm.disablePlugin(this);
                return;
            }
        } else {
            pm.enablePlugin(register);
            pm.disablePlugin(this);
            pm.enablePlugin(this);
            return;
        }*/
        
        this.logger.info(pdfFile.getName() + " version " + pdfFile.getVersion() + " enabled");
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
                if (f.equalsIgnoreCase("config.yml.example"))
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
    private void switchToDatabase() {

        logger.info("[" + plugin.getDescription().getName() + "] Converting flatfile to database...");
        
        //load old config file
        items = plugin.getConfiguration();
        
        // populate the database with existing values
        logger.info("[" + plugin.getDescription().getName() + "] Populating database ...");
        for (String item : items.getKeys()) {
            
            Commodities commodity = plugin.getDatabase().find(Commodities.class).where().ieq("number", items.getString(item + ".number")).findUnique();
            
            if (commodity == null) {
                
                commodity = new Commodities();
                
                for (String key : items.getKeys(item)) {
                    
                    String value = items.getString(item + "." + key);

                    if (key.equalsIgnoreCase("value"))
                        commodity.setValue(BigDecimal(value));
                    if (key.equalsIgnoreCase("number"))
                        commodity.setNumber(Integer.valueOf(value));
                    if (key.equalsIgnoreCase("minValue"))
                        commodity.setMinValue(BigDecimal(value));
                    if (key.equalsIgnoreCase("maxValue"))
                        commodity.setMaxValue(BigDecimal(value));
                    if (key.equalsIgnoreCase("changeRate"))
                        commodity.setChangeRate(BigDecimal(value));
                    if (key.equalsIgnoreCase("data"))
                        commodity.setData(Integer.valueOf(value));
                    if (key.equalsIgnoreCase("spread"))
                        commodity.setSpread(BigDecimal(value));
                }
            } else {
                logger.warning("[" + plugin.getDescription().getName() + "] Duplicate commodity \"number\" found, that can't be good. You may want to restore the config.yml backup, then check the file for commodities with the same \"number\", correct the issue, and then restart your server to try again.");
            }
            
            plugin.getDatabase().save(commodity);
            commodity = null;
        }
        
        // mv config.yml to config.yml.bak
        logger.info("[" + plugin.getDescription().getName() + "] backing up config.yml...");
        
        File configFlatFile = new File("config.yml");
        File backupName = new File("config.yml.bak");
        
        while (!configFlatFile.renameTo(backupName)) {
            
            logger.info("[" + plugin.getDescription().getName() + "] backup name taken, trying something random...");
            
            backupName = new File("config.yml." + Math.random() + ".bak");
        }
        
        //  TODO rm config.yml.example
        
        logger.info("[" + plugin.getDescription().getName() + "] Successfully converter flatfile to database");

    }
    
    /**
     * Take a string, change it to a double, then to a BigDecimal
     * @param value
     * @return BigDecimal value of the given string
     */
    private BigDecimal BigDecimal(String value) {
        
        BigDecimal bd = BigDecimal.valueOf(Double.valueOf(value));
        return bd;
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
    
                sender.sendMessage(ChatColor.GRAY + item +ChatColor.GREEN + ": " + ChatColor.WHITE + price);
                return true;
    
            } else if (args.length == 2) {
                String item = args[0];
                int amt = Integer.valueOf(args[1]);
                
                price(sender, item, amt);
            } else {
                // We received too many or too few arguments.
                sender.sendMessage("Invalid Arguments");
                return false;
            }
        // Example: '/market top' should return the top 5 most expensive items on the market
        // '/market bottom' should do the dame for the least expensive items.
        } else if(command.equalsIgnoreCase("market")) {
            
            if (permission.has(sender, "dynamark.market")) {
                return market(sender, args);
            } else {
                sender.sendMessage(ChatColor.RED + "You need permission to use this command");
                return true;
            }
            
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
        if(args.length == 1) {
            if(args[0].equalsIgnoreCase("top")) {
                
                return marketTop(sender);
            } else if (args[0].equalsIgnoreCase("bottom")) {
                
                return marketBottom (sender);
            } else if (args[0].equalsIgnoreCase("list")) {
                
                return marketList(sender);
            } else if (args[0].equalsIgnoreCase("help") | args[0].equalsIgnoreCase("?")) {
                
                return marketHelp(sender);
            }
        }
        sender.sendMessage("Invalid number of arguments");
        return false;
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
        // TODO Auto-generated method stub
        sender.sendMessage(ChatColor.GRAY + "-----------------------------------------------------");
        sender.sendMessage(ChatColor.GREEN + "DyanaMark v " + this.getDescription().getVersion().toString());
        sender.sendMessage("An easy way to buy and sell your stuff");
        sender.sendMessage("Use \"" + ChatColor.GREEN + "/buy" + ChatColor.WHITE + "\"" + ChatColor.GRAY + " <item> <amount>" + ChatColor.WHITE + " to buy some stuff.");
        sender.sendMessage("Use \"" + ChatColor.GREEN + "/sell" + ChatColor.WHITE + "\"" + ChatColor.GRAY + " <item> <amount>" + ChatColor.WHITE + " to sell some stuff.");
        sender.sendMessage("Use \"" + ChatColor.GREEN + "/price" + ChatColor.WHITE + "\"" + ChatColor.GRAY + " <item> (amount)" + ChatColor.WHITE + " to check the price of some stuff. (amount optional)");
        sender.sendMessage("Use \"" + ChatColor.GREEN + "/market" + ChatColor.WHITE + "\"" + ChatColor.GRAY + " <top | bottom | list>" + ChatColor.WHITE + " to get some market info.");
        return true;
    }

    public boolean marketTop(CommandSender sender) {
        // We received '/market top'
        items.load();// load the item list
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
            
            sender.sendMessage(ChatColor.GREEN + String.valueOf(rank) + ". " + ChatColor.WHITE + board[x][1] + " " + ChatColor.GRAY + value + " " + ChatColor.DARK_GREEN + elasticity);
        }
        return true;
    }

    public boolean marketBottom(CommandSender sender) {

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
                                                            
            sender.sendMessage(ChatColor.GREEN + String.valueOf(rank) + ". " + ChatColor.WHITE + board[x][1] + " " + ChatColor.GRAY + value + " " + ChatColor.DARK_GREEN + elasticity);
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
            //(to be deleted TBD) MethodAccount cash = Methods.getMethod().getAccount(player.getName());
          
            //(TBD)if (cash.hasEnough(invoice.getTotal().doubleValue())) {
            if (economy.has(player.getName(), invoice.getTotal().doubleValue())) {
                Byte byteData = Byte.valueOf(items.getString(item + ".data", "0"));
                
                // give 'em the items and drop any extra
                HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(new ItemStack(id, amount, (short) 0, byteData));
                for (int a : overflow.keySet()) {
                    player.getWorld().dropItem(player.getLocation(), overflow.get(a));
                }
                
                items.setProperty(item + ".value", invoice.getValue());
                items.save();
                
                // get the new price of the item
                BigDecimal value = price(item);
                
                // Give some nice output.
                player.sendMessage(ChatColor.GREEN + "--------------------------------");
                player.sendMessage(ChatColor.GREEN + "Old Balance: " + ChatColor.WHITE + BigDecimal.valueOf(economy.getBalance(player.getName())).setScale(2, RoundingMode.HALF_UP));
                // Subtract the invoice (this is an efficient place to do this)
                //TBD cash.subtract(invoice.getTotal().doubleValue());
                economy.withdrawPlayer(player.getName(), invoice.getTotal().doubleValue());
    
                player.sendMessage(ChatColor.GREEN + "Cost: " + ChatColor.WHITE + invoice.getTotal());
                player.sendMessage(ChatColor.GREEN + "New Balance: " + ChatColor.WHITE + BigDecimal.valueOf(economy.getBalance(player.getName())).setScale(2, RoundingMode.HALF_UP));
                player.sendMessage(ChatColor.GREEN + "--------------------------------");
                player.sendMessage(ChatColor.GRAY + item + ChatColor.GREEN + " New Price: " + ChatColor.WHITE + value);
                return true;
            } else {
                // Otherwise, give nice output anyway ;)
                // The idea here is to show how much more money is needed.
                BigDecimal difference = BigDecimal.valueOf(economy.getBalance(player.getName()) - invoice.getTotal().doubleValue()).setScale(2, RoundingMode.HALF_UP);
                player.sendMessage(ChatColor.RED + "You don't have enough money");
                player.sendMessage(ChatColor.GREEN + "Balance: " + ChatColor.WHITE + BigDecimal.valueOf(economy.getBalance(player.getName())).setScale(2, RoundingMode.HALF_UP));
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
            Byte byteData = Byte.valueOf(items.getString(item + ".data", "0"));
            
            // determine what it will pay 
            Invoice invoice = generateInvoice(0, item, amount);
            //TBD MethodAccount cash = Methods.getMethod().getAccount(player.getName());
            // If the player has enough of the item, perform the transaction.
            ItemStack its = new ItemStack(id, amount, (short) 0, byteData);
            
            if (player.getInventory().contains(id)) {
                BigDecimal spread = BigDecimal.valueOf(items.getDouble(item + ".spread", 0));
                
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
                int x = 0;
                // we do it this way incase a user has an expanded inventory via another plugin
                for (@SuppressWarnings("unused") ItemStack stack : player.getInventory().getContents()) {
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
                items.setProperty(item + ".value", invoice.getValue());
                // record the change in value
                items.save();
                
                // get the new price of the item
                BigDecimal value = price(item);
                
                // give some nice output
                BigDecimal sale = invoice.getTotal().add(spread);
                
                player.sendMessage(ChatColor.GREEN + "--------------------------------");
                player.sendMessage(ChatColor.GREEN + "Old Balance: " + ChatColor.WHITE + BigDecimal.valueOf(economy.getBalance(player.getName())).setScale(2, RoundingMode.HALF_UP));
                //TBD cash.add(invoice.getTotal().doubleValue());
                economy.depositPlayer(player.getName(), invoice.getTotal().doubleValue());
                player.sendMessage(ChatColor.GREEN + "Sale: " + ChatColor.WHITE + sale);
                player.sendMessage(ChatColor.GREEN + "Selling Fee: " + ChatColor.WHITE + spread);
                player.sendMessage(ChatColor.GREEN + "--------------------------------");
                player.sendMessage(ChatColor.GREEN + "Net Gain: " + ChatColor.WHITE + invoice.getTotal());

                player.sendMessage(ChatColor.GREEN + "New Balance: " + ChatColor.WHITE + BigDecimal.valueOf(economy.getBalance(player.getName())).setScale(2, RoundingMode.HALF_UP));
                player.sendMessage(ChatColor.GREEN + "--------------------------------");
                player.sendMessage(ChatColor.GRAY + item + ChatColor.GREEN + " New Price: " + ChatColor.WHITE + value);
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

    public boolean sellAll(Player player) {
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
            byteData[x] = Byte.valueOf(items.getString(names.get(x) + ".data", "0"));
        }
        
        // run thru each slot and sell any sellable items
        int index = 0;
        // we do it this way incase a user has an expanded inventory via another plugin
        for (@SuppressWarnings("unused") ItemStack stack : player.getInventory().getContents()) {
            ItemStack slot = player.getInventory().getItem(index);
            int slotId = slot.getTypeId();
            BigDecimal slotAmount = new BigDecimal(slot.getAmount()).setScale(0, RoundingMode.HALF_UP);
            
            Byte slotByteData = Byte.valueOf("0");
            try {
                slotByteData = slot.getData().getData();
            } catch (NullPointerException e) {
                slotByteData = Byte.valueOf("0");
            }
            
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
                    //TBD MethodAccount cash = Methods.getMethod().getAccount(player.getName());
                    //TBD cash.add(thisSale.getTotal().doubleValue());
                    economy.depositPlayer(player.getName(), thisSale.getTotal().doubleValue());
                    // give nice output
                    player.sendMessage(ChatColor.GREEN + "Sold " + ChatColor.WHITE + slotAmount + " " + ChatColor.GRAY + names.get(x) + ChatColor.GREEN + " for " + ChatColor.WHITE + thisSale.getTotal());
                    break;
                }
            }
            index++;
        }
        
        // give a nice total collumn
        if (sale == BigDecimal.ZERO.setScale(2))
            player.sendMessage("Nothing to Sell");
        player.sendMessage(ChatColor.GREEN + "--------------------------------");
        player.sendMessage(ChatColor.GREEN + "Total Sale: " + ChatColor.WHITE + sale);
        return true;
    }

    public BigDecimal price(String item) {
        // Load the item list
        items.load();
        // get the price of the given item, if it's an invalid item set our variable to -2000000000 (an unlikely number to receive 'naturally')
        BigDecimal price = BigDecimal.valueOf(items.getDouble(item + ".value", -2000000000));
        BigDecimal minValue = BigDecimal.valueOf(items.getDouble(item + ".minValue", MINVALUE.doubleValue()));
        BigDecimal maxValue = BigDecimal.valueOf(items.getDouble(item + ".maxValue", MAXVALUE.doubleValue()));
        
        if (price.intValue() != -2000000000) {
            // We received an argument which resolved to an item on our list.
            // The price could register as a negative or below minValue
            // in this case we should return minValue as the price.
            if (price.compareTo(minValue) == -1) {
                price = minValue;
            } else if (price.compareTo(maxValue) == 1) {
                price = maxValue;
            }
            
            return price;
        }
        return BigDecimal.ZERO;
    }
    
    public void price (CommandSender sender, String item, int amt) {
        
        // get the buy and sell price of the item
        Invoice sellPrice = generateInvoice(0, item, amt);
        Invoice buyPrice = generateInvoice(1, item, amt);
        
        sender.sendMessage(ChatColor.GRAY + item +ChatColor.GREEN + " If sold: " + ChatColor.WHITE + sellPrice.getTotal());
        sender.sendMessage(ChatColor.GRAY + item +ChatColor.GREEN + " If bought: " + ChatColor.WHITE + buyPrice.getTotal());
    }

    public boolean marketList(CommandSender sender) {
        items.load();
        String list[] = new String[20];
        list[0] = "";
        int row = 0;
        
        for (String index : items.getKeys()) {
            // console is 55 characters wide, 20 tall
            
            list[row] = list[row] + index + ",  ";
            
            if (list[row].length() > 55) {
                int split = list[row].lastIndexOf(" ", 55);
                
                list[row] = list[row].substring(0, split);
                row++;
                list[row] = index + ",  ";
            }
        }
        list[row] = list[row].substring(0, list[row].lastIndexOf(","));
        
        sender.sendMessage(ChatColor.GREEN + "All items on the market");
        for (int x = 0; x <= row; x++) {
            sender.sendMessage(ChatColor.WHITE + list[x]);
        }
        return true;
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
        
        // get the spread so we can do one initial decrement of the value if we are selling
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
            } else if ((oper == 0) && (x > 1)) { // otherwise, do the usual decriment.
                inv.subtractValue(changeRate);
            }
            
            // check the current value
            if((inv.getValue().compareTo(minValue) == 1) | (inv.getValue().compareTo(minValue) == 0)) {// current value is @ or above minValue
                // be sure value is not above maxValue
                if (inv.getValue().compareTo(maxValue) == -1) {// current value is "just right"
                    inv.addTotal(inv.getValue());// add current value to total
                } else {// current value is above the max
                    inv.addTotal(maxValue); // add maxValue to total
                }
            } else {// current value is below the minimum
                
                inv.addTotal(minValue);// add the minimum to total
                
                if ((oper == 0) && (x == 1)) {
                    inv.subtractTotal(spread);// subtract the spread if we're selling and this is the first run
                }   
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
