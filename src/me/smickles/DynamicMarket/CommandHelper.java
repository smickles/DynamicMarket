package me.smickles.DynamicMarket;

import info.somethingodd.OddItem.configuration.OddItemAliases;

import org.bukkit.command.CommandSender;

public class CommandHelper extends DynamicMarket{

	String message;
	CommandSender sender;

	public CommandHelper(String message, CommandSender sender) {

		super();
		this.message = message;
		this.sender = sender;
		
		this.sender.sendMessage(this.message);
	}
	
	public CommandHelper(CommandSender sender) {

		super();
		this.sender = sender;
	}

	public void marketAddHelp() {

		sender.sendMessage("market add requires");
		sender.sendMessage("[number] [data] [name] [value] [minvalue] [maxvalue] [changerate] [spread]");
		sender.sendMessage("as");
		sender.sendMessage("<###> <##> <abc> <#.#> <#.#> <#.#> <#.#> <#.#>");
	}

	public void marketRemoveHelp() {
		// TODO Auto-generated method stub
		
	}
	
	public void suggestCommodityName(String item, OddItemAliases items) {
		sender.sendMessage("did you mean:");
		sender.sendMessage(items.getSuggestions().findBestWordMatch(item));
	}

	public void priceHelp() {
		// TODO Auto-generated method stub
		
	}

}
